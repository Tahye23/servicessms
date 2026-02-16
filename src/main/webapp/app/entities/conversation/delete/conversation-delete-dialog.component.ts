import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import SharedModule from 'app/shared/shared.module';
import { ITEM_DELETED_EVENT } from 'app/config/navigation.constants';
import { IConversation } from '../conversation.model';
import { ConversationService } from '../service/conversation.service';

@Component({
  standalone: true,
  templateUrl: './conversation-delete-dialog.component.html',
  imports: [SharedModule, FormsModule],
})
export class ConversationDeleteDialogComponent {
  conversation?: IConversation;

  protected conversationService = inject(ConversationService);
  protected activeModal = inject(NgbActiveModal);

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.conversationService.delete(id).subscribe(() => {
      this.activeModal.close(ITEM_DELETED_EVENT);
    });
  }
}
