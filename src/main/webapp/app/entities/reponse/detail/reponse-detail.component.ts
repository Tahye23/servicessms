import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IReponse } from '../reponse.model';

@Component({
  standalone: true,
  selector: 'jhi-reponse-detail',
  templateUrl: './reponse-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class ReponseDetailComponent {
  reponse = input<IReponse | null>(null);

  previousState(): void {
    window.history.back();
  }
}
