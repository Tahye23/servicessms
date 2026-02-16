import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IReferentiel } from '../referentiel.model';

@Component({
  standalone: true,
  selector: 'jhi-referentiel-detail',
  templateUrl: './referentiel-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class ReferentielDetailComponent {
  referentiel = input<IReferentiel | null>(null);

  previousState(): void {
    window.history.back();
  }
}
