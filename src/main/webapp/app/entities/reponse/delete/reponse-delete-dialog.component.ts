import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IReponse } from '../reponse.model';
import { ReponseService } from '../service/reponse.service';

@Component({
  standalone: true,
  templateUrl: './reponse-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ReponseDeleteDialogComponent {
  reponse?: IReponse;

  protected reponseService = inject(ReponseService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.reponseService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
