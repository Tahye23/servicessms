import { AfterViewInit, Component, ElementRef, HostListener, inject, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormArray, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IGroupe } from 'app/entities/groupe/groupe.model';
import { EntityArrayResponseType, GroupeService } from 'app/entities/groupe/service/groupe.service';
import { IContact, PhoneInput } from '../contact.model';
import { ContactService, CustomField } from '../service/contact.service';
import { ContactFormService, ContactFormGroup } from './contact-form.service';
import { CountryISO, NgxIntlTelInputComponent, NgxIntlTelInputModule, SearchCountryField } from 'ngx-intl-tel-input';

declare global {
  interface Window {
    intlTelInputGlobals: any;
  }
}

interface RawCountry {
  name: string;
  iso2: string;
  dialCode: string;
}
@Component({
  standalone: true,
  selector: 'jhi-contact-update',
  templateUrl: './contact-update.component.html',
  styleUrl: './contact-update.component.css',
  imports: [SharedModule, FormsModule, ReactiveFormsModule, NgxIntlTelInputModule],
})
export class ContactUpdateComponent implements OnInit, AfterViewInit {
  isSaving = false;
  @ViewChild('groupSearchInput') groupSearchInput!: ElementRef;
  @ViewChild(NgxIntlTelInputComponent) phoneInput!: NgxIntlTelInputComponent;
  contact: IContact | null = null;
  groupesSharedCollection: IGroupe[] = [];
  isLoadingMore = false;

  currentPage = 0;
  pageSize = 20;
  error = false;
  message = '';
  groupes: IGroupe[] = [];
  selectedCountryISO: CountryISO = CountryISO.Mauritania;
  searchTerm = '';
  showDropdown = false;
  selectedGroupes: IGroupe[] = [];
  tempSelectedGroupes: IGroupe[] = [];
  private allCountries!: RawCountry[];
  private dialCodeToISO: Record<string, CountryISO> = {};

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (this.showDropdown && !this.groupSearchInput?.nativeElement?.contains(target) && !target.closest('.dropdown-content')) {
      this.closeDropdown();
    }
  }
  customFieldDefinitions: Record<string, CustomField> = {};
  customFields: Record<string, string> = {};
  protected contactService = inject(ContactService);
  protected contactFormService = inject(ContactFormService);
  protected groupeService = inject(GroupeService);
  protected activatedRoute = inject(ActivatedRoute);

  editForm: ContactFormGroup = this.contactFormService.createContactFormGroup();
  searchCountryFields: SearchCountryField[] = [SearchCountryField.Name, SearchCountryField.DialCode, SearchCountryField.Iso2];
  compareGroupe = (o1: IGroupe | null, o2: IGroupe | null): boolean => this.groupeService.compareGroupe(o1, o2);
  ngAfterViewInit() {
    for (const c of this.phoneInput.allCountries) {
      this.dialCodeToISO['+' + c.dialCode] = c.iso2.toUpperCase() as CountryISO;
    }
  }
  ngOnInit(): void {
    this.loadGroups();
    this.getCustomFields();
  }
  getCustomFields() {
    this.contactService.getCustomFields().subscribe({
      next: fields => {
        this.customFieldDefinitions = fields;

        this.activatedRoute.data.subscribe(({ contact }) => {
          this.contact = contact;
          if (contact) {
            this.updateForm(contact);
          } else {
            this.initializeCustomFields();
          }
          this.loadRelationshipsOptions();
        });
      },
      error: err => {
        this.error = true;
        this.message = 'Erreur lors du chargement';
        console.error('Erreur lors du chargement des custom fields', err);
      },
    });
  }
  get customFieldsControls() {
    return (this.editForm.get('customFields') as FormArray).controls;
  }

  initializeCustomFields(): void {
    const formArray = this.editForm.get('customFields') as FormArray;
    formArray.clear(); // Clear au cas où
    Object.keys(this.customFieldDefinitions).forEach(key => {
      formArray.push(
        new FormGroup({
          key: new FormControl(key, { nonNullable: true }),
          value: new FormControl('', { nonNullable: true }),
        }),
      );
    });
  }

  toggleDropdown() {
    this.showDropdown = !this.showDropdown;
    if (this.showDropdown) {
      this.tempSelectedGroupes = [...this.selectedGroupes];
      setTimeout(() => {
        this.groupSearchInput?.nativeElement?.focus();
      }, 0);
    }
  }
  closeDropdown() {
    this.showDropdown = false;
    this.tempSelectedGroupes = [...this.selectedGroupes];
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const contact = this.contactFormService.getContact(this.editForm);
    console.log('contact', contact);
    if (contact.id !== null) {
      this.subscribeToSaveResponse(this.contactService.update(contact));
    } else {
      this.subscribeToSaveResponse(this.contactService.create(contact));
    }
  }

  isSelected(groupe: IGroupe): boolean {
    return this.tempSelectedGroupes.some(g => g.id === groupe.id);
  }

  toggleGroupe(groupe: IGroupe): void {
    const index = this.tempSelectedGroupes.findIndex(g => g.id === groupe.id);
    if (index > -1) {
      // Désélection
      this.tempSelectedGroupes.splice(index, 1);
    } else {
      // Sélection
      this.tempSelectedGroupes.push(groupe);
    }
  }

  removeGroupe(groupe: IGroupe): void {
    const index = this.selectedGroupes.findIndex(g => g.id === groupe.id);
    if (index > -1) {
      this.selectedGroupes.splice(index, 1);
      // On retire également du tableau temporaire s'il y figure
      const tempIndex = this.tempSelectedGroupes.findIndex(g => g.id === groupe.id);
      if (tempIndex > -1) {
        this.tempSelectedGroupes.splice(tempIndex, 1);
      }
      this.updateFormGroupes();
    }
  }

  clearAllSelections(): void {
    this.tempSelectedGroupes = [];
    this.selectedGroupes = [];
    this.updateFormGroupes();
  }

  confirmSelections(): void {
    this.selectedGroupes = [...this.tempSelectedGroupes];
    this.showDropdown = false;
    this.updateFormGroupes();
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    const req = {
      search: this.searchTerm,
      page: this.currentPage,
      size: this.pageSize,
    };
    return this.groupeService.query(req).pipe(tap(() => console.log('groupeService ...')));
  }

  loadGroups(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
        this.isLoadingMore = false;
      },
    });
  }

  protected fillComponentAttributesFromResponseBody(data: IGroupe[] | null): IGroupe[] {
    return data ?? [];
  }

  updateFormGroupes(): void {
    // Mise à jour du champ 'groupes' dans le formulaire
    this.editForm.patchValue({
      groupes: this.selectedGroupes,
    });
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    if (this.currentPage === 0) {
      this.groupes = dataFromBody;
    } else {
      this.groupes = [...this.groupes, ...dataFromBody];
    }
  }

  onSearch(event: any): void {
    const term = event.target.value;
    this.searchTerm = term;
    this.currentPage = 0; // Réinitialisation de la pagination lors d'une nouvelle recherche
    this.groupes = [];
    this.loadGroups();
  }

  private parseE164(value: string | PhoneInput): PhoneInput {
    if (value && typeof value === 'object') {
      return value;
    }
    const e164 = (value as string) || '';

    if (!e164.startsWith('+')) {
      return {
        number: e164,
        nationalNumber: e164,
        internationalNumber: e164,
        e164Number: e164,
        dialCode: '',
        countryCode: null,
      };
    }

    const codes = Object.keys(this.dialCodeToISO).sort((a, b) => b.length - a.length);
    let dialCode = '';
    let countryCode: CountryISO | null = null;
    for (const dc of codes) {
      if (e164.startsWith(dc)) {
        dialCode = dc;
        countryCode = this.dialCodeToISO[dc];
        break;
      }
    }

    const national = e164.slice(dialCode.length);

    return {
      number: national,
      nationalNumber: national,
      internationalNumber: `${dialCode} ${national}`,
      e164Number: e164,
      dialCode,
      countryCode,
    };
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IContact>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: (error: any) => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(result: any): void {
    // Gestion éventuelle de l'erreur
    console.log('result', result);
    if (result.error.message == 'error.contactexists') {
      this.message = 'Un contact avec ce numéro existe déjà';
    } else if (result.error.message == 'error.contactInvalid') {
      this.message = 'Le numéro de téléphone fourni est invalide. Veuillez vérifier sa saisie.';
    } else {
      this.message = "Une erreur est survenue lors de l'enregistrement du contact.";
    }
    this.error = true;
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(contact: IContact): void {
    this.contact = contact;
    console.log('contact', contact);
    this.contactFormService.resetForm(this.editForm, contact);
    const parsed = this.parseE164(contact.contelephone || '');
    this.editForm.get('contelephone')!.setValue(parsed);

    // Mise à jour du pays sélectionné pour l'indicatif téléphonique
    this.selectedCountryISO = parsed.countryCode ?? CountryISO.Mauritania;

    // Gestion des customFields
    const formArray = this.editForm.get('customFields') as FormArray;
    formArray.clear();

    let parsedFields: Record<string, string> = {};
    if (contact.customFields) {
      try {
        parsedFields = JSON.parse(contact.customFields);
      } catch (e) {
        console.error('Erreur parsing customFields', e);
      }
    }

    Object.keys(this.customFieldDefinitions).forEach(key => {
      formArray.push(
        new FormGroup({
          key: new FormControl(key, { nonNullable: true }),
          value: new FormControl(parsedFields[key] || '', { nonNullable: true }),
        }),
      );
    });

    // Chargement des groupes liés au contact via la table de jointure
    if (contact.id) {
      this.groupeService.getGroupesByContact(contact.id).subscribe({
        next: groupes => {
          this.selectedGroupes = [...groupes];
          this.tempSelectedGroupes = [...groupes];
          // Met à jour la collection partagée des groupes pour éviter doublons dans le select
          this.groupesSharedCollection = this.groupeService.addGroupeToCollectionIfMissing<IGroupe>(
            this.groupesSharedCollection,
            ...groupes,
          );
        },
        error: err => {
          console.error('Erreur récupération groupes du contact', err);
          this.selectedGroupes = [];
          this.tempSelectedGroupes = [];
        },
      });
    } else {
      this.selectedGroupes = [];
      this.tempSelectedGroupes = [];
    }
  }

  protected loadRelationshipsOptions(): void {
    this.groupeService
      .query()
      .pipe(map((res: HttpResponse<IGroupe[]>) => res.body ?? []))
      .pipe(map((groupes: IGroupe[]) => this.groupeService.addGroupeToCollectionIfMissing<IGroupe>(groupes, this.contact?.groupe)))
      .subscribe((groupes: IGroupe[]) => (this.groupesSharedCollection = groupes));
  }
}
