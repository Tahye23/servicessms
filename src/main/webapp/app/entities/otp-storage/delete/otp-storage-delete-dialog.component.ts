import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IOTPStorage } from '../otp-storage.model';
import { OTPStorageService } from '../service/otp-storage.service';

@Component({
  standalone: true,
  templateUrl: './otp-storage-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class OTPStorageDeleteDialogComponent {
  oTPStorage?: IOTPStorage;

  protected oTPStorageService = inject(OTPStorageService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.oTPStorageService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
