import { Component, Input, OnInit, OnChanges, SimpleChanges, OnDestroy } from '@angular/core';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgClass, NgForOf, NgIf } from '@angular/common';
import { SendSmsService } from '../service/send-sms.service';
import { Router } from '@angular/router';

interface SmsStats {
  total: number;
  delivered: number;
  failed: number;
  pending: number;
  read: number;
  sent: number;
}

interface StatusOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-sms-list',
  templateUrl: './sms-list.component.html',
  standalone: true,
  imports: [FormsModule, NgClass, DatePipe, NgIf, NgForOf],
  styleUrls: ['./sms-list.component.scss'],
})
export class SmsListComponent implements OnInit, OnChanges, OnDestroy {
  @Input() bulkId: number | null | undefined = null;

  // Données et pagination
  smsList: any[] = [];
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  isLoadingSms = false;

  // Filtres
  searchTerm = '';
  deliveryStatusFilter = '';
  dateFromFilter = '';
  dateToFilter = '';

  // Subject pour debounce de la recherche et destruction du composant
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Options pour les sélecteurs
  pageSizeOptions: number[] = [5, 10, 20, 50];

  statusOptions: StatusOption[] = [
    { value: '', label: 'Tous les statuts' },
    { value: 'delivered', label: 'Livrés' },
    { value: 'read', label: 'Lus' },
    { value: 'sent', label: 'Envoyés' },
    { value: 'pending', label: 'En attente' },
    { value: 'failed', label: 'Échec' },
  ];

  // Statistiques
  stats: SmsStats = {
    total: 0,
    delivered: 0,
    failed: 0,
    pending: 0,
    read: 0,
    sent: 0,
  };

  constructor(
    private sendSmsService: SendSmsService,
    private router: Router,
  ) {
    // Configuration du debounce pour la recherche
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.currentPage = 0;
      this.loadSmsPage();
    });
  }

  ngOnInit(): void {
    if (this.bulkId) {
      this.loadSmsPage();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['bulkId'] && this.bulkId) {
      this.resetFilters();
      // ❌ plus besoin d'appeler loadSmsPage ici, resetFilters va déjà le faire
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Chargement des SMS avec filtres côté backend
   */
  // Dans sms-list.component.ts
  loadSmsPage(): void {
    if (!this.bulkId && this.bulkId !== 0) return;

    if (this.dateFromFilter && this.dateToFilter && this.dateFromFilter > this.dateToFilter) {
      console.warn('Intervalle de dates invalide: dateFrom > dateTo');
      return;
    }

    this.isLoadingSms = true;

    const params = {
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm?.trim() || undefined,
      deliveryStatus: this.deliveryStatusFilter?.trim() || undefined,
      dateFrom: this.dateFromFilter?.trim() || undefined,
      dateTo: this.dateToFilter?.trim() || undefined,
    };

    this.sendSmsService.getSmsByBulkId(this.bulkId!, params).subscribe({
      next: (response: any) => {
        this.smsList = response?.content ?? [];
        this.totalPages = response?.totalPages ?? 0;

        // NOUVEAU: Utiliser les stats globales au lieu des stats locales
        this.updateGlobalStats(response?.globalStats);
        this.isLoadingSms = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des SMS', error);
        this.isLoadingSms = false;
        this.smsList = [];
        this.totalPages = 0;
        this.updateGlobalStats({});
      },
    });
  }
  isDownloading = false;
  /**
   * Télécharge un fichier CSV avec tous les contacts selon les filtres actuels
   */
  downloadCsv(): void {
    if (!this.bulkId && this.bulkId !== 0) {
      console.warn('Aucun bulkId défini pour le téléchargement');
      return;
    }

    this.isDownloading = true;

    const params = {
      search: this.searchTerm?.trim() || undefined,
      deliveryStatus: this.deliveryStatusFilter?.trim() || undefined,
      dateFrom: this.dateFromFilter?.trim() || undefined,
      dateTo: this.dateToFilter?.trim() || undefined,
    };

    this.sendSmsService.downloadSmsContacts(this.bulkId!, params).subscribe({
      next: (response: Blob) => {
        this.handleCsvDownload(response);
        this.isDownloading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du téléchargement CSV', error);
        this.isDownloading = false;
        // Optionnel: afficher un message d'erreur à l'utilisateur
        alert('Erreur lors du téléchargement du fichier CSV');
      },
    });
  }

  /**
   * Gère le téléchargement du fichier CSV
   */
  private handleCsvDownload(blob: Blob): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');

    // Générer un nom de fichier avec timestamp
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:]/g, '-');
    const filename = `sms-contacts-${this.bulkId}-${timestamp}.csv`;

    link.href = url;
    link.download = filename;
    link.click();

    // Nettoyer l'URL temporaire
    window.URL.revokeObjectURL(url);

    console.log(`Fichier CSV téléchargé: ${filename}`);
  }
  // NOUVELLE MÉTHODE: Utiliser les stats globales du backend
  updateGlobalStats(globalStats: any): void {
    this.stats = {
      total: globalStats?.total || 0,
      delivered: globalStats?.delivered || 0,
      failed: globalStats?.failed || 0,
      pending: globalStats?.pending || 0,
      read: globalStats?.read || 0,
      sent: globalStats?.sent || 0,
    };
  }

  /**
   * Gestion de la recherche avec debounce
   */
  onSearchTermChange(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }

  /**
   * Changement de filtre de statut
   */
  onStatusFilterChange(): void {
    this.currentPage = 0;
    this.loadSmsPage();
  }

  /**
   * Changement de filtre de date
   */
  onDateFilterChange(): void {
    this.currentPage = 0;
    this.loadSmsPage();
  }

  /**
   * Réinitialisation des filtres
   */
  resetFilters(): void {
    this.searchTerm = '';
    this.deliveryStatusFilter = '';
    this.dateFromFilter = '';
    this.dateToFilter = '';
    this.currentPage = 0;
    this.loadSmsPage();
  }

  /**
   * Changement de page
   */
  changePage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.currentPage = newPage;
      this.loadSmsPage();
    }
  }

  /**
   * Changement de taille de page
   */
  onPageSizeChange(): void {
    this.currentPage = 0;
    this.loadSmsPage();
  }

  /**
   * Voir le détail d'un SMS
   */
  viewSms(sms: any): void {
    // TODO: Implémentez votre logique pour afficher le détail du SMS
    // Par exemple, ouvrir une modal ou naviguer vers une page de détail
    console.log('Voir SMS:', sms);

    // Exemple d'implémentation avec une modal ou un événement
    // this.modalService.open(SmsDetailComponent, { data: sms });
    // ou
    this.router.navigate(['sms', sms.id]);
    // ou
    // this.onSmsSelected.emit(sms);
  }

  /**
   * Retourne les classes CSS pour le badge de statut
   */
  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      read: 'bg-green-100 text-green-800',
      delivered: 'bg-blue-100 text-blue-800',
      sent: 'bg-yellow-100 text-yellow-800',
      pending: 'bg-yellow-100 text-yellow-800',
      failed: 'bg-red-100 text-red-800',
    };
    return statusClasses[status] || 'bg-gray-100 text-gray-600';
  }

  /**
   * Retourne le label traduit pour un statut
   */
  getStatusLabel(status: string): string {
    const statusLabels: { [key: string]: string } = {
      read: 'Lu',
      delivered: 'Livré',
      sent: 'Envoyé',
      pending: 'En attente',
      failed: 'Échec',
    };
    return statusLabels[status] || status || 'Inconnu';
  }

  /**
   * Vérifie si des filtres sont actifs
   */
  get hasActiveFilters(): boolean {
    return !!(this.searchTerm || this.deliveryStatusFilter || this.dateFromFilter || this.dateToFilter);
  }

  /**
   * Retourne le nombre total d'éléments (si disponible dans la réponse)
   */
  get totalElements(): number {
    // Si votre API retourne le nombre total d'éléments
    // return this.pageResponse?.totalElements || 0;
    return this.stats.total;
  }

  /**
   * Vérifie si la pagination doit être affichée
   */
  get shouldShowPagination(): boolean {
    return this.totalPages > 1 && !this.isLoadingSms;
  }

  /**
   * Vérifie si le tableau doit être affiché
   */
  get shouldShowTable(): boolean {
    return !this.isLoadingSms && this.smsList.length > 0;
  }

  /**
   * Vérifie si l'état vide doit être affiché
   */
  get shouldShowEmptyState(): boolean {
    return !this.isLoadingSms && this.smsList.length === 0;
  }
}
