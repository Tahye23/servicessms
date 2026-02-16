import { Component, NgZone, inject, OnInit, computed, OnDestroy } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, Observable, Subscription, tap, debounceTime, distinctUntilChanged } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { ItemCountComponent } from 'app/shared/pagination';

import { ITEMS_PER_PAGE, PAGE_HEADER, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { IGroupe } from '../groupe.model';
import { EntityArrayResponseType, GroupeService } from '../service/groupe.service';
import { GroupeDeleteDialogComponent } from '../delete/groupe-delete-dialog.component';
import { IContact } from '../../contact/contact.model';
import { AccountService } from '../../../core/auth/account.service';

@Component({
  standalone: true,
  selector: 'jhi-groupe',
  templateUrl: './groupe.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    ItemCountComponent,
    CommonModule,
  ],
})
export class GroupeComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  searchSubscription: Subscription | null = null;

  groupes?: IGroupe[];
  isLoading = false;
  contacts: IContact[] = [];

  sortState = sortStateSignal({});
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));

  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  totalPages: number = 1;

  // Propriété pour Math dans le template
  Math = Math;

  // Messages d'état
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Terme de recherche avec debounce
  private _searchTerm: string = '';
  get searchTerm(): string {
    return this._searchTerm;
  }
  set searchTerm(value: string) {
    this._searchTerm = value;
  }

  // Filtres
  selectedGroupType: string = '';
  selectedStatus: string = '';

  // Services injectés
  private accountService = inject(AccountService);
  public router = inject(Router);
  protected groupeService = inject(GroupeService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: IGroupe): number => this.groupeService.getGroupeIdentifier(item);

  ngOnInit(): void {
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    if (this.searchSubscription) {
      this.searchSubscription.unsubscribe();
    }
  }

  /**
   * Supprime un groupe avec confirmation
   */
  delete(groupe: IGroupe): void {
    const modalRef = this.modalService.open(GroupeDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.groupe = groupe;

    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => {
          this.showSuccessMessage('Groupe supprimé avec succès');
          this.load();
        }),
      )
      .subscribe();
  }

  /**
   * Affiche les détails d'un groupe
   */
  view(groupe: IGroupe): void {
    this.router.navigate(['/groupe', groupe.id, 'view']);
  }

  /**
   * Édite un groupe
   */
  edit(groupe: IGroupe): void {
    this.router.navigate(['/groupe', groupe.id, 'edit']);
  }

  /**
   * Charge les données des groupes
   */
  load(): void {
    this.clearMessages();
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
      error: err => {
        this.isLoading = false;
        this.showErrorMessage('Erreur lors du chargement des données.');
        console.error('Erreur de chargement:', err);
      },
    });
  }

  /**
   * Recherche avec debounce pour éviter trop de requêtes
   */
  onSearch(): void {
    this.page = 1; // Réinitialiser la page lors d'une nouvelle recherche
    this.load();
  }

  /**
   * Gestion des changements de filtres
   */
  onFilterChange(): void {
    this.page = 1; // Réinitialiser la page lors d'un changement de filtre
    this.load();
  }

  /**
   * Vérifie s'il y a des filtres actifs
   */
  hasActiveFilters(): boolean {
    return !!(this.selectedGroupType || this.selectedStatus || this.searchTerm);
  }

  /**
   * Remet à zéro tous les filtres
   */
  resetFilters(): void {
    this.searchTerm = '';
    this.selectedGroupType = '';
    this.selectedStatus = '';
    this.page = 1;
    this.load();
  }

  /**
   * Détermine si un groupe est un groupe de test
   */
  isTestGroup(groupe: IGroupe): boolean {
    // Vérifie si groupType existe et contient 'test'
    if (groupe.groupType && groupe.groupType.toLowerCase().includes('test')) {
      return true;
    }

    return false;
  }

  /**
   * Navigation avec tri
   */
  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page, event);
  }

  /**
   * Navigation vers une page spécifique
   */
  navigateToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.handleNavigation(page, this.sortState());
    }
  }

  /**
   * Génère les numéros de pages pour la pagination
   */
  getPaginationPages(): number[] {
    const totalPages = this.totalPages;
    const pages: number[] = [];
    const maxVisiblePages = 5;

    let startPage = Math.max(1, this.page - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

    // Ajuster startPage si endPage est à la fin
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  /**
   * Vérifie si nous sommes sur la première page
   */
  isFirstPage(): boolean {
    return this.page === 1;
  }

  /**
   * Vérifie si nous sommes sur la dernière page
   */
  isLastPage(): boolean {
    return this.page >= this.totalPages;
  }

  /**
   * Calcule l'index de début pour l'affichage
   */
  getStartIndex(): number {
    return (this.page - 1) * this.itemsPerPage + 1;
  }

  /**
   * Calcule l'index de fin pour l'affichage
   */
  getEndIndex(): number {
    return Math.min(this.page * this.itemsPerPage, this.totalItems);
  }

  /**
   * Efface la recherche
   */
  clearSearch(): void {
    this.searchTerm = '';
    this.onSearch();
  }

  /**
   * Rafraîchit les données
   */
  refresh(): void {
    this.load();
  }

  /**
   * Navigue vers la création d'un nouveau groupe
   */

  /**
   * Affiche un message de succès
   */
  private showSuccessMessage(message: string): void {
    this.successMessage = message;
    this.errorMessage = null;
    // Auto-hide après 5 secondes
    setTimeout(() => {
      this.successMessage = null;
    }, 5000);
  }

  /**
   * Affiche un message d'erreur
   */
  private showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.successMessage = null;
    // Auto-hide après 8 secondes
    setTimeout(() => {
      this.errorMessage = null;
    }, 8000);
  }

  /**
   * Efface tous les messages
   */
  private clearMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }

  // Méthodes protégées existantes

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page = +(page ?? 1);

    // Récupérer le terme de recherche depuis les paramètres d'URL
    const search = params.get('search');
    this.searchTerm = search ?? '';

    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.groupes = dataFromBody;

    // Calculer le nombre total de pages
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
  }

  protected fillComponentAttributesFromResponseBody(data: IGroupe[] | null): IGroupe[] {
    return data ?? [];
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER)) || 0;
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;

    const queryObject: any = {
      page: this.page - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    // Ajouter le terme de recherche seulement s'il n'est pas vide
    if (this.searchTerm && this.searchTerm.trim()) {
      queryObject.search = this.searchTerm.trim();
    }

    // Ajouter les filtres
    if (this.selectedGroupType) {
      queryObject.groupType = this.selectedGroupType;
    }

    return this.groupeService.query(queryObject).pipe(
      tap(() => {
        this.isLoading = false;
      }),
    );
  }

  protected handleNavigation(page: number, sortState: SortState): void {
    const queryParamsObj: any = {
      page,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(sortState),
    };

    // Inclure le terme de recherche dans la navigation
    if (this.searchTerm && this.searchTerm.trim()) {
      queryParamsObj.search = this.searchTerm.trim();
    }

    this.ngZone.run(() => {
      this.router.navigate(['./'], {
        relativeTo: this.activatedRoute,
        queryParams: queryParamsObj,
      });
    });
  }

  /**
   * Gestion de la pagination avec PrimeNG (si utilisé)
   */
  onPageChange(event: any): void {
    this.page = event.first / event.rows + 1;
    this.itemsPerPage = event.rows;
    this.load();
  }

  /**
   * Obtient la couleur de statut pour un groupe
   */
  getGroupStatusColor(groupe: IGroupe): string {
    if (groupe.extendedUser) {
      return 'text-green-600 bg-green-50';
    }
    return 'text-yellow-600 bg-yellow-50';
  }

  /**
   * Obtient le texte de statut pour un groupe
   */
  getGroupStatusText(groupe: IGroupe): string {
    if (groupe.extendedUser) {
      return 'Assigné';
    }
    return 'Non assigné';
  }

  /**
   * Formate le titre d'un groupe pour l'affichage
   */
  formatGroupTitle(title: string | null | undefined): string {
    if (!title) {
      return 'Groupe sans titre';
    }
    return title.length > 30 ? title.substring(0, 30) + '...' : title;
  }
}
