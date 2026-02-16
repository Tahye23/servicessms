import { Component, OnInit, signal, computed, ViewChild, AfterViewInit } from '@angular/core';
import { FormControl, FormGroup, FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { debounceTime, distinctUntilChanged, finalize } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { SelectionModel } from '@angular/cdk/collections';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

// Interfaces et enums
export enum RequestStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  IN_REVIEW = 'IN_REVIEW',
}

export interface PartnershipRequestStats {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  todayRequests: number;
}

interface FilterCriteria {
  status?: string;
  industry?: string;
  email?: string;
  companyName?: string;
  dateFrom?: string;
  dateTo?: string;
}

interface ExportOptions {
  format: 'excel' | 'csv' | 'pdf';
  includeAll: boolean;
  selectedOnly: boolean;
}

// Import du service
import { IPartnershipRequest } from '../partnership-request.model';
import { PartnershipRequestService } from '../service/partnershipRequestService.service';

@Component({
  selector: 'app-partnership-request-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './partnershipRequestService.component.html',
  styleUrls: ['./partnership-request-list.component.scss'],
})
export class PartnershipRequestListComponent implements OnInit, AfterViewInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  // Signaux pour la gestion d'état
  loading = signal(false);
  loadingStats = signal(false);
  selectedRequest = signal<IPartnershipRequest | null>(null);
  showFilters = signal(false);
  viewMode = signal<'table' | 'cards'>('table');
  showFabMenu = signal(false);

  // Données et état
  dataSource = new MatTableDataSource<IPartnershipRequest>([]);
  selection = new SelectionModel<IPartnershipRequest>(true, []);
  totalElements = signal(0);
  pageSize = signal(10);
  pageIndex = signal(0);
  currentPage = 0;

  // Statistiques
  stats = signal<PartnershipRequestStats | null>(null);

  // Formulaires et contrôles
  filterForm!: FormGroup;
  quickSearchForm!: FormGroup;
  searchControl = new FormControl('');
  statusFilter = new FormControl('');
  industryFilter = new FormControl('');
  emailFilter = new FormControl('');
  companyFilter = new FormControl('');
  dateFromFilter = new FormControl('');
  dateToFilter = new FormControl('');

  // Configuration des colonnes du tableau
  displayedColumns: string[] = [
    'select',
    'id',
    'createdDate',
    'fullName',
    'email',
    'companyName',
    'industry',
    'selectedPlanName',
    'status',
    'actions',
  ];

  // Options et constantes
  readonly requestStatuses = Object.values(RequestStatus);
  readonly statusColors = {
    [RequestStatus.PENDING]: 'orange',
    [RequestStatus.APPROVED]: 'green',
    [RequestStatus.REJECTED]: 'red',
    [RequestStatus.IN_REVIEW]: 'blue',
  };

  readonly statusLabels = {
    [RequestStatus.PENDING]: 'En attente',
    [RequestStatus.APPROVED]: 'Approuvée',
    [RequestStatus.REJECTED]: 'Rejetée',
    [RequestStatus.IN_REVIEW]: "En cours d'examen",
  };

  readonly industries = [
    'ecommerce',
    'retail',
    'services',
    'technology',
    'healthcare',
    'finance',
    'education',
    'restaurant',
    'real-estate',
    'automotive',
    'travel',
    'consulting',
    'other',
  ];

  readonly industryLabels: { [key: string]: string } = {
    ecommerce: 'E-commerce',
    retail: 'Commerce de détail',
    services: 'Services',
    technology: 'Technologie',
    healthcare: 'Santé',
    finance: 'Finance',
    education: 'Éducation',
    restaurant: 'Restauration',
    'real-estate': 'Immobilier',
    automotive: 'Automobile',
    travel: 'Voyage & Tourisme',
    consulting: 'Conseil',
    other: 'Autre',
  };

  // Subjects pour la recherche
  private searchSubject = new Subject<string>();
  private filterSubject = new Subject<FilterCriteria>();

  // Computed values
  hasSelection = computed(() => this.selection.selected.length > 0);
  selectedRequests = this.selection.selected;
  pendingCount = computed(() => this.stats()?.pendingRequests || 0);
  approvedCount = computed(() => this.stats()?.approvedRequests || 0);
  rejectedCount = computed(() => this.stats()?.rejectedRequests || 0);
  todayCount = computed(() => this.stats()?.todayRequests || 0);
  filteredRequests = computed(() => this.dataSource.data);

  constructor(
    private partnershipRequestService: PartnershipRequestService,
    private fb: FormBuilder,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {
    this.initializeForms();
    this.setupSearchSubscriptions();
  }

  ngOnInit(): void {
    this.loadRequests();
    this.loadStatistics();
  }

  ngAfterViewInit(): void {
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }

    // Configuration du tri custom
    this.dataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case 'fullName':
          return `${item.firstName} ${item.lastName}`;
        case 'createdDate':
          return (item.createdDate as any)?.valueOf();
        default:
          return (item as any)[property];
      }
    };
  }

  /**
   * Initialisation des formulaires
   */
  private initializeForms(): void {
    this.filterForm = this.fb.group({
      status: [''],
      industry: [''],
      email: [''],
      companyName: [''],
      dateFrom: [''],
      dateTo: [''],
    });

    this.quickSearchForm = this.fb.group({
      searchTerm: [''],
    });
  }

  /**
   * Configuration des souscriptions de recherche
   */
  private setupSearchSubscriptions(): void {
    // Recherche rapide avec debounce
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(searchTerm => {
      this.searchSubject.next(searchTerm || '');
    });

    // Filtres avancés
    this.filterForm.valueChanges.pipe(debounceTime(500), distinctUntilChanged()).subscribe(filters => {
      this.filterSubject.next(filters);
    });

    // Application des recherches
    this.searchSubject.subscribe(searchTerm => {
      this.applyQuickSearch(searchTerm);
    });

    this.filterSubject.subscribe(filters => {
      this.applyFilters(filters);
    });
  }

  /**
   * Chargement des demandes avec pagination
   */
  loadRequests(pageEvent?: PageEvent): void {
    this.loading.set(true);

    const page = pageEvent ? pageEvent.pageIndex : this.pageIndex();
    const size = pageEvent ? pageEvent.pageSize : this.pageSize();

    const params: Record<string, any> = {
      page: page,
      size: size,
      sort: this.getSortParams(),
    };

    // Ajout des filtres actifs
    const filters = this.filterForm.value;
    Object.keys(filters).forEach(key => {
      if (filters[key]) {
        params[key] = filters[key];
      }
    });

    this.partnershipRequestService
      .query(params)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.dataSource.data = response.body || [];
          this.totalElements.set(parseInt(response.headers.get('X-Total-Count') || '0'));
          this.pageIndex.set(page);
          this.pageSize.set(size);
          this.currentPage = page;

          // Clear selection when data changes
          this.selection.clear();
        },
        error: error => {
          console.error('Erreur lors du chargement des demandes:', error);
          this.showError('Erreur lors du chargement des données');
        },
      });
  }

  /**
   * Chargement des statistiques
   */
  loadStatistics(): void {
    this.loadingStats.set(true);

    this.partnershipRequestService
      .getStatistics()
      .pipe(finalize(() => this.loadingStats.set(false)))
      .subscribe({
        next: response => {
          this.stats.set(response.body);
        },
        error: error => {
          console.error('Erreur lors du chargement des statistiques:', error);
        },
      });
  }

  /**
   * Application de la recherche rapide
   */
  private applyQuickSearch(searchTerm: string): void {
    if (!searchTerm.trim()) {
      this.dataSource.filter = '';
      return;
    }

    this.dataSource.filterPredicate = (data: IPartnershipRequest, filter: string) => {
      const searchStr = filter.toLowerCase();
      return (
        data.firstName?.toLowerCase().includes(searchStr) ||
        data.lastName?.toLowerCase().includes(searchStr) ||
        data.email?.toLowerCase().includes(searchStr) ||
        data.companyName?.toLowerCase().includes(searchStr) ||
        data.phone?.toLowerCase().includes(searchStr) ||
        data.selectedPlanName?.toLowerCase().includes(searchStr) ||
        false
      );
    };

    this.dataSource.filter = searchTerm.trim().toLowerCase();
  }

  /**
   * Application des filtres avancés
   */
  applyFilters(filters: FilterCriteria): void {
    // Recharger les données avec les nouveaux filtres
    this.loadRequests();
  }

  /**
   * Obtention des paramètres de tri
   */
  private getSortParams(): string[] {
    if (!this.sort?.active || !this.sort?.direction) {
      return ['createdDate,desc'];
    }
    return [`${this.sort.active},${this.sort.direction}`];
  }

  /**
   * Méthodes de sélection
   */
  isSelected(request: IPartnershipRequest): boolean {
    return this.selection.isSelected(request);
  }

  toggleSelection(request: IPartnershipRequest): void {
    this.selection.toggle(request);
  }

  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  isSomeSelected(): boolean {
    return this.selection.selected.length > 0 && !this.isAllSelected();
  }

  toggleAllSelection(): void {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.dataSource.data.forEach(row => this.selection.select(row));
    }
  }

  masterToggle(): void {
    this.toggleAllSelection();
  }

  /**
   * Affichage des détails d'une demande
   */
  viewDetails(request: IPartnershipRequest): void {
    this.selectedRequest.set(request);
    this.router.navigate(['/requests/' + request.id + '/view']);
  }

  /**
   * Approbation d'une demande
   */
  approveRequest(request: IPartnershipRequest, adminNotes?: string): void {
    // Simulation du dialog - remplacez par votre composant de dialog
    const result = confirm(`Approuver la demande de ${this.getFullName(request)} ?`);

    if (result) {
      this.loading.set(true);
      this.partnershipRequestService
        .approve(request.id, adminNotes)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => {
            this.showSuccess('Demande approuvée avec succès');
            this.loadRequests();
            this.loadStatistics();
          },
          error: error => {
            console.error("Erreur lors de l'approbation:", error);
            this.showError("Erreur lors de l'approbation");
          },
        });
    }
  }

  /**
   * Rejet d'une demande
   */
  rejectRequest(request: IPartnershipRequest): void {
    // Simulation du dialog - remplacez par votre composant de dialog
    const result = confirm(`Rejeter la demande de ${this.getFullName(request)} ?`);

    if (result) {
      this.loading.set(true);
      this.partnershipRequestService
        .reject(request.id)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => {
            this.showSuccess('Demande rejetée');
            this.loadRequests();
            this.loadStatistics();
          },
          error: error => {
            console.error('Erreur lors du rejet:', error);
            this.showError('Erreur lors du rejet');
          },
        });
    }
  }

  /**
   * Actions groupées
   */
  bulkApprove(): void {
    const selectedRequests = this.selection.selected;
    if (selectedRequests.length === 0) return;

    console.log('Approbation en lot de', selectedRequests.length, 'demandes');
    // Implémenter l'approbation en lot
  }

  bulkReject(): void {
    const selectedRequests = this.selection.selected;
    if (selectedRequests.length === 0) return;

    console.log('Rejet en lot de', selectedRequests.length, 'demandes');
    // Implémenter le rejet en lot
  }

  /**
   * Export des données
   */
  exportData(options: ExportOptions): void {
    console.log('Export des données:', options);
    // Implémenter l'export selon le format choisi
  }

  /**
   * Filtres rapides
   */
  filterByStatus(status: RequestStatus | string): void {
    this.statusFilter.setValue(status);
    this.filterForm.patchValue({ status: status });
  }

  filterByToday(): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    this.dateFromFilter.setValue(today.toISOString().split('T')[0]);
    this.filterForm.patchValue({ dateFrom: today.toISOString().split('T')[0] });
  }

  clearFilters(): void {
    this.filterForm.reset();
    this.quickSearchForm.reset();
    this.searchControl.setValue('');
    this.statusFilter.setValue('');
    this.industryFilter.setValue('');
    this.emailFilter.setValue('');
    this.companyFilter.setValue('');
    this.dateFromFilter.setValue('');
    this.dateToFilter.setValue('');
    this.dataSource.filter = '';
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  /**
   * Méthodes utilitaires d'affichage
   */
  getStatusLabel(status: RequestStatus | string | null | undefined): string {
    if (!status) return 'Inconnu'; // ou retourne une chaîne vide si tu préfères
    return this.statusLabels[status as RequestStatus] || status;
  }

  getStatusColor(status: RequestStatus): string {
    return this.statusColors[status] || 'gray';
  }

  getStatusClasses(status: RequestStatus | string | null | undefined): string {
    const baseClasses = 'bg-white/20 text-white border-white/30';
    if (!status) return baseClasses; // Si le statut est vide, on retourne la classe de base

    switch (status) {
      case RequestStatus.PENDING:
        return `${baseClasses} bg-orange-500/20 text-orange-100 border-orange-300/30`;
      case RequestStatus.APPROVED:
        return `${baseClasses} bg-green-500/20 text-green-100 border-green-300/30`;
      case RequestStatus.REJECTED:
        return `${baseClasses} bg-red-500/20 text-red-100 border-red-300/30`;
      case RequestStatus.IN_REVIEW:
        return `${baseClasses} bg-blue-500/20 text-blue-100 border-blue-300/30`;
      default:
        return baseClasses;
    }
  }

  getStatusDotClass(status: RequestStatus | string | null | undefined): string {
    switch (status) {
      case RequestStatus.PENDING:
        return 'bg-orange-400';
      case RequestStatus.APPROVED:
        return 'bg-green-400';
      case RequestStatus.REJECTED:
        return 'bg-red-400';
      case RequestStatus.IN_REVIEW:
        return 'bg-blue-400';
      default:
        return 'bg-gray-400'; // Par défaut pour les valeurs non reconnues ou null/undefined
    }
  }

  getIndustryLabel(industry: string | null | undefined): string {
    if (!industry) return 'Inconnu'; // ou un label par défaut de ton choix
    return this.industryLabels[industry] || industry;
  }

  getFullName(request: IPartnershipRequest): string {
    return `${request.firstName || ''} ${request.lastName || ''}`.trim();
  }

  getInitials(request: IPartnershipRequest): string {
    const firstName = request.firstName || '';
    const lastName = request.lastName || '';
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  }

  formatDate(date: any): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  getRelativeTime(date: any): string {
    if (!date) return '';
    const now = new Date();
    const then = new Date(date);
    const diffInHours = Math.floor((now.getTime() - then.getTime()) / (1000 * 60 * 60));

    if (diffInHours < 24) {
      return `Il y a ${diffInHours}h`;
    } else {
      const diffInDays = Math.floor(diffInHours / 24);
      return `Il y a ${diffInDays}j`;
    }
  }

  /**
   * Méthodes de pagination
   */
  getDisplayRange(): string {
    const start = this.currentPage * this.pageSize() + 1;
    const end = Math.min((this.currentPage + 1) * this.pageSize(), this.totalElements());
    return `${start}-${end}`;
  }

  getTotalPages(): number {
    return Math.ceil(this.totalElements() / this.pageSize());
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      const pageEvent: PageEvent = {
        pageIndex: this.currentPage - 1,
        pageSize: this.pageSize(),
        length: this.totalElements(),
      };
      this.loadRequests(pageEvent);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.getTotalPages() - 1) {
      const pageEvent: PageEvent = {
        pageIndex: this.currentPage + 1,
        pageSize: this.pageSize(),
        length: this.totalElements(),
      };
      this.loadRequests(pageEvent);
    }
  }

  /**
   * TrackBy function pour les performances
   */
  trackByRequestId(index: number, request: IPartnershipRequest): number {
    return request.id;
  }

  /**
   * Messages utilisateur
   */
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass: ['success-snackbar'],
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar'],
    });
  }

  /**
   * Rafraîchissement des données
   */
  refresh(): void {
    this.loadRequests();
    this.loadStatistics();
  }

  /**
   * Basculer entre vue tableau et cartes
   */
  toggleViewMode(): void {
    this.viewMode.set(this.viewMode() === 'table' ? 'cards' : 'table');
  }

  /**
   * Basculer l'affichage des filtres
   */
  toggleFilters(): void {
    this.showFilters.set(!this.showFilters());
  }

  /**
   * Basculer le menu FAB
   */
  toggleFabMenu(): void {
    this.showFabMenu.set(!this.showFabMenu());
  }

  // Exposition de l'enum pour le template
  protected readonly RequestStatus = RequestStatus;
}
