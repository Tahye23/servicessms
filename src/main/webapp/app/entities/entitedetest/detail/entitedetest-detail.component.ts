import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IEntitedetest } from '../entitedetest.model';

@Component({
  standalone: true,
  selector: 'jhi-entitedetest-detail',
  templateUrl: './entitedetest-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class EntitedetestDetailComponent {
  entitedetest = input<IEntitedetest | null>(null);

  previousState(): void {
    window.history.back();
  }
}
