import { Component, NgZone, inject, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, Observable, Subscription, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';

import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { FormsModule } from '@angular/forms';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { IApplication, IApplicationStats, ITokensApp } from '../application.model';
import { EntityArrayResponseType, ApplicationService } from '../service/application.service';
import { ApplicationDeleteDialogComponent } from '../delete/application-delete-dialog.component';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';

@Component({
  standalone: true,
  selector: 'jhi-application',
  templateUrl: './application.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    CommonModule,
  ],
})
export class ApplicationComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  applications?: IApplication[];
  isLoading = false;

  // Statistiques
  totalApplications = 0;
  activeApplications = 0;
  todayApiCalls = 0;
  monthlyApiCalls = 0;

  // Pagination
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  first = 0;
  rows = 10;

  // État des messages
  successMessage: string | null = null;
  errorMessage: string | null = null;

  sortState = sortStateSignal({});

  // Services injectés
  public router = inject(Router);
  protected applicationService = inject(ApplicationService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: IApplication): number => this.applicationService.getApplicationIdentifier(item);

  ngOnInit(): void {
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => {
          if (!this.applications || this.applications.length === 0) {
            this.load();
          }
        }),
      )
      .subscribe();

    // Charger les statistiques
    this.loadStats();
  }

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }

  // ==================== GESTION DES TOKENS ====================

  /**
   * Récupère le token actif d'une application
   * ✅ CORRECTION: Gestion null appropriée
   */
  getActiveToken(application: IApplication | null | undefined): ITokensApp | null {
    if (!application?.tokens || application.tokens.length === 0) {
      return null;
    }

    return application.tokens.find(t => t.active && !t.isExpired) || null;
  }

  /**
   * Récupère le token à afficher (actif ou premier disponible)
   * ✅ CORRECTION: Utilise l'opérateur optionnel chain pour éviter les erreurs
   */
  getDisplayToken(application: IApplication | null | undefined): string | null {
    if (!application?.tokens || application.tokens.length === 0) return null;
    const activeToken = application.tokens.find(t => t.active && !t.isExpired);
    return activeToken?.token ?? application.tokens[0]?.token ?? null;
  }

  /**
   * Copie le token dans le presse-papiers
   * ✅ CORRECTION: Gestion complète des erreurs
   */
  async copyToken(token: string | null | undefined): Promise<void> {
    if (!token) {
      this.showErrorMessage('Token indisponible');
      return;
    }

    try {
      await navigator.clipboard.writeText(token);
      this.showSuccessMessage('Token copié dans le presse-papiers');
    } catch (err) {
      this.showErrorMessage('Erreur lors de la copie du token');
      console.error('Erreur de copie:', err);
    }
  }

  /**
   * Régénère le token d'une application
   * ✅ CORRECTION: Vérifications null et gestion d'erreur
   */
  regenerateToken(application: IApplication | null | undefined): void {
    if (!application?.id) {
      this.showErrorMessage("L'ID de l'application est manquant");
      return;
    }

    const activeToken = this.getActiveToken(application);

    if (!activeToken?.id) {
      this.showErrorMessage('Aucun token actif à régénérer');
      return;
    }

    if (confirm("Êtes-vous sûr de vouloir régénérer le token ? L'ancien token ne fonctionnera plus.")) {
      this.applicationService.regenerateToken(activeToken.id).subscribe({
        next: response => {
          if (response?.body) {
            this.showSuccessMessage('Token régénéré avec succès');
            this.load(); // Recharger pour afficher le nouveau token
          }
        },
        error: error => {
          const message = error?.error?.message || 'Erreur lors de la régénération du token';
          this.showErrorMessage(message);
          console.error('Erreur de régénération:', error);
        },
      });
    }
  }

  /**
   * Crée un nouveau token pour une application
   * ✅ CORRECTION: Vérifications null et gestion d'erreur
   */
  createToken(application: IApplication | null | undefined): void {
    if (!application?.id) {
      this.showErrorMessage("L'ID de l'application est manquant");
      return;
    }

    const expirationDate = new Date();
    expirationDate.setFullYear(expirationDate.getFullYear() + 1); // Expire dans 1 an

    this.applicationService
      .createToken({
        applicationId: application.id,
        dateExpiration: expirationDate.toISOString(),
        active: true,
      })
      .subscribe({
        next: response => {
          if (response?.body) {
            this.showSuccessMessage('Token créé avec succès');
            this.load();
          }
        },
        error: error => {
          const message = error?.error?.message || 'Erreur lors de la création du token';
          this.showErrorMessage(message);
          console.error('Erreur de création:', error);
        },
      });
  }

  /**
   * Vérifie si une application a un token
   */
  hasToken(application: IApplication | null | undefined): boolean {
    return !!this.getDisplayToken(application);
  }

  /**
   * Vérifie si le token est expiré
   */
  isTokenExpired(application: IApplication | null | undefined): boolean {
    const activeToken = this.getActiveToken(application);
    return activeToken?.isExpired || false;
  }

  // ==================== GESTION DES APPLICATIONS ====================

  /**
   * Supprime une application
   */
  delete(application: IApplication | null | undefined): void {
    if (!application) {
      this.showErrorMessage('Application non disponible');
      return;
    }

    const modalRef = this.modalService.open(ApplicationDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.application = application;

    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => {
          this.showSuccessMessage('Application supprimée avec succès');
          this.load();
          this.loadStats();
        }),
      )
      .subscribe();
  }

  /**
   * Charge les applications
   * ✅ CORRECTION: Gestion d'erreur améliorée
   */
  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
      error: error => {
        this.isLoading = false;
        const message = error?.error?.message || 'Erreur lors du chargement des applications';
        this.showErrorMessage(message);
        console.error('Erreur de chargement:', error);
      },
    });
  }

  /**
   * Charge les statistiques des applications
   * ✅ CORRECTION: Gestion d'erreur et endpoint /stats
   */
  loadStats(): void {
    this.applicationService.getStats().subscribe({
      next: (stats: IApplicationStats) => {
        this.totalApplications = stats.totalApplications || 0;
        this.activeApplications = stats.activeApplications || 0;
        this.todayApiCalls = stats.todayApiCalls || 0;
        this.monthlyApiCalls = stats.monthlyApiCalls || 0;
      },
      error: error => {
        console.error('Erreur lors du chargement des statistiques:', error);
        // Ne pas afficher d'erreur bloquante pour les stats
      },
    });
  }

  /**
   * Affiche les détails d'une application
   */
  view(application: IApplication | null | undefined): void {
    if (!application?.id) {
      this.showErrorMessage("L'ID de l'application est manquant");
      return;
    }
    this.router.navigate(['/application', application.id, 'view']);
  }

  /**
   * Édite une application
   */
  edit(application: IApplication | null | undefined): void {
    if (!application?.id) {
      this.showErrorMessage("L'ID de l'application est manquant");
      return;
    }
    this.router.navigate(['/application', application.id, 'edit']);
  }

  /**
   * Active/désactive une application
   */
  toggleApplicationStatus(application: IApplication | null | undefined): void {
    if (!application?.id) {
      this.showErrorMessage("L'ID de l'application est manquant");
      return;
    }

    const newStatus = !application.isActive;
    const action = newStatus ? 'activer' : 'désactiver';

    if (confirm(`Êtes-vous sûr de vouloir ${action} cette application ?`)) {
      this.applicationService.updateStatus(application.id, newStatus).subscribe({
        next: () => {
          application.isActive = newStatus;
          this.showSuccessMessage(`Application ${newStatus ? 'activée' : 'désactivée'} avec succès`);
          this.loadStats();
        },
        error: error => {
          const message = error?.error?.message || 'Erreur lors de la modification du statut';
          this.showErrorMessage(message);
          console.error('Erreur de mise à jour:', error);
        },
      });
    }
  }

  // ==================== UTILITAIRES D'AFFICHAGE ====================

  /**
   * Obtient la couleur de l'environnement
   */
  getEnvironmentColor(environment: string | null | undefined): string {
    if (!environment) return 'bg-gray-100 text-gray-800';

    const colors: { [key: string]: string } = {
      development: 'bg-blue-100 text-blue-800',
      staging: 'bg-yellow-100 text-yellow-800',
      production: 'bg-green-100 text-green-800',
    };
    return colors[environment] || 'bg-gray-100 text-gray-800';
  }

  /**
   * Calcule le pourcentage d'utilisation
   */
  getUsagePercentage(current: number | null | undefined, limit: number | null | undefined): number {
    if (!current || !limit || limit === 0) return 0;
    return Math.min((current / limit) * 100, 100);
  }

  /**
   * Obtient la couleur de la barre de progression selon l'utilisation
   */
  getUsageBarColor(percentage: number): string {
    if (percentage >= 90) return 'bg-red-500';
    if (percentage >= 75) return 'bg-yellow-500';
    return 'bg-blue-500';
  }

  /**
   * Formate un nombre avec des séparateurs de milliers
   */
  formatNumber(num: number | null | undefined): string {
    if (!num) return '0';
    return new Intl.NumberFormat('fr-FR').format(num);
  }

  /**
   * Formate la date de dernière utilisation
   */
  formatLastApiCall(lastCall: string | null | undefined): string {
    if (!lastCall) return 'Jamais';

    const date = new Date(lastCall);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return "À l'instant";
    if (diffInMinutes < 60) return `Il y a ${diffInMinutes} min`;
    if (diffInMinutes < 1440) return `Il y a ${Math.floor(diffInMinutes / 60)} h`;
    return `Il y a ${Math.floor(diffInMinutes / 1440)} j`;
  }

  /**
   * Obtient les services formatés pour l'affichage
   */
  getFormattedServices(services: string[] | null | undefined): string {
    if (!services || services.length === 0) return 'Aucun service';

    const serviceNames: { [key: string]: string } = {
      sms: 'SMS',
      whatsapp: 'WhatsApp',
      email: 'Email',
      voice: 'Vocal',
    };

    return services.map(service => serviceNames[service] || service).join(', ');
  }

  /**
   * Vérifie si une application a des limites configurées
   */
  hasLimits(application: IApplication | null | undefined): boolean {
    return !!(application?.dailyLimit || application?.monthlyLimit);
  }

  /**
   * Vérifie si une application approche de ses limites
   */
  isNearLimit(application: IApplication | null | undefined): boolean {
    if (!application) return false;

    if (application.dailyLimit && application.currentDailyUsage) {
      const dailyPercentage = (application.currentDailyUsage / application.dailyLimit) * 100;
      if (dailyPercentage >= 80) return true;
    }

    if (application.monthlyLimit && application.currentMonthlyUsage) {
      const monthlyPercentage = (application.currentMonthlyUsage / application.monthlyLimit) * 100;
      if (monthlyPercentage >= 80) return true;
    }

    return false;
  }

  // ==================== ACTIONS GLOBALES ====================

  /**
   * Rafraîchit toutes les données
   */
  refresh(): void {
    this.load();
    this.loadStats();
  }

  /**
   * Navigue vers la création d'une nouvelle application
   */
  createNew(): void {
    this.router.navigate(['/application/new']);
  }

  // ==================== GESTION DES MESSAGES ====================

  /**
   * Affiche un message de succès
   */
  private showSuccessMessage(message: string): void {
    this.successMessage = message;
    this.errorMessage = null;
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

  // ==================== NAVIGATION ET PAGINATION ====================

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page, event);
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState());
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.applications = this.refineData(dataFromBody);

    // Mettre à jour les statistiques locales
    this.updateLocalStats();
  }

  protected refineData(data: IApplication[]): IApplication[] {
    const { predicate, order } = this.sortState();
    return predicate && order ? data.sort(this.sortService.startSort({ predicate, order })) : data;
  }

  protected fillComponentAttributesFromResponseBody(data: IApplication[] | null): IApplication[] {
    return data ?? [];
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    this.clearMessages();

    const queryObject: any = {
      sort: this.sortService.buildSortParam(this.sortState()),
      page: this.page - 1,
      size: this.itemsPerPage,
    };

    return this.applicationService.query(queryObject).pipe(
      tap(() => {
        this.isLoading = false;
      }),
    );
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

  /**
   * Met à jour les statistiques locales basées sur les applications chargées
   */
  private updateLocalStats(): void {
    if (this.applications) {
      this.totalApplications = this.applications.length;
      this.activeApplications = this.applications.filter(app => app.isActive).length;

      this.todayApiCalls = this.applications.reduce((sum, app) => {
        return sum + (app.currentDailyUsage || 0);
      }, 0);

      this.monthlyApiCalls = this.applications.reduce((sum, app) => {
        return sum + (app.currentMonthlyUsage || 0);
      }, 0);
    }
  }
}
