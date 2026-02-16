import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IAbonnement } from '../abonnement.model';
import { AbonnementService } from '../service/abonnement.service';

@Component({
  standalone: true,
  templateUrl: './abonnement-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class AbonnementDeleteDialogComponent {
  abonnement?: IAbonnement;

  protected abonnementService = inject(AbonnementService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.abonnementService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
