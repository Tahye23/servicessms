import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IUserService } from '../user-service.model';
import { UserServiceService } from '../service/user-service.service';

@Component({
  standalone: true,
  templateUrl: './user-service-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class UserServiceDeleteDialogComponent {
  userService?: IUserService;

  protected userServiceService = inject(UserServiceService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.userServiceService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
