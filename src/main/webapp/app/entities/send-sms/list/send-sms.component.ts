import { Component, NgZone, inject, OnInit, Input } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Data, ParamMap, Router, RouterModule } from '@angular/router';
import { combineLatest, filter, Observable, Subscription, tap } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CardModule } from 'primeng/card';
import SharedModule from 'app/shared/shared.module';
import { sortStateSignal, SortDirective, SortByDirective, type SortState, SortService } from 'app/shared/sort';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { ItemCountComponent } from 'app/shared/pagination';
import { FormsModule } from '@angular/forms';
import { FileUploadModule } from 'primeng/fileupload';
import { ASC, DESC, SORT, ITEM_DELETED_EVENT, DEFAULT_SORT_DATA } from 'app/config/navigation.constants';
import { TableModule } from 'primeng/table';

import { ITEMS_PER_PAGE, PAGE_HEADER, TOTAL_COUNT_RESPONSE_HEADER } from 'app/config/pagination.constants';
import { ISendSms, MessageType } from '../send-sms.model';
import { EntityArrayResponseType, SendSmsService } from '../service/send-sms.service';
import { SendSmsDeleteDialogComponent } from '../delete/send-sms-delete-dialog.component';
import { FileextraitService } from 'app/entities/fileextrait/service/fileextrait.service';
import { DialogModule } from 'primeng/dialog';
import { SendSmsUpdateComponent } from '../update/send-sms-update.component';
import { MultiSelectModule } from 'primeng/multiselect';
import { PaginatorModule } from 'primeng/paginator';

interface PageEvent {
  first: number;
  rows: number;
  page: number;
  pageCount: number;
}

@Component({
  standalone: true,
  selector: 'jhi-send-sms',
  templateUrl: './send-sms.component.html',
  imports: [
    RouterModule,
    CardModule,
    DialogModule,
    FileUploadModule,
    FormsModule,
    SharedModule,
    SortDirective,
    SortByDirective,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    ItemCountComponent,
    SendSmsUpdateComponent,
    DialogModule,
    MultiSelectModule,
    PaginatorModule,
    TableModule,
  ],
})
export class SendSmsComponent implements OnInit {
  subscription: Subscription | null = null;
  sendSms?: ISendSms[];
  isLoading = false;
  sortState = sortStateSignal({});
  @Input() isWhatsapp: boolean = false;
  page = 1;
  itemsPerPage = 10;
  totalItems = 0;
  totalPages = 0;
  itemsPerPageOptions: number[] = [10, 20, 25, 30, 50];
  searchTerm: string = '';
  filterIsSent: boolean | null = null;
  filterIsBulk: boolean | null = null;
  filterReceiver: string = '';
  filterreceivers: string = '';

  predicate = 'id';
  ascending = true;

  first: number = 0;
  rows: number = 10;

  errorMessage?: string;
  successMessage?: string;

  public router = inject(Router);
  protected sendSmsService = inject(SendSmsService);
  protected fileextraitService = inject(FileextraitService);
  protected activatedRoute = inject(ActivatedRoute);
  protected sortService = inject(SortService);
  protected modalService = inject(NgbModal);
  protected ngZone = inject(NgZone);

  trackId = (_index: number, item: ISendSms): number => this.sendSmsService.getSendSmsIdentifier(item);

  ngOnInit(): void {
    this.load();
    this.subscription = combineLatest([this.activatedRoute.queryParamMap, this.activatedRoute.data])
      .pipe(
        tap(([params, data]) => this.fillComponentAttributeFromRoute(params, data)),
        tap(() => this.load()),
      )
      .subscribe();
    this.loadFiltersFromSession();
  }

  getSentCount(): number {
    if (!this.sendSms) return 0;
    return this.sendSms.filter(sms => sms.isSent === true).length;
  }

  getPendingCount(): number {
    if (!this.sendSms) return 0;
    return this.sendSms.filter(sms => sms.isSent === false || sms.isSent === null).length;
  }

  getBulkCount(): number {
    if (!this.sendSms) return 0;
    return this.sendSms.filter(sms => sms.isbulk === true).length;
  }

  getBulkProgressPercent(sms: any): number {
    const total = (sms.totalMessage || 0) > 0 ? sms.totalMessage : 1;
    const done = (sms.totalSent || 0) + (sms.totalDelivered || 0) + (sms.totalRead || 0);
    return parseFloat(((done / total) * 100).toFixed(1));
  }

  getBulkProgressSegment(sms: any, segment: 'sentDeliveredRead' | 'pending' | 'failed'): number {
    const total = (sms.totalMessage || 0) > 0 ? sms.totalMessage : 1;
    switch (segment) {
      case 'sentDeliveredRead':
        return (((sms.totalSent || 0) + (sms.totalDelivered || 0) + (sms.totalRead || 0)) / total) * 100;
      case 'pending':
        return ((sms.totalPending || 0) / total) * 100;
      case 'failed':
        return ((sms.totalFailed || 0) / total) * 100;
      default:
        return 0;
    }
  }

  getDeliveryLabel(deliveryStatus: any): string {
    if (!deliveryStatus) return 'En attente';
    const status = deliveryStatus.toLowerCase();
    switch (status) {
      case 'read':
        return 'Lu';
      case 'delivered':
        return 'Distribué';
      case 'sent':
        return 'Envoyé';
      case 'pending':
        return 'En attente';
      case 'failed':
        return 'Échec';
      default:
        return 'Inconnu';
    }
  }

  loadFiltersFromSession(): void {
    const storedReceiver = sessionStorage.getItem('filterReceiver');
    const storedReceivers = sessionStorage.getItem('filterreceivers');
    if (storedReceiver) this.filterReceiver = storedReceiver;
    if (storedReceivers) this.filterreceivers = storedReceivers;
  }

  changeItemsPerPage(): void {
    this.page = 1;
    this.load();
  }

  prevPage(): void {
    if (this.page > 1) {
      this.page--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages) {
      this.page++;
      this.load();
    }
  }

  goToPage(pageNumber: number): void {
    if (pageNumber >= 1 && pageNumber <= this.totalPages) {
      this.page = pageNumber;
      this.load();
    }
  }

  getDisplayedPages(): (number | string)[] {
    const totalPages = this.totalPages;
    const currentPage = this.page;
    const delta = 2;
    const range: (number | string)[] = [];
    const rangeWithDots: (number | string)[] = [];
    let l: number | undefined;

    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || (i >= currentPage - delta && i <= currentPage + delta)) {
        range.push(i);
      }
    }

    for (let i of range) {
      if (l !== undefined && typeof i === 'number') {
        if (i - l === 2) {
          rangeWithDots.push(l + 1);
        } else if (i - l > 2) {
          rangeWithDots.push('...');
        }
      }
      rangeWithDots.push(i);
      if (typeof i === 'number') {
        l = i;
      }
    }

    return rangeWithDots;
  }

  isNumber(value: any): value is number {
    return typeof value === 'number';
  }

  // Actions CRUD
  delete(sendSms: ISendSms): void {
    const modalRef = this.modalService.open(SendSmsDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.sendSms = sendSms;
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  view(sms: any): void {
    const route = this.isWhatsapp ? '/send-whatsapp' : '/send-sms';
    this.router.navigate([route, sms.id, 'view']);
  }

  load(): void {
    this.errorMessage = undefined;
    this.successMessage = undefined;

    this.queryBackend().subscribe({
      next: (res: EntityArrayResponseType) => {
        this.onResponseSuccess(res);
      },
      error: error => {
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement des données';
        console.error('Erreur de chargement:', error);
      },
    });
  }

  navigateToWithComponentValues(): void {
    this.handleNavigation(this.page, this.predicate, this.ascending);
  }

  navigateToPage(page = this.page): void {
    this.handleNavigation(page, this.predicate, this.ascending);
  }

  // Méthodes protégées (conservées de l'original)
  protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
    const page = params.get(PAGE_HEADER);
    this.page = +(page ?? 1);
    this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  }

  protected onResponseSuccess(response: EntityArrayResponseType): void {
    this.fillComponentAttributesFromResponseHeader(response.headers);
    const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
    this.sendSms = dataFromBody;
    this.isLoading = false;
  }

  protected fillComponentAttributesFromResponseBody(data: ISendSms[] | null): ISendSms[] {
    return data ?? [];
  }

  protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
    this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER));
    this.totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
  }

  protected queryBackend(): Observable<EntityArrayResponseType> {
    this.isLoading = true;
    const pageToLoad: number = this.page;

    const queryObject: any = {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      sort: this.sortService.buildSortParam(this.sortState()),
    };

    // Ajout des filtres selon l'HTML
    if (this.searchTerm) {
      queryObject['search'] = this.searchTerm;
    }
    if (this.filterIsSent !== null) {
      queryObject['isSent'] = this.filterIsSent;
    }
    if (this.filterIsBulk !== null) {
      queryObject['isBulk'] = this.filterIsBulk;
    }
    if (this.filterReceiver) {
      queryObject['receiver'] = this.filterReceiver;
    }
    if (this.filterreceivers) {
      queryObject['receivers'] = this.filterreceivers;
    }

    // Type de message selon isWhatsapp
    if (this.isWhatsapp) {
      queryObject['type'] = MessageType.WHATSAPP;
    } else {
      queryObject['type'] = MessageType.SMS;
    }

    console.log('queryObject', queryObject);
    return this.sendSmsService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
  }

  protected handleNavigation(page = this.page, predicate?: string, ascending?: boolean): void {
    const queryParamsObj = {
      page,
      size: this.itemsPerPage,
      sort: this.getSortQueryParam(predicate, ascending),
    };
    this.router.navigate(['./'], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsObj,
    });
  }

  protected getSortQueryParam(predicate = this.predicate, ascending = this.ascending): string[] {
    const ascendingQueryParam = ascending ? ASC : DESC;
    if (predicate === '') {
      return [];
    } else {
      return [predicate + ',' + ascendingQueryParam];
    }
  }
}
