import { Component, inject, input, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { RouterModule } from '@angular/router';
import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IContact } from '../contact.model';
import { IGroupe } from '../../groupe/groupe.model';
import { GroupeService } from '../../groupe/service/groupe.service';
import { HttpResponse, HttpParams } from '@angular/common/http';
import { ContactService } from '../service/contact.service';
import { ContactDeleteDialogComponent } from '../delete/contact-delete-dialog.component';
import { filter, tap } from 'rxjs';
import { ITEM_DELETED_EVENT } from '../../../config/navigation.constants';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ToastComponent } from '../../toast/toast.component';
import { AccountService } from '../../../core/auth/account.service';

@Component({
  standalone: true,
  selector: 'jhi-contact-detail',
  templateUrl: './contact-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe, ToastComponent],
})
export class ContactDetailComponent implements OnInit {
  contact = input<IContact | null>(null);

  @Input() contactInput?: IContact | null;
  @Input() isModal: boolean = false;

  groupes?: IGroupe[];
  customFields: Record<string, string> = {};
  protected contactService = inject(ContactService);
  groupPage: number = 1;
  groupPageSize: number = 5;
  totalGroupItems: number = 0;
  totalGroupPages: number = 1;
  searchGroup: string = '';
  protected modalService = inject(NgbModal);
  contactCustomFieldsValues: Record<string, string> = {};
  error = false;
  message = '';
  @Output() closeModalEvent = new EventEmitter<void>();

  constructor(
    protected groupeService: GroupeService,
    private accountService: AccountService,
  ) {}

  ngOnInit(): void {
    this.accountService.identity(true).subscribe();
    const currentContact = this.getCurrentContact();

    if (currentContact && currentContact.id) {
      this.loadContactGroups(currentContact.id);

      if (currentContact.customFields) {
        try {
          this.contactCustomFieldsValues = JSON.parse(currentContact.customFields);
        } catch (e) {
          this.error = true;
          this.message = 'Erreur lors du parsing des customFields du contact';
          console.error('Erreur lors du parsing des customFields du contact', e);
        }
      }
    } else {
      this.error = true;
      this.message = 'Erreur lors de récupération du contact';
    }
  }

  getCurrentContact(): IContact | null {
    return this.contactInput || this.contact();
  }

  getCustomFieldsKeys(): string[] {
    return Object.keys(this.contactCustomFieldsValues);
  }

  delete(contact: IContact): void {
    const modalRef = this.modalService.open(ContactDeleteDialogComponent, { size: 'lg', backdrop: 'static' });
    modalRef.componentInstance.contact = contact;
    modalRef.closed
      .pipe(
        filter(reason => reason === ITEM_DELETED_EVENT),
        tap(() => this.load()),
      )
      .subscribe();
  }

  load() {
    const currentContact = this.getCurrentContact();
    if (currentContact?.id) {
      this.loadContactGroups(currentContact.id);
    }
  }

  loadContactGroups(contactId: number): void {
    const paramsObj = {
      page: (this.groupPage - 1).toString(),
      size: this.groupPageSize.toString(),
      search: this.searchGroup,
    };
    const params = new HttpParams({ fromObject: paramsObj });

    this.groupeService.findGroupsByContactId(contactId, params).subscribe({
      next: (res: HttpResponse<IGroupe[]>) => {
        this.groupes = res.body ?? [];
        const totalItems = res.headers.get('X-Total-Count');
        this.totalGroupItems = totalItems ? +totalItems : 0;
        this.totalGroupPages = Math.ceil(this.totalGroupItems / this.groupPageSize);
      },
      error: err => {
        this.error = true;
        this.message = 'Erreur lors de la récupération des groupes';
        console.error('Erreur lors de la récupération des groupes', err);
      },
    });
  }

  // ... reste du code existant ...

  closeModal(): void {
    if (this.isModal) {
      this.closeModalEvent.emit(); // ✅ Émet l'événement
    } else {
      // Mode page normale
      window.history.back();
    }
  }

  previousState(): void {
    if (this.isModal) {
      this.closeModal();
    } else {
      window.history.back();
    }
  }

  onGroupSearch(): void {
    this.groupPage = 1;
    const currentContact = this.getCurrentContact();
    if (currentContact?.id) {
      this.loadContactGroups(currentContact.id);
    }
  }

  changeGroupPage(page: number): void {
    if (page < 1 || page > this.totalGroupPages) return;
    this.groupPage = page;
    const currentContact = this.getCurrentContact();
    if (currentContact?.id) {
      this.loadContactGroups(currentContact.id);
    }
  }
}
