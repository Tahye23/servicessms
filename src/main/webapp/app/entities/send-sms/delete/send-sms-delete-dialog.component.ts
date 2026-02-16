import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { ISendSms } from '../send-sms.model';
import { SendSmsService } from '../service/send-sms.service';

@Component({
  standalone: true,
  templateUrl: './send-sms-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class SendSmsDeleteDialogComponent {
  sendSms?: ISendSms;

  protected sendSmsService = inject(SendSmsService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.sendSmsService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
