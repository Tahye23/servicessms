import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IParticipant } from '../participant.model';
import { ParticipantService } from '../service/participant.service';

@Component({
  standalone: true,
  templateUrl: './participant-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ParticipantDeleteDialogComponent {
  participant?: IParticipant;

  protected participantService = inject(ParticipantService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.participantService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
