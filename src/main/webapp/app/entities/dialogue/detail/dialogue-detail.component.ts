import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IDialogue } from '../dialogue.model';

@Component({
  standalone: true,
  selector: 'jhi-dialogue-detail',
  templateUrl: './dialogue-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class DialogueDetailComponent {
  dialogue = input<IDialogue | null>(null);

  previousState(): void {
    window.history.back();
  }
}
