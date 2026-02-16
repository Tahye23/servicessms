import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ITEMS_PER_PAGE } from 'app/config/pagination.constants';
import { ISendSms } from 'app/entities/send-sms/send-sms.model';
import { EntityArrayResponseType } from 'app/entities/send-sms/service/send-sms.service';
import { sortStateSignal } from 'app/shared/sort';
import { Subscription } from 'rxjs';
import { WhatsappService } from '../service/whatsapp.service';
import { SendSmsComponent } from '../../send-sms/list/send-sms.component';

@Component({
  selector: 'jhi-send-whatsapp',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SendSmsComponent],
  templateUrl: './send-whatsapp.component.html',
  styleUrl: './send-whatsapp.component.scss',
})
export class SendWhatsappComponent implements OnInit {
  subscription: Subscription | null = null;
  sendSms?: ISendSms[];
  isLoading = false;
  uploadedFiles: any[] = [];
  showUpdateDialog = false;
  sortState = sortStateSignal({});
  selectedColumns: string[] = [];
  itemsPerPage = ITEMS_PER_PAGE;
  totalItems = 0;
  page = 1;
  whatsappList: any[] = [];
  predicate = 'id';
  ascending = true;
  size = 10;

  first: number = 0;
  rows: number = 10;
  myfiles: any = [];
  errorMessage?: string;
  successMessage?: string;
  totalPages = 0;

  // ____________________________________________________________
  constructor(private whatsappService: WhatsappService) {}

  ngOnInit(): void {
    this.getAllSendWhats();
  }
  // ____________________________________________________________
  // load(): void {
  //     this.queryBackend().subscribe({
  //       next: (res: EntityArrayResponseType) => {
  //         this.onResponseSuccess(res);
  //       },
  //     });
  //   }
  // Navigation simple :
  prevPage(): void {
    if (this.page > 1) {
      this.page--;
      this.getAllSendWhats();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages) {
      this.page++;
      this.getAllSendWhats();
    }
  }

  // Méthodes manquantes pour les boutons d'actions :
  // view(sms: any): void {
  //   this.router.navigate(['/send-sms', sms.id, 'view']);
  // }

  // edit(sms: any): void {
  //   this.router.navigate(['/send-sms', sms.id, 'edit']);
  // }

  // navigateToWithComponentValues(): void {
  //   this.handleNavigation(this.page, this.predicate, this.ascending);
  // }

  // // navigateToPage(page: number): void {
  // //   this.handleNavigation(page, this.sortState());
  // // }

  // navigateToPage(page = this.page): void {
  //   this.handleNavigation(page, this.predicate, this.ascending);
  // }
  // // onPageChange2(event: any): void {
  //   this.rows = event.rows;
  //   this.itemsPerPage = event.rows;
  //   console.log(event);
  //   this.handleNavigation(event.page + 1, this.predicate, this.ascending);
  //   //this.first = event.first;
  //   //
  // }

  // onPageChange2(event: any): void {
  //   this.rows = event.rows;
  //   this.itemsPerPage = event.rows;
  //   console.log(event);
  //   this.handleNavigation(event.page + 1, this.predicate, this.ascending);
  //   //this.first = event.first;
  //   //
  // }

  onPageChange(event: any): void {
    this.page = event.first / event.rows + 1; // PrimeNG paginates with 0-based index
    this.itemsPerPage = event.rows;
    this.getAllSendWhats();
  }

  // onUpload(event: any): void {
  //   console.log('going to upload' + event.files);
  //   this.uploadedFiles = event.files;
  //   for (const file of event.files) {
  //     this.fileextraitService.pushFiletostorage('idop', file).subscribe({
  //       next: (res: any) => {
  //         console.log(res.body.message);
  //         //this.messageService1.add({ severity: 'info', summary: 'Confirmé', detail: res.body.message });
  //         this.load();
  //         this.myfiles = [];
  //         this.uploadedFiles.push(file);
  //       },
  //       //error: (err: any) => this.messageService1.add({ severity: 'error', summary: 'Erreur', detail: "Erreur lors de l'upload" }),
  //       //
  //     });
  //   }
  // }

  // protected fillComponentAttributeFromRoute(params: ParamMap, data: Data): void {
  //   const page = params.get(PAGE_HEADER);
  //   this.page = +(page ?? 1);
  //   this.sortState.set(this.sortService.parseSortParam(params.get(SORT) ?? data[DEFAULT_SORT_DATA]));
  // }

  // protected onResponseSuccess(response: EntityArrayResponseType): void {
  //   this.fillComponentAttributesFromResponseHeader(response.headers);
  //   const dataFromBody = this.fillComponentAttributesFromResponseBody(response.body);
  //   this.sendSms = dataFromBody;
  // }

  protected fillComponentAttributesFromResponseBody(data: ISendSms[] | null): ISendSms[] {
    return data ?? [];
  }

  // protected fillComponentAttributesFromResponseHeader(headers: HttpHeaders): void {
  //   this.totalItems = Number(headers.get(TOTAL_COUNT_RESPONSE_HEADER));
  // }

  // protected queryBackend(): Observable<EntityArrayResponseType> {
  //   const { page } = this;

  //   this.isLoading = true;
  //   const pageToLoad: number = page;
  //   const queryObject: any = {
  //     page: pageToLoad - 1,
  //     size: this.itemsPerPage,
  //     sort: this.sortService.buildSortParam(this.sortState()),
  //   };
  //   return this.sendSmsService.query(queryObject).pipe(tap(() => (this.isLoading = false)));
  // }

  // protected handleNavigation(page: number, sortState: SortState): void {
  //   const queryParamsObj = {
  //     page,
  //     size: this.itemsPerPage,
  //     sort: this.sortService.buildSortParam(sortState),
  //   };

  //   this.ngZone.run(() => {
  //     this.router.navigate(['./'], {
  //       relativeTo: this.activatedRoute,
  //       queryParams: queryParamsObj,
  //     });
  //   });
  // }

  // protected handleNavigation(page = this.page, predicate?: string, ascending?: boolean): void {
  //   const queryParamsObj = {
  //     page,
  //     size: this.itemsPerPage,
  //     sort: this.getSortQueryParam(predicate, ascending),
  //   };
  //   this.router.navigate(['./'], {
  //     relativeTo: this.activatedRoute,
  //     queryParams: queryParamsObj,
  //   });
  // }

  // protected getSortQueryParam(predicate = this.predicate, ascending = this.ascending): string[] {
  //   const ascendingQueryParam = ascending ? ASC : DESC;
  //   if (predicate === '') {
  //     return [];
  //   } else {
  //     return [predicate + ',' + ascendingQueryParam];
  //   }
  // }

  // ______________________________________________________________________________________

  protected getAllSendWhats(): void {
    this.isLoading = true;
    this.whatsappService.getAllSendWhatsapp(this.page - 1, this.size).subscribe({
      next: data => {
        this.whatsappList = data.content.reverse(); // contenu paginé
        this.totalItems = data.totalElements;
        this.totalPages = data.totalPages;
        this.isLoading = false;
      },
      error: err => {
        console.error('Erreur lors du chargement des messages WhatsApp :', err);
        this.isLoading = false;
      },
    });
  }
}
