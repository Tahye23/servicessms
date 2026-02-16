import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IQuestion } from '../question.model';

@Component({
  standalone: true,
  selector: 'jhi-question-detail',
  templateUrl: './question-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class QuestionDetailComponent {
  question = input<IQuestion | null>(null);

  previousState(): void {
    window.history.back();
  }
}
