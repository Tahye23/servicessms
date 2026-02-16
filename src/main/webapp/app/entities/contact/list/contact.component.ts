import { Component, NgZone, inject, OnInit, computed, ViewChild, ElementRef, HostListener } from '@angular/core';
import { HttpHeaders, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, interval, Observable, Subscription, switchMap, takeWhile, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { ItemCountComponent } from 'app/shared/pagination';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ITEMS_PER_PAGE, PAGE_HEADER, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { DataUtils } from 'app/core/util/data-util.service';
import { CustomFieldPayload, FilterType, IContact, ProgressStatus } from '../contact.model';

import { FileUploadModule } from 'primeng/fileupload';
import { EntityArrayResponseType, ContactService } from '../service/contact.service';
import { ContactDeleteDialogComponent } from '../delete/contact-delete-dialog.component';
import { FileextraitService } from 'app/entities/fileextrait/service/fileextrait.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { PaginatorModule } from 'primeng/paginator';
import { AccountService } from '../../../core/auth/account.service';
import { GroupeService } from '../../groupe/service/groupe.service';
import { IGroupe } from '../../groupe/groupe.model';
import { ToastComponent } from '../../toast/toast.component';
// @ts-ignore
import { ImportResultComponent } from '../import-result/import-result.component';
import { ContactImportComponent } from '../contact-import/contact-import.component';

export interface DuplicateContactsResponse {
  uniqueContacts: IContact[];
  duplicateContacts: IContact[];
  databaseDuplicates: IContact[];
  fileDuplicates: IContact[];
  errorContacts: IContact[];

  totalFileLines: number;
  totalInserted: number;
  totalDuplicates: number;
  totalDatabaseDuplicates: number;
  totalFileDuplicates: number;
  totalErrors: number;

  errorFileLocation?: string;
  duplicateFileLocation?: string;
  databaseDuplicateFileLocation?: string;
  fileDuplicateFileLocation?: string;

  progressId?: string;

  successRate?: number;
  duplicateRate?: number;
  errorRate?: number;
  importSummary?: string;

  valid?: boolean;
  hasDuplicates?: boolean;
  hasErrors?: boolean;
  hasDatabaseDuplicates?: boolean;
  hasFileDuplicates?: boolean;

  allContactsToInsert?: IContact[];
}

interface AdvancedContactFilters {
  nom?: string;
  prenom?: string;
  telephone?: string;

  nomFilterType?: FilterType;
  prenomFilterType?: FilterType;
  telephoneFilterType?: FilterType;

  statut?: '' | '0' | '1' | '3' | '4' | '5';
  hasWhatsapp?: '' | 'true' | 'false';

  minSmsSent?: number;
  maxSmsSent?: number;
  minWhatsappSent?: number;
  maxWhatsappSent?: number;
  hasReceivedMessages?: '' | 'true' | 'false';

  campaignId?: number | null;
  smsStatus?: string;
  deliveryStatus?: string;
  lastErrorContains?: string;
}

interface CustomField {
  label: string;
  maxLength: number;
}
@Component({
  standalone: true,
  selector: 'jhi-contact',
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss'],
  imports: [
    PaginatorModule,
    RouterModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    ItemCountComponent,
    FileUploadModule,
    ToastComponent,
    ImportResultComponent,
    ReactiveFormsModule,
    ContactImportComponent,
  ],
  providers: [ConfirmationService, MessageService],
})
export class ContactComponent implements OnInit {
  showAdvancedFilters = false;
  smsStatusOptions: string[] = ['SENT', 'FAILED'];
  deliveryStatusOptions: string[] = ['pending', 'sent', 'delivered', 'read', 'failed', 'expired', 'rejected'];
  lastErrorOptions: { value: string; label: string }[] = [
    { value: '', label: 'Tous' },
    { value: 'message undeliverable', label: 'Message undeliverable' },
    { value: 'This message was not delivered to maintain healthy ecosystem engagement.', label: 'Healthy ecosystem block' },
    { value: 'blocked', label: 'Blocked' },
    { value: 'spam', label: 'Spam suspected' },
    { value: 'blacklist', label: 'Blacklisted' },
    { value: 'invalid number', label: 'Invalid number' },
    { value: 'rate limit', label: 'Rate limit' },
    { value: '__custom__', label: 'Autre…' },
  ];
  searchTermDraft: string = '';

  lastErrorSelection = '';
  lastErrorCustom = '';

  advancedFilters: AdvancedContactFilters = {
    nom: '',
    prenom: '',
    telephone: '',
    nomFilterType: FilterType.CONTAINS,
    prenomFilterType: FilterType.CONTAINS,
    telephoneFilterType: FilterType.CONTAINS,
    statut: '',
    hasWhatsapp: '',
    minSmsSent: undefined,
    maxSmsSent: undefined,
    minWhatsappSent: undefined,
    maxWhatsappSent: undefined,
    hasReceivedMessages: '',
    campaignId: null,
    smsStatus: '',
    deliveryStatus: '',
    lastErrorContains: '',
  };
  advancedFiltersDraft: AdvancedContactFilters = { ...this.advancedFilters };

  toggleAdvancedFilters() {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  hasActiveFilters(): boolean {
    const f = this.advancedFilters;
    return Object.entries(f).some(([k, v]) => {
      if (k.endsWith('FilterType')) return false;
      return v !== '' && v !== null && v !== undefined;
    });
  }

  clearAllFilters(): void {
    const empty: AdvancedContactFilters = {
      nom: '',
      prenom: '',
      telephone: '',
      nomFilterType: FilterType.CONTAINS,
      prenomFilterType: FilterType.CONTAINS,
      telephoneFilterType: FilterType.CONTAINS,
      statut: '',
      hasWhatsapp: '',
      minSmsSent: undefined,
      maxSmsSent: undefined,
      minWhatsappSent: undefined,
      maxWhatsappSent: undefined,
      hasReceivedMessages: '',
      campaignId: null,
      smsStatus: '',
      deliveryStatus: '',
      lastErrorContains: '',
    };

    this.advancedFiltersDraft = { ...empty };
    this.advancedFilters = { ...empty };
    this.searchTermDraft = '';
    this.searchTerm = '';
    this.lastErrorSelection = '';
    this.lastErrorCustom = '';

    this.page = 1;
    this.handleNavigation(this.page, this.sortState());
    this.load();
  }

  onLastErrorCustomInput() {
    if (this.lastErrorSelection === '__custom__') {
      this.advancedFiltersDraft.lastErrorContains = this.lastErrorCustom || '';
    }
  }
  onLastErrorChange(v: string) {
    this.lastErrorSelection = v;
    if (v === '__custom__') {
      this.advancedFiltersDraft.lastErrorContains = this.lastErrorCustom || '';
    } else {
      this.advancedFiltersDraft.lastErrorContains = v;
      this.lastErrorCustom = '';
    }
  }

  subscription: Subscription | null = null;
  contacts?: IContact[] = [];
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const clickedInsideDropdown = this.dropdownContainer?.nativeElement.contains(target);
    const clickedInsideInput = this.searchInput?.nativeElement.contains(target);

    if (this.dropdownOpen && !clickedInsideDropdown && !clickedInsideInput) {
      this.dropdownOpen = false;
    }
  }
  actionsOpen = false;
  @ViewChild('actionsMenu') actionsMenu!: ElementRef;

  toggleActions(ev?: MouseEvent) {
    ev?.stopPropagation();
    this.actionsOpen = !this.actionsOpen;
  }
  closeActions() {
    this.actionsOpen = false;
  }
  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!this.actionsOpen) return;
    const target = e.target as Node;
    if (this.actionsMenu && !this.actionsMenu.nativeElement.contains(target)) {
      this.actionsOpen = false;
    }
  }
  @HostListener('document:keydown.escape')
  onEsc() {
    this.actionsOpen = false;
  }
  isImport = false;
  isLoadingCustomFields = false;
  @ViewChild('searchInput') searchInput!: ElementRef;
  @ViewChild('dropdownContainer') dropdownContainer!: ElementRef;
  dublantContacts: IContact[] = [];
  isLoading = false;
  selectedTab: string = 'contacts';
  groups: IGroupe[] = [];
  sortState = sortStateSignal({});
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  groupService = inject(GroupeService);
  error: IContact[] = [];
  itemsPerPage = 10;
  totalItems = 0;
  page = 1;
  first = 0;
  showModal = false;
  rows = 10;
  currentPage: number = 1;
  progressId: string | null = null;
  progressSubscription: Subscription | null = null;
  pageSize: number = 10;
  totalCount: number = 0;
  fieldForm!: FormGroup;
  isSubmitting = false;
  hasMore: boolean = false;
  customFields: Record<string, CustomField> = {};
  searchTerm2: string = '';
  showFileUpload: boolean = false;
  dropdownOpen: boolean = false;
  totalPages = 0;
  searchTerm: string = '';
  itemsPerPageOptions: number[] = [10, 20, 25, 30, 50];
  public router = inject(Router);
  protected fileextraitService = inject(FileextraitService);
  protected contactService = inject(ContactService);
  private accountService = inject(AccountService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected dataUtils = inject(DataUtils);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);
  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  trackId = (_index: number, item: IContact): number => this.contactService.getContactIdentifier(item);
  constructor(private fb: FormBuilder) {}
  ngOnInit(): void {
    this.loadParamImput();
    this.loadCustomFields();
    console.log('isAdmin', this.isAdmin());
    this.loadDublantContacts();
    this.searchTermDraft = this.searchTerm;
    this.advancedFiltersDraft = { ...this.advancedFilters };
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
  }
  loadParamImput() {
    this.activatedRoute.queryParamMap.subscribe(params => {
      this.isImport = params.get('isImport') === 'true';

      if (this.isImport) {
        this.showFileUpload = true;
      }
    });
  }
  byteSize(base64String: string): string {
    return this.dataUtils.byteSize(base64String);
  }
  onSearchClick(): void {
    const draft = { ...this.advancedFiltersDraft };

    draft.lastErrorContains =
      this.lastErrorSelection === '__custom__' ? (this.lastErrorCustom || '').trim() : (this.lastErrorSelection || '').trim();

    this.advancedFilters = draft;
    this.searchTerm = (this.searchTermDraft || '').trim();

    // pagination + charge
    this.page = 1;
    this.handleNavigation(this.page, this.sortState());
    this.load();
  }

  openFile(base64String: string, contentType: string | null | undefined): void {
    return this.dataUtils.openFile(base64String, contentType);
  }

  delete(contact: IContact): void {
    const modalRef = this.modalService.open(ContactDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.contact = contact;
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  view(contact: IContact) {
    this.router.navigate(['/contact', contact.id, 'view']);
  }
  edit(contact: IContact) {
    this.router.navigate(['/contact', contact.id, 'edit']);
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => this.onResponseSuccess(res),
    });
  }

  // Méthodes de pagination corrigées :
  prevPage(): void {
    if (this.page > 1) {
      this.page--;
      this.handleNavigation(this.page, this.sortState());
      this.load();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages) {
      this.page++;
      this.handleNavigation(this.page, this.sortState());
      this.load();
    }
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page, event);
  }

  selectTab(tabName: string): void {
    this.selectedTab = tabName;
  }

  toggleFileUpload(): void {
    this.showFileUpload = !this.showFileUpload;
  }

  loadDublantContacts(): void {
    this.contactService.getDoublantContacts().subscribe({
      next: contacts => {
        this.dublantContacts = contacts;
        console.log('Contacts doublons : ', this.dublantContacts);
      },
      error: error => console.error('Erreur lors de la récupération des contacts doublons', error),
    });
  }
  customFieldKeys(): string[] {
    return Object.keys(this.customFields);
  }

  addField(): void {
    if (this.fieldForm.invalid) {
      this.markFormGroupTouched(this.fieldForm);
      return;
    }

    const formValue = this.fieldForm.value;
    const api = formValue.apiName.trim();
    const label = formValue.label.trim();
    const max = formValue.maxLength;

    if (this.customFields[api]) {
      this.toast.showToast('Ce champ existe déjà', 'error');
      return;
    }

    this.customFields[api] = { label, maxLength: max };
    this.fieldForm.reset({ apiName: '', label: '', maxLength: 15 });
    this.toast.showToast(`Champ "${label}" ajouté`, 'success');
  }

  removeField(key: string): void {
    if (confirm(`Êtes-vous sûr de vouloir supprimer le champ "${this.customFields[key].label}" ?`)) {
      delete this.customFields[key];
      this.toast.showToast('Champ supprimé', 'success');
    }
  }
  openModal(): void {
    this.showModal = true;
  }

  loadCustomFields(): void {
    this.initForm();
    this.isLoadingCustomFields = true;
    this.contactService.getCustomFields().subscribe({
      next: fields => {
        this.customFields = fields;
        this.isLoadingCustomFields = false;
      },
      error: err => {
        console.error('Erreur lors du chargement des custom fields', err);
        this.toast.showToast('Impossible de charger les champs personnalisés', 'error');
        this.isLoadingCustomFields = false;
      },
    });
  }
  initForm(): void {
    this.fieldForm = this.fb.group({
      apiName: ['', [Validators.required, Validators.pattern(/^[a-zA-Z][a-zA-Z0-9]*$/), Validators.maxLength(30)]],
      label: ['', [Validators.required, Validators.maxLength(50)]],
      maxLength: [15, [Validators.required, Validators.min(1), Validators.max(500)]],
    });
  }
  closeModal() {
    this.showModal = false;
    this.initForm();
  }

  countCustomFields(): number {
    return this.customFieldKeys().length;
  }
  saveCustomFields(): void {
    if (this.countCustomFields() === 0) {
      this.toast.showToast('Aucun champ à enregistrer', 'error');
      return;
    }

    this.isSubmitting = true;
    const payload: CustomFieldPayload[] = Object.entries(this.customFields).map(([apiName, { label, maxLength }]) => ({
      apiName,
      label,
      maxLength,
    }));

    this.contactService.createCustomFields(payload).subscribe({
      next: () => {
        this.toast.showToast('Champs personnalisés enregistrés avec succès', 'success');
        this.closeModal();
        this.isSubmitting = false;
      },
      error: err => {
        console.error("Erreur lors de l'enregistrement", err);
        this.toast.showToast("Échec de l'enregistrement des champs personnalisés", 'error');
        this.isSubmitting = false;
      },
    });
  }
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  // Form validation helpers
  hasError(controlName: string, errorName: string): boolean {
    const control = this.fieldForm.get(controlName);
    return (control?.touched && control?.hasError(errorName)) || false;
  }

  getFieldErrorMessage(fieldName: string): string {
    const control = this.fieldForm.get(fieldName);

    if (!control || !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Ce champ est obligatoire';
    }

    if (fieldName === 'apiName' && control.hasError('pattern')) {
      return 'Doit commencer par une lettre et ne contenir que des lettres et des chiffres';
    }

    if (control.hasError('min')) {
      return `Valeur minimum: ${control.errors?.['min'].min}`;
    }

    if (control.hasError('max')) {
      return `Valeur maximum: ${control.errors?.['max'].max}`;
    }

    if (control.hasError('maxLength')) {
      return `Maximum ${control.errors?.['maxLength'].requiredLength} caractères`;
    }

    return 'Valeur invalide';
  }
  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page = +(page ?? 1);
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);

    this.contacts = dataFromBody;
  }

  protected fillComponentAttributesFromResponseBody(data: IContact[] | null): IContact[] {
    return data ?? [];
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER));
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    const { page } = this;
    this.isLoading = true;

    const queryObject: any = {
      page: page - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    const filterPayload: any = {};
    const f = this.advancedFilters;

    // Recherche globale
    if (this.searchTerm?.trim()) {
      filterPayload.search = this.searchTerm.trim();
    }

    // Filtres spécifiques
    if (f.nom?.trim()) {
      filterPayload.nom = f.nom.trim();
      filterPayload.nomFilterType = f.nomFilterType || FilterType.CONTAINS;
    }
    if (f.prenom?.trim()) {
      filterPayload.prenom = f.prenom.trim();
      filterPayload.prenomFilterType = f.prenomFilterType || FilterType.CONTAINS;
    }
    if (f.telephone?.trim()) {
      filterPayload.telephone = f.telephone.trim();
      filterPayload.telephoneFilterType = f.telephoneFilterType || FilterType.CONTAINS;
    }

    // Autres filtres
    if (f.statut) filterPayload.statut = f.statut;
    if (f.hasWhatsapp) filterPayload.hasWhatsapp = f.hasWhatsapp === 'true';
    if (f.minSmsSent != null) filterPayload.minSmsSent = f.minSmsSent;
    if (f.maxSmsSent != null) filterPayload.maxSmsSent = f.maxSmsSent;
    if (f.minWhatsappSent != null) filterPayload.minWhatsappSent = f.minWhatsappSent;
    if (f.maxWhatsappSent != null) filterPayload.maxWhatsappSent = f.maxWhatsappSent;
    if (f.hasReceivedMessages) filterPayload.hasReceivedMessages = f.hasReceivedMessages === 'true';

    // Campagne
    if (f.campaignId) filterPayload.campaignId = f.campaignId;
    if (f.smsStatus?.trim()) filterPayload.smsStatus = f.smsStatus.trim();
    if (f.deliveryStatus?.trim()) filterPayload.deliveryStatus = f.deliveryStatus.trim();
    if (f.lastErrorContains?.trim()) filterPayload.lastErrorContains = f.lastErrorContains.trim();

    console.log('🔍 Query:', queryObject, 'Filters:', filterPayload);

    return this.contactService.advancedSearch(filterPayload, queryObject).pipe(tap(() => (this.isLoading = false)));
  }
  protected handleNavigation(page: number, sortState: SortState): void {
    const queryParamsObj = {
      page,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(sortState),
    };

    this.ngZone.run(() => {
      this.router.navigate(['./'], {
        relativeTo: this.activatedRoute,
        queryParams: queryParamsObj,
      });
    });
  }

  getDisplayedPages(): (number | string)[] {
    const totalPages = this.totalPages;
    const currentPage = this.page;
    const delta = 2;
    const range: (number | string)[] = [];
    const rangeWithDots: (number | string)[] = [];
    let l: number | undefined;

    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || (i >= currentPage - delta && i <= currentPage + delta)) {
        range.push(i);
      }
    }

    for (let i of range) {
      if (l !== undefined && typeof i === 'number') {
        if (i - l === 2) {
          rangeWithDots.push(l + 1);
        } else if (i - l > 2) {
          rangeWithDots.push('...');
        }
      }
      rangeWithDots.push(i);
      if (typeof i === 'number') {
        l = i;
      }
    }

    return rangeWithDots;
  }
  isNumber(value: any): value is number {
    return typeof value === 'number';
  }

  changeItemsPerPage(): void {
    this.page = 1; // Reset à la première page
    this.handleNavigation(this.page, this.sortState());
    this.load();
  }

  goToPage(pageNumber: number): void {
    if (pageNumber !== this.page && pageNumber >= 1 && pageNumber <= this.totalPages) {
      this.page = pageNumber;
      this.handleNavigation(this.page, this.sortState());
      this.load();
    }
  }
  trackByPageNumber = (index: number, page: number | string): number | string => page;
  trackByContactId = (index: number, contact: IContact): number => contact.id!;

  protected readonly Math = Math;
  protected readonly FilterType = FilterType;
}
