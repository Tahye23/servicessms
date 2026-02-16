import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IUserTokenApi } from '../user-token-api.model';
import { UserTokenApiService } from '../service/user-token-api.service';

@Component({
  standalone: true,
  templateUrl: './user-token-api-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class UserTokenApiDeleteDialogComponent {
  userTokenApi?: IUserTokenApi;

  protected userTokenApiService = inject(UserTokenApiService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.userTokenApiService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
