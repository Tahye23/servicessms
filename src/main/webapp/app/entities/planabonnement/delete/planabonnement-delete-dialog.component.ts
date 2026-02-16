import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IPlanabonnement } from '../planabonnement.model';
import { PlanabonnementService } from '../service/planabonnement.service';

@Component({
  standalone: true,
  templateUrl: './planabonnement-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class PlanabonnementDeleteDialogComponent {
  planabonnement?: IPlanabonnement;

  protected planabonnementService = inject(PlanabonnementService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.planabonnementService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
