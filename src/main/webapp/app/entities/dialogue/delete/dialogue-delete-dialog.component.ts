import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IDialogue } from '../dialogue.model';
import { DialogueService } from '../service/dialogue.service';

@Component({
  standalone: true,
  templateUrl: './dialogue-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class DialogueDeleteDialogComponent {
  dialogue?: IDialogue;

  protected dialogueService = inject(DialogueService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.dialogueService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
