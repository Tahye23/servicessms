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

import { ITEMS_PER_PAGE, PAGE_HEADER, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import { SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { IAbonnement } from '../abonnement.model';
import { EntityArrayResponseType, AbonnementService } from '../service/abonnement.service';
import { AbonnementDeleteDialogComponent } from '../delete/abonnement-delete-dialog.component';

@Component({
  standalone: true,
  selector: 'jhi-abonnement',
  templateUrl: './abonnement.component.html',
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
  ],
})
export class AbonnementComponent implements OnInit, OnDestroy {
  abonnements?: IAbonnement[];
  isLoading = false;

  sortState = sortStateSignal({});

  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  totalPages = 0;
  private subscription: Subscription | null = null;

  protected router = inject(Router);
  protected abonnementService = inject(AbonnementService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: IAbonnement): number => this.abonnementService.getAbonnementIdentifier(item);

  ngOnInit(): void {
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  delete(abonnement: IAbonnement): void {
    const modalRef = this.modalService.open(AbonnementDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.abonnement = abonnement;
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load(): void {
    this.queryBackend().subscribe({
      next: res => this.onResponseSuccess(res),
      error: () => (this.isLoading = false),
    });
  }

  view(abonnement: IAbonnement): void {
    this.router.navigate(['/abonnement', abonnement.id, 'view']);
  }

  edit(abonnement: IAbonnement): void {
    this.router.navigate(['/abonnement', abonnement.id, 'edit']);
  }

  navigateToWithComponentValues(sortState: SortState): void {
    this.handleNavigation(this.page, sortState);
  }

  navigateToPage(page: number): void {
    this.handleNavigation(page, this.sortState());
  }

  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const pageParam = params.get(PAGE_HEADER);
    this.page = pageParam !== null ? +pageParam : 1;

    const sortParam = params.get(SORT) ?? data[DEFAULT_SORT_DATA];
    this.sortState.set(this.sortService.parseSortParam(sortParam));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.totalItems = Number(response.headers.get(TOTAL_COUNT_RESPONSE_HEADER));
    this.abonnements = response.body ?? [];
    this.isLoading = false;
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const pageToLoad = this.page - 1;
    const queryObject = {
      page: pageToLoad,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    return this.abonnementService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
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
    this.page = event.first / event.rows + 1;
    this.itemsPerPage = event.rows;
    this.load();
  }
}
