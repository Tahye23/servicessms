import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IChoix } from '../choix.model';

@Component({
  standalone: true,
  selector: 'jhi-choix-detail',
  templateUrl: './choix-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class ChoixDetailComponent {
  choix = input<IChoix | null>(null);

  previousState(): void {
    window.history.back();
  }
}
