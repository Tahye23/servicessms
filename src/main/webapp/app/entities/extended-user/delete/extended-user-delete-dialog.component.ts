import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IExtendedUser } from '../extended-user.model';
import { ExtendedUserService } from '../service/extended-user.service';

@Component({
  standalone: true,
  templateUrl: './extended-user-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ExtendedUserDeleteDialogComponent {
  extendedUser?: IExtendedUser;

  protected extendedUserService = inject(ExtendedUserService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.extendedUserService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
