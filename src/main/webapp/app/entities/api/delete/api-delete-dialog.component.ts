import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IApi } from '../api.model';
import { ApiService } from '../service/api.service';

@Component({
  standalone: true,
  templateUrl: './api-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ApiDeleteDialogComponent {
  api?: IApi;

  protected apiService = inject(ApiService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.apiService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
