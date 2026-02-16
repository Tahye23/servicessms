import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { ICompany } from '../company.model';
import { CompanyService } from '../service/company.service';

@Component({
  standalone: true,
  templateUrl: './company-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class CompanyDeleteDialogComponent {
  company?: ICompany;

  protected companyService = inject(CompanyService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.companyService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
