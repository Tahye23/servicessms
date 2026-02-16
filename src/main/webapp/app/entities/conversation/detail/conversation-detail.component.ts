import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IConversation } from '../conversation.model';

@Component({
  standalone: true,
  selector: 'jhi-conversation-detail',
  templateUrl: './conversation-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class ConversationDetailComponent {
  conversation = input<IConversation | null>(null);

  previousState(): void {
    window.history.back();
  }
}
