import { Component, NgZone, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, Observable, Subscription, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { FormsModule } from '@angular/forms';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { ITokensApp } from '../tokens-app.model';
import { EntityArrayResponseType, TokensAppService } from '../service/tokens-app.service';
import { TokensAppDeleteDialogComponent } from '../delete/tokens-app-delete-dialog.component';
import { TokenManagementModalComponent } from './token-management-modal.component';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { IContact } from '../../contact/contact.model';
import { ApplicationService } from '../../application/service/application.service';
import { NotificationService } from '../../../shared/notification.service';

@Component({
  standalone: true,
  selector: 'jhi-tokens-app',
  templateUrl: './tokens-app.component.html',
  imports: [
    RouterModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
  ],
})
export class TokensAppComponent implements OnInit {
  subscription: Subscription | null = null;
  tokensApps?: ITokensApp[];
  applications: any[] = [];
  isLoading = false;
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  first = 0;
  rows = 10;
  sortState = sortStateSignal({});
  private notificationService = inject(NotificationService);
  public router = inject(Router);
  protected tokensAppService = inject(TokensAppService);
  protected applicationService = inject(ApplicationService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: ITokensApp): number => this.tokensAppService.getTokensAppIdentifier(item);

  ngOnInit(): void {
    this.loadApplications();

    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => {
          if (!this.tokensApps || this.tokensApps.length === 0) {
            this.load();
          }
        }),
      )
      .subscribe();
  }

  loadApplications(): void {
    this.applicationService.query().subscribe({
      next: (res: any) => {
        this.applications = res.body || [];
      },
      error: error => {
        console.error('Erreur lors du chargement des applications:', error);
      },
    });
  }

  openCreateTokenModal(): void {
    const modalRef = this.modalService.open(TokenManagementModalComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });

    modalRef.componentInstance.applications = this.applications;

    modalRef.componentInstance.tokenCreated.subscribe((newToken: ITokensApp) => {
      this.load(); // Refresh the list
      // Don't close modal immediately to let user copy the token
    });
  }

  openEditTokenModal(tokensApp: ITokensApp): void {
    const modalRef = this.modalService.open(TokenManagementModalComponent, {
      size: 'lg',
      backdrop: 'static',
      centered: true,
    });

    modalRef.componentInstance.currentToken = tokensApp;
    modalRef.componentInstance.applications = this.applications;

    modalRef.componentInstance.tokenUpdated.subscribe(() => {
      this.load(); // Refresh the list
    });
  }

  delete(tokensApp: ITokensApp): void {
    const modalRef = this.modalService.open(TokensAppDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.tokensApp = tokensApp;
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load(): void {
    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
    });
  }

  view(tokensApp: ITokensApp): void {
    this.router.navigate(['/tokens-app', tokensApp.id, 'view']);
  }

  edit(tokensApp: ITokensApp): void {
    this.openEditTokenModal(tokensApp);
  }

  navigateToWithComponentValues(event: SortState): void {
    this.handleNavigation(this.page, event);
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.tokensApps = this.refineData(dataFromBody);
  }

  protected refineData(data: ITokensApp[]): ITokensApp[] {
    const { predicate, order } = this.sortState();
    return predicate && order ? data.sort(this.sortService.startSort({ predicate, order })) : data;
  }

  protected fillComponentAttributesFromResponseBody(data: ITokensApp[] | null): ITokensApp[] {
    return data ?? [];
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const queryObject: any = {
      sort: this.sortService.buildSortParam(this.sortState()),
    };
    return this.tokensAppService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
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

  // Quick action methods
  quickToggleStatus(tokensApp: ITokensApp): void {
    if (tokensApp.active) {
      this.deactivateToken(tokensApp);
    } else {
      this.activateToken(tokensApp);
    }
  }

  regenerateToken(tokensApp: ITokensApp): void {
    this.notificationService
      .confirm("Êtes-vous sûr de vouloir régénérer ce token ? L'ancien token ne fonctionnera plus.", 'Régénérer le token', 'warning')
      .subscribe(confirmed => {
        if (confirmed) {
          this.tokensAppService.regenerateToken(tokensApp.id).subscribe({
            next: () => {
              this.load();
              this.notificationService.success('Token régénéré avec succès !');
            },
            error: err => {
              console.error(err);
              this.notificationService.confirm('Erreur lors de la régénération du token', 'Erreur', 'danger');
            },
          });
        }
      });
  }

  activateToken(tokensApp: ITokensApp): void {
    this.tokensAppService.activateToken(tokensApp.id).subscribe({
      next: () => {
        this.load();
        this.notificationService.success('Token activé avec succès !');
      },
      error: err => {
        console.error(err);
        this.notificationService.confirm("Erreur lors de l'activation du token", 'Erreur', 'danger');
      },
    });
  }

  deactivateToken(tokensApp: ITokensApp): void {
    this.notificationService
      .confirm('Êtes-vous sûr de vouloir désactiver ce token ?', 'Désactiver le token', 'warning')
      .subscribe(confirmed => {
        if (confirmed) {
          this.tokensAppService.deactivateToken(tokensApp.id).subscribe({
            next: () => {
              this.load();
              this.notificationService.success('Token désactivé avec succès !');
            },
            error: err => {
              console.error(err);
              this.notificationService.confirm('Erreur lors de la désactivation du token', 'Erreur', 'danger');
            },
          });
        }
      });
  }

  copyTokenToClipboard(token: string): void {
    navigator.clipboard
      .writeText(token)
      .then(() => {
        this.notificationService.success('Token copié dans le presse-papiers !');
      })
      .catch(() => {
        const textArea = document.createElement('textarea');
        textArea.value = token;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        this.notificationService.success('Token copié dans le presse-papiers !');
      });
  }

  generateToken(tokensApp: ITokensApp): void {
    this.tokensAppService.generateToken(tokensApp.id).subscribe({
      next: () => {
        this.load();
        this.notificationService.success('Token généré avec succès !');
      },
      error: () => {
        this.notificationService.confirm('Erreur lors de la génération du token', 'Erreur', 'danger');
      },
    });
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState());
  }

  getStatusBadgeClass(active: boolean): string {
    return active ? 'px-2 py-1 text-xs rounded-full bg-green-100 text-green-800' : 'px-2 py-1 text-xs rounded-full bg-red-100 text-red-800';
  }

  getExpirationStatus(dateExpiration: any): 'expired' | 'expiring-soon' | 'valid' {
    if (!dateExpiration) return 'valid';

    const now = new Date();
    const expiration = new Date(dateExpiration);
    const daysUntilExpiration = Math.ceil((expiration.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

    if (daysUntilExpiration < 0) return 'expired';
    if (daysUntilExpiration <= 30) return 'expiring-soon';
    return 'valid';
  }

  getExpirationBadgeClass(dateExpiration: any): string {
    const status = this.getExpirationStatus(dateExpiration);

    switch (status) {
      case 'expired':
        return 'px-2 py-1 text-xs rounded-full bg-red-100 text-red-800';
      case 'expiring-soon':
        return 'px-2 py-1 text-xs rounded-full bg-orange-100 text-orange-800';
      default:
        return 'px-2 py-1 text-xs rounded-full bg-green-100 text-green-800';
    }
  }

  // Getters pour les statistiques
  get activeTokensCount(): number {
    return this.tokensApps?.filter(token => token.active).length || 0;
  }

  get expiringSoonTokensCount(): number {
    return this.tokensApps?.filter(token => this.getExpirationStatus(token.dateExpiration) === 'expiring-soon').length || 0;
  }

  get expiredTokensCount(): number {
    return this.tokensApps?.filter(token => this.getExpirationStatus(token.dateExpiration) === 'expired').length || 0;
  }

  get totalTokensCount(): number {
    return this.tokensApps?.length || 0;
  }

  protected readonly Math = Math;
}
