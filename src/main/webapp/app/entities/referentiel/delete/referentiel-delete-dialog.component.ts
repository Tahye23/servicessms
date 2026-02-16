import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IReferentiel } from '../referentiel.model';
import { ReferentielService } from '../service/referentiel.service';

@Component({
  standalone: true,
  templateUrl: './referentiel-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ReferentielDeleteDialogComponent {
  referentiel?: IReferentiel;

  protected referentielService = inject(ReferentielService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.referentielService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
