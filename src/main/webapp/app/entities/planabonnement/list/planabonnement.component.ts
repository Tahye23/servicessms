import { Component, NgZone, inject, OnInit, OnDestroy } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, Observable, Subscription, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { ItemCountComponent } from 'app/shared/pagination';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { ITEMS_PER_PAGE, PAGE_HEADER, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { IPlanabonnement } from '../planabonnement.model';
import { EntityArrayResponseType, PlanabonnementService } from '../service/planabonnement.service';
import { PlanabonnementDeleteDialogComponent } from '../delete/planabonnement-delete-dialog.component';

@Component({
  standalone: true,
  selector: 'jhi-planabonnement',
  templateUrl: './planabonnement.component.html',
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
export class PlanabonnementComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  planabonnements?: IPlanabonnement[];
  filteredPlans?: IPlanabonnement[];
  isLoading = false;
  selectedFilter = '';

  sortState = sortStateSignal({});

  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;

  public router = inject(Router);
  protected planabonnementService = inject(PlanabonnementService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: IPlanabonnement): number => this.planabonnementService.getPlanabonnementIdentifier(item);

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
  }

  /**
   * Parse les fonctionnalités du plan
   */
  parseFeatures(features: string | null | undefined): string[] {
    if (!features) return [];
    return features
      .split(',')
      .map(feature => feature.trim())
      .filter(feature => feature.length > 0);
  }

  /**
   * Filtre les plans par type
   */
  filterByType(type: string): void {
    this.selectedFilter = type;
    this.applyFilter();
  }

  /**
   * Applique le filtre sélectionné
   */
  private applyFilter(): void {
    if (!this.planabonnements) {
      this.filteredPlans = [];
      return;
    }

    if (this.selectedFilter === '') {
      this.filteredPlans = [...this.planabonnements];
    } else {
      this.filteredPlans = this.planabonnements.filter(plan => plan.planType === this.selectedFilter);
    }
  }

  /**
   * Formate la période d'affichage
   */
  formatPeriod(period: string | null | undefined): string {
    if (!period) return '';

    const periodMap: { [key: string]: string } = {
      MONTHLY: 'mois',
      YEARLY: 'an',
      LIFETIME: 'à vie',
    };

    return periodMap[period] || period.toLowerCase();
  }

  /**
   * Formate les limites (gère les valeurs illimitées)
   */
  formatLimit(limit: number | null | undefined): string {
    if (limit === null || limit === undefined) return '0';
    if (limit === -1) return '∞';
    if (limit >= 1000000) return `${(limit / 1000000).toFixed(1)}M`;
    if (limit >= 1000) return `${(limit / 1000).toFixed(1)}K`;
    return limit.toString();
  }

  /**
   * Vérifie si le plan a des permissions définies
   */
  hasPermissions(plan: IPlanabonnement): boolean {
    return !!(plan.canManageUsers || plan.canManageTemplates || plan.canViewConversations || plan.canViewAnalytics || plan.prioritySupport);
  }

  /**
   * Obtient le nombre de plans actifs
   */
  getActivePlansCount(): number {
    if (!this.planabonnements) return 0;
    return this.planabonnements.filter(plan => plan.active === true).length;
  }

  /**
   * Obtient le nombre de plans populaires
   */
  getPopularPlansCount(): number {
    if (!this.planabonnements) return 0;
    return this.planabonnements.filter(plan => plan.abpPopular === true).length;
  }

  /**
   * Obtient le nombre de plans gratuits
   */
  getFreePlansCount(): number {
    if (!this.planabonnements) return 0;
    return this.planabonnements.filter(
      plan => plan.planType === 'FREE' || (plan.abpPrice !== null && plan.abpPrice !== undefined && plan.abpPrice === 0),
    ).length;
  }

  /**
   * TrackBy function for better performance
   */
  trackByPlanId = (_index: number, item: IPlanabonnement): number => {
    return item.id || 0;
  };

  /**
   * Bascule le statut actif/inactif d'un plan
   */
  toggleActive(plan: IPlanabonnement): void {
    if (!plan.id) return;

    this.planabonnementService.toggleActive(plan.id).subscribe({
      next: response => {
        if (response.body && this.planabonnements) {
          // Mettre à jour le plan dans la liste
          const index = this.planabonnements.findIndex(p => p.id === plan.id);
          if (index !== -1) {
            this.planabonnements[index] = response.body;
            this.applyFilter(); // Réappliquer le filtre
          }
        }
      },
      error: error => {
        console.error('Erreur lors du changement de statut:', error);
        // Optionnel: afficher un message d'erreur à l'utilisateur
      },
    });
  }

  /**
   * Supprime un plan d'abonnement
   */
  delete(planabonnement: IPlanabonnement): void {
    const modalRef = this.modalService.open(PlanabonnementDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.planabonnement = planabonnement;

    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  /**
   * Charge les données depuis le serveur
   */
  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
      error: error => {
        console.error('Erreur lors du chargement des plans:', error);
        this.isLoading = false;
      },
    });
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
    this.handleNavigation(page, this.sortState());
  }

  /**
   * Voir les détails d'un plan
   */
  view(plan: IPlanabonnement): void {
    if (plan.id) {
      this.router.navigate(['/planabonnement', plan.id, 'view']);
    }
  }

  /**
   * Modifier un plan
   */
  edit(plan: IPlanabonnement): void {
    if (plan.id) {
      this.router.navigate(['/planabonnement', plan.id, 'edit']);
    }
  }

  /**
   * Duplique un plan existant
   */
  duplicate(plan: IPlanabonnement): void {
    // Créer une copie du plan sans l'ID pour la duplication
    const duplicatedPlan = {
      ...plan,
      id: null,
      abpName: `${plan.abpName} (Copie)`,
      active: false, // Nouvelle copie inactive par défaut
      abpPopular: false, // Nouvelle copie non populaire par défaut
    };

    this.planabonnementService.create(duplicatedPlan).subscribe({
      next: () => {
        this.load(); // Recharger la liste
      },
      error: error => {
        console.error('Erreur lors de la duplication:', error);
      },
    });
  }

  /**
   * Export des plans vers CSV
   */
  exportToCSV(): void {
    if (!this.planabonnements || this.planabonnements.length === 0) return;

    const csvHeaders = [
      'ID',
      'Nom',
      'Type',
      'Prix',
      'Devise',
      'Période',
      'SMS Limite',
      'WhatsApp Limite',
      'Utilisateurs Limite',
      'Templates Limite',
      'Actif',
      'Populaire',
    ];

    const csvData = this.planabonnements.map(plan => [
      plan.id,
      plan.abpName,
      plan.planType,
      plan.abpPrice,
      plan.abpCurrency,
      plan.abpPeriod,
      plan.smsLimit,
      plan.whatsappLimit,
      plan.usersLimit,
      plan.templatesLimit,
      plan.active ? 'Oui' : 'Non',
      plan.abpPopular ? 'Oui' : 'Non',
    ]);

    const csvContent = [csvHeaders, ...csvData].map(row => row.map(field => `"${field || ''}"`).join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `plans-abonnement-${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page = +(page ?? 1);
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.planabonnements = dataFromBody;
    this.applyFilter(); // Appliquer le filtre après le chargement
  }

  protected fillComponentAttributesFromResponseBody(data: IPlanabonnement[] | null): IPlanabonnement[] {
    return data ?? [];
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER));
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    const { page } = this;

    this.isLoading = true;
    const pageToLoad: number = page;
    const queryObject: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    return this.planabonnementService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
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

  onPageChange(event: any): void {
    this.page = event.first / event.rows + 1; // PrimeNG paginates with 0-based index
    this.itemsPerPage = event.rows;
    this.load();
  }
}
