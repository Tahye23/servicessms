import { Component, inject, input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdvancedFiltersPayload, ICampaignSummary, IGroupe, NewGroupe } from '../groupe.model';
import { GroupeService } from '../../groupe/service/groupe.service';
import { IContact, AdvancedContactFilters, FilterType, BulkLinkFiltersPayload } from '../../contact/contact.model';
import { AccountService } from '../../../core/auth/account.service';
import { ContactService } from '../../contact/service/contact.service';
import { HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { ContactImportComponent } from '../../contact/contact-import/contact-import.component';
import { ToastComponent } from '../../toast/toast.component';
import { ContactDetailComponent } from '../../contact/detail/contact-detail.component';

@Component({
  standalone: true,
  selector: 'jhi-groupe-detail',
  templateUrl: './groupe-detail.component.html',
  imports: [FormsModule, NgForOf, NgIf, NgClass, RouterLink, ContactImportComponent, ToastComponent, ContactDetailComponent],
  styleUrls: ['./groupe-detail.component.scss'],
})
export class GroupeDetailComponent implements OnInit, OnDestroy {
  groupe = input<IGroupe | null>(null);
  @ViewChild('toast') toast!: ToastComponent;

  // ✅ Variables pour la modal de détail contact
  showContactModal = false;
  selectedContact: IContact | null = null;

  searchContact: string = '';
  searchContactModal: string = '';
  campaignsVisible = false;
  campaignsLoading = false;
  campaigns: ICampaignSummary[] = [];
  campaignSearch = '';
  campaignPage = 1;
  campaignPageSize = 10;
  totalCampaigns = 0;
  totalCampaignPages = 1;
  contacts?: (IContact & { selected?: boolean })[];
  filteredContacts: (IContact & { selected?: boolean })[] = [];
  allContactsModal: (IContact & { selected?: boolean })[] = [];
  filteredContactsModal: (IContact & { selected?: boolean })[] = [];
  private reloadTimer: any;
  showFileUpload: boolean = false;
  showAdvancedFilters: boolean = false;
  selectAllContacts: boolean = false;
  isCreatingGroup: boolean = false;
  newGroupName: string = '';
  allPageSelected = false;
  selectAllAcrossPages = false;
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

  lastErrorSelection: string = '';
  lastErrorCustom: string = '';

  private redirectTimer?: any;
  readonly REDIRECT_DELAY_MS = 2000;

  advancedFilters: AdvancedContactFilters = {
    nom: '',
    prenom: '',
    telephone: '',
    statut: '',
    hasWhatsapp: '',
    minSmsSent: undefined,
    maxSmsSent: undefined,
    minWhatsappSent: undefined,
    maxWhatsappSent: undefined,
    hasReceivedMessages: '',
    nomFilterType: FilterType.CONTAINS,
    prenomFilterType: FilterType.CONTAINS,
    telephoneFilterType: FilterType.CONTAINS,
    campaignId: null,
    smsStatus: '',
    deliveryStatus: '',
    lastErrorContains: '',
  };

  readonly FilterType = FilterType;

  visible: boolean = false;
  protected contactService = inject(ContactService);
  groupeId!: number;
  contactPage: number = 1;
  contactPageSize: number = 5;
  totalItems = 0;
  totalContactPages = 1;
  totalContacts = 0;
  isLoadingModal: boolean = false;
  advancedFiltersDraft: AdvancedContactFilters = { ...this.advancedFilters };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private accountService: AccountService,
    private groupeService: GroupeService,
  ) {
    this.groupeId = +this.route.snapshot.paramMap.get('id')!;
  }

  // ✅ MÉTHODE CORRIGÉE : Ouvrir le détail du contact
  openContactDetail(contact: IContact): void {
    this.selectedContact = contact;
    this.showContactModal = true;
    document.body.style.overflow = 'hidden'; // Empêche le scroll du body
  }

  // ✅ MÉTHODE : Fermer la modal
  closeContactModal(): void {
    this.showContactModal = false;
    this.selectedContact = null;
    document.body.style.overflow = ''; // Réactive le scroll
  }

  ngOnInit(): void {
    this.loadContacts(this.groupeId);
  }

  ngOnDestroy(): void {
    clearTimeout(this.redirectTimer);
    clearTimeout(this.reloadTimer);
    document.body.style.overflow = ''; // ✅ Cleanup
  }

  previousState(): void {
    window.history.back();
  }

  isTestGroup(groupe: IGroupe): boolean {
    if (!groupe) return false;
    const type = groupe.groupType ?? '';
    return type.toLowerCase().includes('test');
  }

  createCampaign(): void {
    const g = this.groupe();
    this.router.navigate(['/send-whatsapp/new-campagne'], {
      queryParams: { groupeId: g?.id },
    });
  }

  createCampaignSms(): void {
    const g = this.groupe();
    this.router.navigate(['/send-sms/new'], {
      queryParams: { groupeId: g?.id },
    });
  }
  removeSelectedContacts(): void {
    const selectedIds = (this.filteredContacts || []).filter(c => c.selected).map(c => c.id);

    if (selectedIds.length === 0) {
      this.showToast('Veuillez sélectionner au moins un contact à supprimer', 'info');
      return;
    }

    if (!confirm(`Êtes-vous sûr de vouloir retirer ${selectedIds.length} contact(s) de ce groupe ?`)) {
      return;
    }

    this.groupeService.removeContactsFromGroup(this.groupeId, selectedIds).subscribe({
      next: response => {
        const deletedCount = response.body?.deletedCount || 0;
        this.showToast(`${deletedCount} contact(s) retiré(s) du groupe avec succès`, 'success');

        // Décocher les sélections
        this.selectAllAcrossPages = false;
        this.allPageSelected = false;

        // Recharger la liste
        this.loadContacts(this.groupeId);
      },
      error: err => {
        console.error('Erreur lors de la suppression des contacts', err);
        this.showToast('Erreur lors de la suppression des contacts du groupe', 'error');
      },
    });
  }
  closeImport(): void {
    this.showFileUpload = false;
  }

  onImportCompleted(event: any): void {
    this.loadContacts(this.groupeId);
    console.log('Import completed:', event);
  }

  onLastErrorChange(v: string) {
    this.lastErrorSelection = v;
    if (v === '__custom__') {
      this.advancedFilters.lastErrorContains = (this.lastErrorCustom || '').trim();
    } else {
      this.advancedFilters.lastErrorContains = v;
      this.lastErrorCustom = '';
      this.applyAdvancedFilters();
    }
  }
  getSelectedContactsCount(): number {
    if (this.selectAllAcrossPages) {
      return this.totalItems;
    }
    return (this.filteredContacts || []).filter(c => c.selected).length;
  }
  onLastErrorCustomInput() {
    if (this.lastErrorSelection === '__custom__') {
      this.advancedFilters.lastErrorContains = (this.lastErrorCustom || '').trim();
      this.applyAdvancedFilters();
    }
  }

  toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  onSearchClick(): void {
    if (this.lastErrorSelection === '__custom__') {
      this.advancedFilters.lastErrorContains = (this.lastErrorCustom || '').trim();
    } else {
      this.advancedFilters.lastErrorContains = (this.lastErrorSelection || '').trim();
    }
    this.contactPage = 1;
    this.loadContacts(this.groupeId);
  }

  resetFilters(): void {
    const empty: AdvancedContactFilters = {
      nom: '',
      prenom: '',
      telephone: '',
      statut: '',
      hasWhatsapp: '',
      minSmsSent: undefined,
      maxSmsSent: undefined,
      minWhatsappSent: undefined,
      maxWhatsappSent: undefined,
      hasReceivedMessages: '',
      nomFilterType: FilterType.CONTAINS,
      prenomFilterType: FilterType.CONTAINS,
      telephoneFilterType: FilterType.CONTAINS,
      campaignId: null,
      smsStatus: '',
      deliveryStatus: '',
      lastErrorContains: '',
    };
    this.advancedFilters = { ...empty };
    this.advancedFiltersDraft = { ...empty };
    this.lastErrorSelection = '';
    this.lastErrorCustom = '';
  }

  onLastErrorSelectChange(v: string) {
    this.lastErrorSelection = v;
    if (v !== '__custom__') {
      this.lastErrorCustom = '';
    }
  }

  hasActiveFilters(): boolean {
    return Object.entries(this.advancedFilters).some(([key, value]) => {
      if (key.includes('FilterType')) return false;
      return value !== '' && value !== undefined && value !== null;
    });
  }

  applyAdvancedFilters(): void {
    this.contactPage = 1;
    clearTimeout(this.reloadTimer);
    this.reloadTimer = setTimeout(() => {
      this.loadContacts(this.groupeId);
    }, 300);
  }

  clearAllFilters(): void {
    this.advancedFilters = {
      nom: '',
      prenom: '',
      telephone: '',
      statut: '',
      hasWhatsapp: '',
      minSmsSent: undefined,
      maxSmsSent: undefined,
      minWhatsappSent: undefined,
      maxWhatsappSent: undefined,
      hasReceivedMessages: '',
      nomFilterType: FilterType.CONTAINS,
      prenomFilterType: FilterType.CONTAINS,
      telephoneFilterType: FilterType.CONTAINS,
      campaignId: null,
      smsStatus: '',
      deliveryStatus: '',
      lastErrorContains: '',
    };
    this.newGroupName = '';
    this.applyAdvancedFilters();
  }

  getFilteredContactsCount(): number {
    return this.filteredContacts?.length || 0;
  }

  get somePageSelected(): boolean {
    const page = this.filteredContacts || [];
    return page.some(c => !!c.selected);
  }

  updateMasterState(): void {
    const page = this.filteredContacts || [];
    this.allPageSelected = page.length > 0 && page.every(c => !!c.selected);
  }

  onToggleMaster(checked: boolean): void {
    this.selectAllAcrossPages = false;
    (this.filteredContacts || []).forEach(c => (c.selected = checked));
    this.updateMasterState();
  }

  onRowToggle(): void {
    this.selectAllAcrossPages = false;
    this.updateMasterState();
  }

  onSelectAllAcrossPages(): void {
    this.selectAllAcrossPages = true;
    (this.filteredContacts || []).forEach(c => (c.selected = false));
    this.updateMasterState();
  }

  onCancelSelectAllAcrossPages(): void {
    this.selectAllAcrossPages = false;
    this.updateMasterState();
  }

  createGroupFromFilter(): void {
    if (!this.newGroupName.trim()) {
      this.showToast(`Veuillez saisir un nom pour le nouveau groupe`, 'error');
      return;
    }

    const pickedIds = (this.filteredContacts || []).filter(c => c.selected).map(c => c.id);
    if (!this.selectAllAcrossPages && pickedIds.length === 0) {
      this.showToast(`Veuillez sélectionner au moins un contact ou utilisez "Sélectionner tous"`, 'info');
      return;
    }

    this.isCreatingGroup = true;

    const newGroup: NewGroupe = { grotitre: this.newGroupName.trim(), groupType: 'production' } as NewGroupe;

    this.groupeService.create(newGroup).subscribe({
      next: resp => {
        const targetId = resp.body?.id!;
        if (!targetId) throw new Error('Groupe créé sans ID');

        if (this.selectAllAcrossPages) {
          const emptyToUndef = (v?: string | null) => (v && v.trim() !== '' ? v.trim() : undefined);

          const payload: BulkLinkFiltersPayload = {
            nom: emptyToUndef(this.advancedFilters.nom),
            prenom: emptyToUndef(this.advancedFilters.prenom),
            telephone: emptyToUndef(this.advancedFilters.telephone),
            statut: this.advancedFilters.statut !== '' ? Number(this.advancedFilters.statut) : undefined,
            hasWhatsapp: this.advancedFilters.hasWhatsapp === '' ? undefined : this.advancedFilters.hasWhatsapp === 'true',
            minSmsSent: this.advancedFilters.minSmsSent ?? undefined,
            maxSmsSent: this.advancedFilters.maxSmsSent ?? undefined,
            minWhatsappSent: this.advancedFilters.minWhatsappSent ?? undefined,
            maxWhatsappSent: this.advancedFilters.maxWhatsappSent ?? undefined,
            hasReceivedMessages:
              this.advancedFilters.hasReceivedMessages === '' ? undefined : this.advancedFilters.hasReceivedMessages === 'true',
            campaignId: this.advancedFilters.campaignId ?? undefined,
            smsStatus: emptyToUndef(this.advancedFilters.smsStatus),
            deliveryStatus: emptyToUndef(this.advancedFilters.deliveryStatus),
            lastErrorContains: emptyToUndef(this.advancedFilters.lastErrorContains),
          };

          this.groupeService.bulkLinkContactsByFilter(this.groupeId, targetId, payload).subscribe({
            next: r => {
              this.isCreatingGroup = false;
              this.showToast(`Groupe "${this.newGroupName}" créé avec ${r.linked} contacts`, 'success');
              clearTimeout(this.redirectTimer);
              this.redirectTimer = setTimeout(() => {
                this.router.navigate(['/groupe', targetId, 'view']);
              }, this.REDIRECT_DELAY_MS);
            },
            error: err => {
              console.error(err);
              this.isCreatingGroup = false;
              this.showToast(`Erreur lors de l'ajout massif des contacts`, 'error');
            },
          });
        } else {
          this.groupeService.addContactsToGroup(targetId, pickedIds).subscribe({
            next: () => {
              this.isCreatingGroup = false;
              clearTimeout(this.redirectTimer);
              this.showToast(`Groupe "${this.newGroupName}" créé avec ${pickedIds.length} contacts`, 'success');
              this.redirectTimer = setTimeout(() => {
                this.router.navigate(['/groupe', targetId, 'view']);
              }, this.REDIRECT_DELAY_MS);
            },
            error: err => {
              console.error(err);
              this.isCreatingGroup = false;
              this.showToast(`Erreur lors de l'ajout des contacts au groupe`, 'error');
            },
          });
        }
      },
      error: err => {
        console.error(err);
        this.isCreatingGroup = false;
        this.showToast(`Erreur lors de la création du groupe`, 'error');
      },
    });
  }

  toggleSelectAll(): void {
    const currentContacts = this.hasActiveFilters() ? this.filteredContacts : this.contacts;
    if (currentContacts) {
      currentContacts.forEach(contact => {
        contact.selected = this.selectAllContacts;
      });
    }
  }

  getWhatsAppIcon(hasWhatsapp: boolean | undefined | null): string {
    return hasWhatsapp ? '✅ WhatsApp' : '❌ Pas WhatsApp';
  }

  getWhatsAppClass(hasWhatsapp: boolean | undefined | null): string {
    return hasWhatsapp
      ? 'text-green-600 bg-green-50 px-2 py-1 rounded-full text-xs font-medium'
      : 'text-gray-500 bg-gray-50 px-2 py-1 rounded-full text-xs font-medium';
  }

  getMessageStats(contact: IContact): string {
    const sms = contact.totalSmsSent || 0;
    const whatsapp = contact.totalWhatsappSent || 0;
    const total = sms + whatsapp;

    if (total === 0) return 'Aucun message';
    return `📱 ${sms} SMS | 💬 ${whatsapp} WhatsApp`;
  }

  getSuccessRate(contact: IContact): string {
    const totalSms = contact.totalSmsSent || 0;
    const totalWhatsapp = contact.totalWhatsappSent || 0;
    const successSms = contact.totalSmsSuccess || 0;
    const successWhatsapp = contact.totalWhatsappSuccess || 0;

    const total = totalSms + totalWhatsapp;
    const success = successSms + successWhatsapp;

    if (total === 0) return '0%';
    return `${Math.round((success / total) * 100)}%`;
  }

  getSuccessRateClass(contact: IContact): string {
    const totalSms = contact.totalSmsSent || 0;
    const totalWhatsapp = contact.totalWhatsappSent || 0;
    const successSms = contact.totalSmsSuccess || 0;
    const successWhatsapp = contact.totalWhatsappSuccess || 0;

    const total = totalSms + totalWhatsapp;
    const success = successSms + successWhatsapp;

    if (total === 0) return 'text-gray-500';

    const rate = (success / total) * 100;
    if (rate >= 80) return 'text-green-600';
    if (rate >= 60) return 'text-yellow-600';
    return 'text-red-600';
  }

  loadContacts(groupeId: number): void {
    let params = new HttpParams().set('page', String(this.contactPage - 1)).set('size', String(this.contactPageSize));

    const payload: AdvancedFiltersPayload = {
      search: (this.searchContact || '').trim() || null,
      nom: (this.advancedFilters.nom || '').trim() || null,
      prenom: (this.advancedFilters.prenom || '').trim() || null,
      telephone: (this.advancedFilters.telephone || '').trim() || null,
      nomFilterType: this.mapFilterType(this.advancedFilters.nomFilterType ?? FilterType.CONTAINS) as FilterType,
      prenomFilterType: this.mapFilterType(this.advancedFilters.prenomFilterType ?? FilterType.CONTAINS) as FilterType,
      telephoneFilterType: this.mapFilterType(this.advancedFilters.telephoneFilterType ?? FilterType.CONTAINS) as FilterType,
      statut: this.advancedFilters.statut !== '' ? Number(this.advancedFilters.statut) : null,
      hasWhatsapp: this.advancedFilters.hasWhatsapp === '' ? null : this.advancedFilters.hasWhatsapp === 'true',
      minSmsSent: this.advancedFilters.minSmsSent ?? null,
      maxSmsSent: this.advancedFilters.maxSmsSent ?? null,
      minWhatsappSent: this.advancedFilters.minWhatsappSent ?? null,
      maxWhatsappSent: this.advancedFilters.maxWhatsappSent ?? null,
      hasReceivedMessages: this.advancedFilters.hasReceivedMessages === '' ? null : this.advancedFilters.hasReceivedMessages === 'true',
      campaignId: this.advancedFilters.campaignId ?? null,
      smsStatus: (this.advancedFilters.smsStatus || '').trim() || null,
      deliveryStatus: (this.advancedFilters.deliveryStatus || '').trim() || null,
      lastErrorContains: (this.advancedFilters.lastErrorContains || '').trim() || null,
    };

    this.groupeService.postContactSearch(groupeId, payload, params).subscribe({
      next: res => {
        this.contacts = (res.body ?? []).map(c => ({ ...c, selected: false }));
        this.filteredContacts = [...(this.contacts || [])];

        const totalHeader = res.headers.get('X-Total-Count');
        this.totalItems = totalHeader ? +totalHeader : this.contacts?.length ?? 0;
        this.totalContactPages = Math.max(1, Math.ceil(this.totalItems / this.contactPageSize));
      },
      error: err => console.error('Erreur chargement contacts', err),
    });
  }

  private mapFilterType(ft: FilterType): string {
    switch (ft) {
      case FilterType.CONTAINS:
        return 'contains';
      case FilterType.STARTS_WITH:
        return 'starts_with';
      case FilterType.ENDS_WITH:
        return 'ends_with';
      case FilterType.EXACT:
        return 'exact';
      case FilterType.NOT_CONTAINS:
        return 'not_contains';
      case FilterType.NOT_STARTS_WITH:
        return 'not_starts_with';
      case FilterType.NOT_ENDS_WITH:
        return 'not_ends_with';
      case FilterType.NOT_EXACT:
        return 'not_exact';
      default:
        return 'contains';
    }
  }

  onContactSearch(): void {
    this.contactPage = 1;
    this.loadContacts(this.groupeId);
  }

  changeContactPage(page: number): void {
    if (page < 1 || page > this.totalContactPages) return;
    this.contactPage = page;
    this.loadContacts(this.groupeId);
  }

  onModalScroll(event: Event): void {
    const target = event.target as HTMLElement;
    if (target.scrollHeight - target.scrollTop === target.clientHeight) {
      this.loadMoreContactsModal();
    }
  }

  showDialog(): void {
    this.visible = true;
    this.loadContactsModal();
  }

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    if (this.toast) {
      this.toast.showToast(message, type);
    }
    console.log(`🍞 Toast (${type}):`, message);
  }

  loadContactsModal(page: number = 1): void {
    this.isLoadingModal = true;

    const params = {
      page: (page - 1).toString(),
      size: this.contactPageSize.toString(),
      search: this.searchContactModal.trim() || undefined,
    };

    this.contactService.query(params).subscribe({
      next: res => {
        const contacts = res.body ?? [];
        const total = res.headers.get('X-Total-Count');
        this.totalContacts = total ? +total : 0;

        if (page === 1) {
          this.allContactsModal = contacts.map(c => ({ ...c, selected: false }));
        } else {
          this.allContactsModal = [...this.allContactsModal, ...contacts.map(c => ({ ...c, selected: false }))];
        }

        this.filteredContactsModal = [...this.allContactsModal];
        this.contactPage = page;
        this.isLoadingModal = false;
      },
      error: err => {
        console.error('Erreur chargement contacts modal', err);
        this.isLoadingModal = false;
      },
    });
  }

  filterContactsModal(): void {
    this.contactPage = 1;
    this.loadContactsModal(1);
  }

  loadMoreContactsModal(): void {
    if (this.allContactsModal.length >= this.totalContacts || this.isLoadingModal) return;
    this.loadContactsModal(this.contactPage + 1);
  }

  addSelectedContacts(): void {
    const selectedContacts = this.filteredContactsModal.filter(c => c.selected);
    if (!selectedContacts.length) return;

    this.groupeService
      .addContactsToGroup(
        this.groupeId,
        selectedContacts.map(c => c.id),
      )
      .subscribe({
        next: () => {
          this.visible = false;
          this.loadContacts(this.groupeId);
        },
        error: err => console.error('Erreur ajout contacts au groupe', err),
      });
  }

  openCampaignsModal(): void {
    this.campaignsVisible = true;
    this.campaignPage = 1;
    this.reloadCampaigns();
  }

  reloadCampaigns(): void {
    this.fetchCampaigns(this.groupeId, this.campaignPage, this.campaignPageSize, this.campaignSearch);
  }

  changeCampaignPage(page: number): void {
    if (page < 1 || page > this.totalCampaignPages) return;
    this.campaignPage = page;
    this.reloadCampaigns();
  }

  private fetchCampaigns(groupId: number, page: number, size: number, q: string): void {
    this.campaignsLoading = true;
    this.groupeService
      .getCampaignsByGroup(groupId, {
        page: String(page - 1),
        size: String(size),
        search: q?.trim() || undefined,
      })
      .subscribe({
        next: res => {
          this.campaigns = res.body ?? [];
          const totalHeader = res.headers.get('X-Total-Count');
          this.totalCampaigns = totalHeader ? +totalHeader : this.campaigns.length;
          this.totalCampaignPages = Math.max(1, Math.ceil(this.totalCampaigns / this.campaignPageSize));
          this.campaignsLoading = false;
        },
        error: err => {
          console.error('Erreur chargement campagnes du groupe', err);
          this.campaigns = [];
          this.totalCampaigns = 0;
          this.totalCampaignPages = 1;
          this.campaignsLoading = false;
        },
      });
  }
}
