import { Component, inject, OnInit, signal, WritableSignal, computed, OnDestroy } from '@angular/core';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { HttpResponse, HttpHeaders } from '@angular/common/http';
import { combineLatest, Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import SharedModule from 'app/shared/shared.module';
import { SortDirective, SortByDirective, sortStateSignal, SortService, SortState } from 'app/shared/sort';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { SORT } from 'app/config/navigation.constants';
import { ItemCountComponent } from 'app/shared/pagination';
import { AccountService } from 'app/core/auth/account.service';
import { UserManagementService } from '../service/user-management.service';
import { User } from '../user-management.model';
import UserManagementDeleteDialogComponent from '../delete/user-management-delete-dialog.component';

@Component({
  standalone: true,
  selector: 'jhi-user-mgmt',
  templateUrl: './user-management.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    UserManagementDeleteDialogComponent,
    SortDirective,
    SortByDirective,
    ItemCountComponent,
  ],
})
export default class UserManagementComponent implements OnInit, OnDestroy {
  // Signaux pour la gestion d'état
  users: WritableSignal<User[]> = signal([]);
  isLoading = signal(false);
  totalItems = signal(0);
  availableRoles = signal<string[]>([]);
  // Paramètres de pagination
  itemsPerPage = ITEMS_PER_PAGE;
  page = 1;
  sortState = sortStateSignal({});

  // Filtres et recherche
  searchTerm = '';
  statusFilter = '';
  roleFilter = ''; // Gardé pour l'affichage mais pas envoyé au backend

  // Subject pour la destruction du composant
  private destroy$ = new Subject<void>();
  private searchSubject = new Subject<string>();

  // Injection de dépendances
  currentAccount = inject(AccountService).trackCurrentAccount();
  private userService = inject(UserManagementService);
  private activatedRoute = inject(ActivatedRoute);
  private router = inject(Router);
  private sortService = inject(SortService);
  private modalService = inject(NgbModal);

  // Signal calculé pour les utilisateurs paginés (directement du backend)
  paginatedUsers = computed(() => {
    // Les utilisateurs sont déjà filtrés et paginés par le backend
    return this.users();
  });

  ngOnInit(): void {
    this.initializeSearchDebounce();
    this.loadAvailableRoles();
    this.handleNavigation();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  /**
   * Charge les rôles disponibles selon l'utilisateur connecté
   */
  loadAvailableRoles(): void {
    this.userService.getAvailableRoles().subscribe({
      next: roles => {
        this.availableRoles.set(roles);
      },
      error: error => {
        console.error('Erreur lors du chargement des rôles:', error);
      },
    });
  }

  /**
   * Initialise le debounce pour la recherche
   */
  private initializeSearchDebounce(): void {
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$)).subscribe(searchTerm => {
      this.searchTerm = searchTerm;
      this.page = 1; // Reset à la première page lors d'une recherche
      this.loadAll();
    });
  }

  /**
   * Gestion de la recherche avec debounce
   */
  onSearch(): void {
    this.searchSubject.next(this.searchTerm);
  }

  /**
   * Application des filtres
   */
  applyFilters(): void {
    this.page = 1; // Reset à la première page lors du filtrage
    this.loadAll();
  }

  /**
   * Changement du nombre d'éléments par page
   */
  onItemsPerPageChange(): void {
    this.page = 1;
    this.loadAll();
  }

  /**
   * Active/désactive un utilisateur
   */
  setActive(user: User, isActivated: boolean): void {
    this.isLoading.set(true);
    this.userService.update({ ...user, activated: isActivated }).subscribe({
      next: () => {
        this.loadAll();
        // Optionnel : afficher une notification de succès
      },
      error: error => {
        this.isLoading.set(false);
        console.error("Erreur lors de la mise à jour de l'utilisateur:", error);
        // Optionnel : afficher une notification d'erreur
      },
    });
  }

  /**
   * Suivi de l'identité pour ngFor
   */
  trackIdentity(_index: number, item: User): number {
    return item.id!;
  }

  /**
   * Navigation vers une page spécifique
   */
  navigateToPage(page: number): void {
    if (page >= 1 && page <= this.getTotalPages()) {
      this.page = page;
      this.loadAll();
    }
  }

  /**
   * Suppression d'un utilisateur
   */
  deleteUser(user: User): void {
    const modalRef = this.modalService.open(UserManagementDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.user = user;

    modalRef.closed.subscribe(reason => {
      if (reason === 'deleted') {
        this.loadAll();
      }
    });
  }

  /**
   * Chargement de tous les utilisateurs
   */
  loadAll(): void {
    this.isLoading.set(true);

    // Préparer les paramètres de requête
    const params: any = {
      page: this.page - 1, // backend zéro-based
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    // Ajouter search seulement s'il y a une valeur
    if (this.searchTerm && this.searchTerm.trim()) {
      params.search = this.searchTerm.trim();
    }

    // Ajouter status seulement s'il y a une valeur
    if (this.statusFilter) {
      params.status = this.statusFilter;
    }

    // Ajouter role seulement s'il y a une valeur
    if (this.roleFilter) {
      params.role = this.roleFilter;
    }

    this.userService.query(params).subscribe({
      next: (res: HttpResponse<User[]>) => {
        this.isLoading.set(false);
        this.onSuccess(res.body, res.headers);
      },
      error: error => {
        this.isLoading.set(false);
        console.error('Erreur lors du chargement des utilisateurs:', error);
      },
    });
  }

  /**
   * Récupère tous les rôles disponibles - MODIFIÉE
   */
  getAllRoles(): string[] {
    return this.availableRoles();
  }

  /**
   * Transition avec mise à jour de l'URL
   */
  private updateUrl(): void {
    const queryParams: any = {
      page: this.page,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    // Ajouter search seulement s'il y a une valeur
    if (this.searchTerm && this.searchTerm.trim()) {
      queryParams.search = this.searchTerm.trim();
    }

    // Ajouter status seulement s'il y a une valeur
    if (this.statusFilter) {
      queryParams.status = this.statusFilter;
    }

    // Ne pas inclure role car il est géré automatiquement côté backend

    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute.parent,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Gestion de la navigation depuis l'URL
   */
  private handleNavigation(): void {
    combineLatest([this.activatedRoute.data, this.activatedRoute.queryParamMap])
      .pipe(takeUntil(this.destroy$))
      .subscribe(([data, params]) => {
        const page = params.get('page');
        this.page = +(page ?? 1);

        this.searchTerm = params.get('search') ?? '';
        this.statusFilter = params.get('status') ?? '';

        this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data['defaultSort']));
        this.loadAll();
      });
  }

  /**
   * Gestion du succès du chargement
   */
  private onSuccess(users: User[] | null, headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get('X-Total-Count')) || (users?.length ?? 0));
    this.users.set(users ?? []);
  }

  // Méthodes utilitaires pour les statistiques - LIMITÉES À LA PAGE ACTUELLE

  /**
   * Nombre d'utilisateurs actifs sur la page actuelle
   */
  getActiveUsersCount(): number {
    return this.users().filter(user => user.activated).length;
  }

  /**
   * Nombre d'utilisateurs inactifs sur la page actuelle
   */
  getInactiveUsersCount(): number {
    return this.users().filter(user => !user.activated).length;
  }

  /**
   * Nombre d'utilisateurs créés récemment sur la page actuelle
   */
  getRecentUsersCount(): number {
    const oneWeekAgo = new Date();
    oneWeekAgo.setDate(oneWeekAgo.getDate() - 7);

    return this.users().filter(user => {
      if (!user.createdDate) return false;
      const createdDate = new Date(user.createdDate);
      return createdDate >= oneWeekAgo;
    }).length;
  }

  // Méthodes utilitaires pour la pagination

  /**
   * Index de début pour l'affichage
   */
  getStartIndex(): number {
    if (this.totalItems() === 0) return 0;
    return (this.page - 1) * this.itemsPerPage + 1;
  }

  /**
   * Index de fin pour l'affichage
   */
  getEndIndex(): number {
    const end = this.page * this.itemsPerPage;
    return Math.min(end, this.totalItems());
  }

  /**
   * Nombre total de pages
   */
  getTotalPages(): number {
    return Math.ceil(this.totalItems() / this.itemsPerPage);
  }

  /**
   * Gestion du changement de page via input mobile
   */
  onPageInputChange(): void {
    const newPage = Math.max(1, Math.min(this.page, this.getTotalPages()));
    if (newPage !== this.page) {
      this.page = newPage;
    }
    this.navigateToPage(this.page);
  }

  /**
   * Pages visibles pour la navigation rapide (responsive)
   */
  getVisiblePages(): number[] {
    const totalPages = this.getTotalPages();
    const currentPage = this.page;
    const pages: number[] = [];

    // Responsive: moins de pages sur mobile
    const isSmallScreen = window.innerWidth < 768;
    const maxVisible = isSmallScreen ? 3 : 5;

    let start = Math.max(1, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);

    // Ajustement si on est proche du début ou de la fin
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  /**
   * Exporte les utilisateurs de la page actuelle en CSV
   */
  exportToCsv(): void {
    const users = this.users();
    const csvContent = this.convertToCSV(users);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');

    if (link.download !== undefined) {
      const url = URL.createObjectURL(blob);
      link.setAttribute('href', url);
      link.setAttribute('download', `utilisateurs_page_${this.page}_${new Date().toISOString().split('T')[0]}.csv`);
      link.style.visibility = 'hidden';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }

  /**
   * Convertit les utilisateurs en format CSV
   */
  private convertToCSV(users: User[]): string {
    const headers = ['ID', 'Login', 'Email', 'Prénom', 'Nom', 'Statut', 'Rôles', 'Date de création'];
    const csvRows = [headers.join(',')];

    users.forEach(user => {
      const row = [
        user.id || '',
        user.login || '',
        user.email || '',
        user.firstName || '',
        user.lastName || '',
        user.activated ? 'Actif' : 'Inactif',
        user.authorities?.join(';') || '',
        user.createdDate ? new Date(user.createdDate).toLocaleDateString('fr-FR') : '',
      ];
      csvRows.push(row.map(field => `"${field}"`).join(','));
    });

    return csvRows.join('\n');
  }

  // Référence Math pour le template
  protected readonly Math = Math;
}
