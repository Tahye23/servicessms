import { Component, computed, ElementRef, HostListener, inject, Input, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IContact } from 'app/entities/contact/contact.model';
import { ContactService } from 'app/entities/contact/service/contact.service';
import { IGroupe } from 'app/entities/groupe/groupe.model';
import { GroupeService } from 'app/entities/groupe/service/groupe.service';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';
import { ReferentielService } from 'app/entities/referentiel/service/referentiel.service';
import { SendSmsService } from '../service/send-sms.service';
import { ISendSms, MessageType } from '../send-sms.model';
import { SendSmsFormService, SendSmsFormGroup } from './send-sms-form.service';
import { ITemplate } from '../../template/template.model';
import { PageTemplate, TemplateService } from '../../template/service/template.service';
import { AccountService } from '../../../core/auth/account.service';
import { TemplateRendererComponent } from '../../template/detailMessage/template-renderer.component';
import { SubscriptionService } from '../../../Subscription/service/subscriptionService.service';
import { DynamicMenuService } from '../../../Subscription/service/dynamicMenuService.service';

@Component({
  standalone: true,
  selector: 'jhi-send-sms-update',
  templateUrl: './send-sms-update.component.html',
  styleUrls: ['./send-sms-update.component.css'],
  imports: [SharedModule, FormsModule, ReactiveFormsModule, TemplateRendererComponent],
})
export class SendSmsUpdateComponent implements OnInit {
  @ViewChild('searchInput') searchInput!: ElementRef;
  @ViewChild('dropdownContainer') dropdownContainer!: ElementRef;

  @ViewChild('contactSearchInput') contactSearchInput!: ElementRef;
  @ViewChild('groupeSearchInput') groupeSearchInput!: ElementRef;
  @ViewChild('templateSearchInput') templateSearchInput!: ElementRef;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;

    const isContactArea = target.closest('.contact-search-container');

    const isGroupeArea = target.closest('.groupe-search-container');

    const isTemplateArea = target.closest('.template-search-container');

    if (this.dropdownOpen && !isContactArea) {
      this.dropdownOpen = false;
    }

    if (this.groupeDropdownOpen && !isGroupeArea) {
      this.groupeDropdownOpen = false;
    }

    if (this.templateDropdownOpen && !isTemplateArea) {
      this.templateDropdownOpen = false;
    }
  }

  @Input() isWhatsapp: boolean = false;
  isSaving = false;
  template!: ITemplate;
  sendSms: ISendSms | null = null;
  private subscriptionService = inject(SubscriptionService);
  public dynamicMenuService = inject(DynamicMenuService);

  extendedUsersSharedCollection: IExtendedUser[] = [];
  contactsSharedCollection: IContact[] = [];
  groupesSharedCollection: IGroupe[] = [];
  referentielsSharedCollection: IReferentiel[] = [];
  templates: ITemplate[] = [];
  groupeIdParam: number | null = null;

  recentContacts: IContact[] = [];

  searchTemplateTerm: string = '';
  remainingCharacters = 0;
  templateDropdownOpen: boolean = false;
  selectedTemplate = false;
  loadingTemplates: boolean = false;
  currentTemplatePage: number = 1;
  hasMoreTemplates: boolean = true;
  itemsPerPage: number = 10;

  isBulkSelected = false;
  messageError: string = '';
  messageSuccess: string = '';
  quotaInfo: any = null;
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  maxCharactersPerMessage: number = 160;

  searchTerm = '';
  page = 0;
  pageSize = 30;
  isLoading = false;
  hasMore = true;
  dropdownOpen = false;
  selectedContact: IContact | null = null;

  selectedGroupe: IGroupe | null = null;
  groupeDropdownOpen = false;
  groupePage = 0;
  isGroupeLoading = false;
  groupeHasMore = true;
  searchGroupeTerm = '';
  selectedGroupeId!: number;
  currentPage = 0;

  protected router = inject(Router);
  protected sendSmsService = inject(SendSmsService);
  protected sendSmsFormService = inject(SendSmsFormService);
  protected extendedUserService = inject(ExtendedUserService);
  protected contactService = inject(ContactService);
  protected groupeService = inject(GroupeService);
  protected referentielService = inject(ReferentielService);
  protected activatedRoute = inject(ActivatedRoute);
  protected templateService = inject(TemplateService);

  editForm: SendSmsFormGroup = this.sendSmsFormService.createSendSmsFormGroup();

  constructor(private accountService: AccountService) {}

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  compareContact = (o1: IContact | null, o2: IContact | null): boolean => this.contactService.compareContact(o1, o2);

  compareGroupe = (o1: IGroupe | null, o2: IGroupe | null): boolean => this.groupeService.compareGroupe(o1, o2);

  compareReferentiel = (o1: IReferentiel | null, o2: IReferentiel | null): boolean => this.referentielService.compareReferentiel(o1, o2);

  ngOnInit(): void {
    this.loadTemplates();
    this.loadRecentContacts();
    this.activatedRoute.data.subscribe(({ sendSms }) => {
      this.sendSms = sendSms;
      if (sendSms) {
        this.updateForm(sendSms);
        // Déterminer le type de message lors du chargement
        this.isBulkSelected = sendSms.isbulk || false;
      }
      this.loadRelationshipsOptions();
    });

    this.updateCharacterCount();
    this.initGroupeFromRoute();
  }

  setMessageType(type: 'individual' | 'bulk'): void {
    this.isBulkSelected = type === 'bulk';
    this.editForm.get('isbulk')?.setValue(this.isBulkSelected);

    if (type === 'individual') {
      this.clearSelectedGroupe();
    } else {
      this.clearSelectedContact();
    }
  }
  hasPermission(isBulk: boolean): boolean {
    if (this.isWhatsapp) {
      return isBulk
        ? this.dynamicMenuService.hasPermission('whatsapp.send.bulk')
        : this.dynamicMenuService.hasPermission('whatsapp.send.single');
    } else {
      return isBulk ? this.dynamicMenuService.hasPermission('sms.send.bulk') : this.dynamicMenuService.hasPermission('sms.send.single');
    }
  }

  toggleMessagePlatform(platform: 'sms' | 'whatsapp'): void {
    this.isWhatsapp = platform === 'whatsapp';

    this.templates = [];
    this.currentTemplatePage = 1;
    this.loadTemplates();
  }

  get subscriptionAccess() {
    return this.subscriptionService.subscriptionAccess();
  }

  insertVariable(variable: string): void {
    const msgdataControl = this.editForm.get('msgdata');
    const currentValue = msgdataControl?.value || '';
    const newValue = currentValue + variable;
    msgdataControl?.setValue(newValue);
    this.updateCharacterCount();
  }

  loadRecentContacts(): void {
    this.contactService
      .query({ page: 0, size: 5, sort: ['id,desc'] })
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .subscribe(contacts => {
        this.recentContacts = contacts;
      });
  }

  clearTemplate(): void {
    this.selectedTemplate = false;
    this.template = null as any;
    this.editForm.patchValue({
      template_id: null,
      msgdata: '',
      sender: '',
    });
    this.searchTemplateTerm = '';
    this.updateCharacterCount();
  }

  getTotalRecipients(): number {
    if (this.isBulkSelected && this.selectedGroupe) {
      return 1250;
    }
    return this.selectedContact ? 1 : 0;
  }

  updateCharacterCount(): void {
    const message = this.editForm.get('msgdata')?.value || '';
    this.remainingCharacters = message.length;
  }

  loadTemplates(): void {
    this.loadingTemplates = true;
    if (this.currentTemplatePage === 1) {
      this.templates = [];
    }

    const backendPage = this.currentTemplatePage - 1;

    this.templateService.getAllTemplates(backendPage, this.itemsPerPage, this.searchTemplateTerm, this.isWhatsapp).subscribe({
      next: (pageResponse: PageTemplate) => {
        if (this.currentTemplatePage === 1) {
          this.templates = pageResponse.content;
        } else {
          this.templates = [...this.templates, ...pageResponse.content];
        }

        // Trier par date de création (plus récent en premier)
        this.templates.sort((a, b) => {
          const dateA = a.created_at ? new Date(a.created_at).getTime() : 0;
          const dateB = b.created_at ? new Date(b.created_at).getTime() : 0;
          return dateB - dateA;
        });

        this.hasMoreTemplates = pageResponse.number + 1 < pageResponse.totalPages;
        this.loadingTemplates = false;
      },
      error: error => {
        console.error('Erreur lors du chargement des templates :', error);
        this.loadingTemplates = false;
      },
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.messageError = '';
    this.messageSuccess = '';

    if (!this.editForm.valid) {
      this.editForm.markAllAsTouched();
      this.messageError = 'Veuillez remplir tous les champs obligatoires.';
      return;
    }

    if (this.isBulkSelected && !this.selectedGroupe) {
      this.messageError = "Veuillez sélectionner un groupe pour l'envoi de masse.";
      return;
    }

    if (!this.isBulkSelected && !this.selectedContact) {
      this.messageError = 'Veuillez sélectionner un destinataire.';
      return;
    }

    if (!this.editForm.get('msgdata')?.value?.trim()) {
      this.messageError = 'Veuillez saisir un message.';
      return;
    }

    this.isSaving = true;

    // Définir le type de message
    if (this.isWhatsapp) {
      this.editForm.get('type')?.setValue(MessageType.WHATSAPP);
    } else {
      this.editForm.get('type')?.setValue(MessageType.SMS);
    }

    // Définir le titre automatiquement s'il n'est pas renseigné
    if (!this.editForm.get('titre')?.value) {
      const messagePreview = this.editForm.get('msgdata')?.value?.substring(0, 30) + '...';
      const recipient = this.isBulkSelected
        ? this.selectedGroupe?.grotitre
        : `${this.selectedContact?.conprenom} ${this.selectedContact?.connom}`;
      this.editForm.get('titre')?.setValue(`Message à ${recipient} - ${messagePreview}`);
    }

    const sendSms = this.sendSmsFormService.getSendSms(this.editForm);

    if (sendSms.id !== null) {
      this.subscribeToSaveResponse(this.sendSmsService.update(sendSms));
    } else {
      this.subscribeToSaveResponse(this.sendSmsService.create(sendSms));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISendSms>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: (result: any) => this.onSaveSuccess(result),
      error: (error: any) => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(result: any): void {
    this.messageSuccess = 'Message créé avec succès !';

    // Afficher le message de succès pendant 2 secondes avant de rediriger
    setTimeout(() => {
      if (this.isWhatsapp) {
        this.router.navigate(['/send-whatsapp', result.body.id, 'view']);
      } else {
        this.router.navigate(['/send-sms', result.body.id, 'view']);
      }
    }, 2000);
  }

  protected onSaveError(error: any): void {
    console.error('Erreur lors de la sauvegarde:', error);

    // Gestion détaillée des erreurs selon le code de statut
    if (error.status === 403 || error.status === 429) {
      // Erreurs de quota ou permissions
      this.handleQuotaError(error);
    } else if (error.status === 400) {
      // Erreurs de validation
      this.handleValidationError(error);
    } else if (error.status === 404) {
      // Ressources non trouvées
      this.messageError = 'Ressource non trouvée. Veuillez vérifier vos données.';
    } else if (error.status === 500) {
      // Erreur serveur
      this.messageError = 'Erreur serveur. Veuillez réessayer plus tard.';
    } else {
      // Erreur générique
      this.messageError = this.extractErrorMessage(error);
    }
  }

  private handleQuotaError(error: any): void {
    const errorMessage = this.extractErrorMessage(error);

    // Vérifier si c'est une erreur de quota spécifique
    if (
      errorMessage.toLowerCase().includes('quota') ||
      errorMessage.toLowerCase().includes('insuffisant') ||
      errorMessage.toLowerCase().includes('épuisé')
    ) {
      this.messageError = ` ${errorMessage}`;
    } else if (errorMessage.toLowerCase().includes('permission') || errorMessage.toLowerCase().includes('autorisé')) {
      this.messageError = ` ${errorMessage}`;
    } else {
      this.messageError = errorMessage;
    }
  }

  private handleValidationError(error: any): void {
    const errorMessage = this.extractErrorMessage(error);

    if (errorMessage.toLowerCase().includes('template') || errorMessage.toLowerCase().includes('modèle')) {
      this.messageError = ` ${errorMessage}`;
    } else if (errorMessage.toLowerCase().includes('contact') || errorMessage.toLowerCase().includes('destinataire')) {
      this.messageError = ` ${errorMessage}`;
    } else if (errorMessage.toLowerCase().includes('groupe')) {
      this.messageError = ` ${errorMessage}`;
    } else {
      this.messageError = ` ${errorMessage}`;
    }
  }

  private extractErrorMessage(error: any): string {
    // Essayer différentes propriétés pour extraire le message d'erreur
    if (error.error?.message) {
      return error.error.message;
    }

    if (error.error?.detail) {
      return error.error.detail;
    }

    if (error.error?.error) {
      return error.error.error;
    }

    if (error.message) {
      return error.message;
    }

    if (typeof error.error === 'string') {
      return error.error;
    }

    // Message par défaut
    return "Une erreur est survenue lors de l'envoi du message.";
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  openTemplateDropdown(): void {
    this.templateDropdownOpen = true;
    if (this.templates.length === 0) {
      this.loadTemplates();
    }
  }

  onTemplateSearch(): void {
    this.currentTemplatePage = 1;
    this.templates = [];
    this.hasMoreTemplates = true;
    this.loadTemplates();
  }

  selectTemplate(template: ITemplate): void {
    this.editForm.patchValue({
      template_id: template.id,
      msgdata: template.content,
      sender: template.expediteur,
    });
    this.template = template;
    this.remainingCharacters = template.content?.length ?? 0;
    this.selectedTemplate = true;
    this.editForm.get('characterCount')?.setValue(this.remainingCharacters, { emitEvent: false });
    this.searchTemplateTerm = template.name;
    this.templateDropdownOpen = false;
    this.updateCharacterCount();
  }

  openDropdown(): void {
    this.dropdownOpen = true;
    if (this.contactsSharedCollection.length === 0) {
      this.loadContacts();
    }
  }

  onSearchChange(term: string): void {
    this.page = 0;
    this.contactsSharedCollection = [];
    this.hasMore = true;
    this.loadContacts(term);
  }

  onScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20 && this.hasMore && !this.isLoading) {
      this.page++;
      this.loadContacts(this.searchTerm, this.page);
    }
  }

  loadContacts(search: string = '', page: number = 0): void {
    this.isLoading = true;
    this.contactService
      .query({ search, page, size: 20 })
      .pipe(map((res: HttpResponse<IContact[]>) => res.body ?? []))
      .subscribe(contacts => {
        if (page === 0) {
          this.contactsSharedCollection = contacts;
        } else {
          this.contactsSharedCollection = [...this.contactsSharedCollection, ...contacts];
        }
        this.hasMore = contacts.length === 20;
        this.isLoading = false;
      });
  }

  selectContact(contact: IContact): void {
    this.selectedContact = contact;
    this.dropdownOpen = false;
    this.editForm.get('destinateur')?.setValue(contact);

    // Si on est en mode bulk, passer en mode individuel
    if (this.isBulkSelected) {
      this.setMessageType('individual');
    }
  }

  clearSelectedContact(): void {
    this.selectedContact = null;
    this.editForm.get('destinateur')?.setValue(null);
  }

  loadGroupes(search: string = '', page: number = 0): void {
    this.isGroupeLoading = true;
    this.groupeService
      .query({ search, page, size: 20 })
      .pipe(map((res: HttpResponse<IGroupe[]>) => res.body ?? []))
      .subscribe(groupes => {
        if (page === 0) {
          this.groupesSharedCollection = groupes;
        } else {
          this.groupesSharedCollection = [...this.groupesSharedCollection, ...groupes];
        }
        this.groupeHasMore = groupes.length === 20;
        this.isGroupeLoading = false;
      });
  }

  selectGroupe(groupe: IGroupe): void {
    this.selectedGroupe = groupe;
    this.selectedGroupeId = groupe.id;
    this.groupeDropdownOpen = false;
    this.editForm.get('destinataires')?.setValue(groupe);

    // Si on est en mode individuel, passer en mode bulk
    if (!this.isBulkSelected) {
      this.setMessageType('bulk');
    }
  }

  clearSelectedGroupe(): void {
    this.selectedGroupe = null;
    this.editForm.get('destinataires')?.setValue(null);
  }

  loadMoreGroupes(event: MouseEvent): void {
    event.stopPropagation();
    this.groupePage++;
    this.loadGroupes(this.searchGroupeTerm, this.groupePage);
  }

  openGroupeDropdown(): void {
    this.groupeDropdownOpen = true;
    if (this.groupesSharedCollection.length === 0) {
      this.loadGroupes();
    }
  }

  onGroupeSearchChange(term: string): void {
    this.groupePage = 0;
    this.groupesSharedCollection = [];
    this.groupeHasMore = true;
    this.loadGroupes(term);
  }

  onGroupeScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20 && this.groupeHasMore && !this.isGroupeLoading) {
      this.groupePage++;
      this.loadGroupes(this.searchGroupeTerm, this.groupePage);
    }
  }

  protected updateForm(sendSms: ISendSms): void {
    this.sendSms = sendSms;
    this.sendSmsFormService.resetForm(this.editForm, sendSms);

    // Mettre à jour les états selon les données chargées
    this.isBulkSelected = sendSms.isbulk || false;

    if (sendSms.template_id) {
      this.templateService.find(sendSms.template_id).subscribe(template => {
        if (template.body) {
          this.template = template.body;
          this.selectedTemplate = true;
          this.editForm.patchValue({
            sender: template.body.expediteur,
          });
        }
      });
    }

    // Charger les collections avec les données existantes
    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      sendSms.user,
    );

    if (sendSms.destinateur) {
      this.selectedContact = sendSms.destinateur;
      this.contactsSharedCollection = this.contactService.addContactToCollectionIfMissing<IContact>(
        this.contactsSharedCollection,
        sendSms.destinateur,
      );
    }

    if (sendSms.destinataires) {
      this.selectedGroupe = sendSms.destinataires;
      this.groupesSharedCollection = this.groupeService.addGroupeToCollectionIfMissing<IGroupe>(
        this.groupesSharedCollection,
        sendSms.destinataires,
      );
    }

    this.referentielsSharedCollection = this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(
      this.referentielsSharedCollection,
      sendSms.statut,
    );

    this.updateCharacterCount();
  }

  protected loadRelationshipsOptions(): void {
    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.sendSms?.user),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));
    if (!this.groupeIdParam) {
      this.loadContacts();
      this.loadGroupes();
    }

    this.referentielService
      .query()
      .pipe(map((res: HttpResponse<IReferentiel[]>) => res.body ?? []))
      .pipe(
        map((referentiels: IReferentiel[]) =>
          this.referentielService.addReferentielToCollectionIfMissing<IReferentiel>(referentiels, this.sendSms?.statut),
        ),
      )
      .subscribe((referentiels: IReferentiel[]) => (this.referentielsSharedCollection = referentiels));
  }

  adjustHeight(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    target.style.height = 'auto';
    target.style.height = `${target.scrollHeight}px`;
    this.updateCharacterCount();
  }

  // === GETTERS ===

  get message(): string {
    return this.editForm.get('msgdata')?.value || '';
  }

  get messageCount(): number {
    return Math.ceil(this.remainingCharacters / this.maxCharactersPerMessage) || 1;
  }
  private initGroupeFromRoute(): void {
    this.groupeIdParam = Number(this.activatedRoute.snapshot.queryParamMap.get('groupeId')) || null;

    if (this.groupeIdParam) {
      this.groupeService.find(this.groupeIdParam).subscribe({
        next: res => {
          if (res.body) {
            this.selectGroupe(res.body);
          }
        },
      });
    }
  }
  protected readonly Math = Math;
}
