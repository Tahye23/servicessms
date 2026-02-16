import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IAbonnement } from '../abonnement.model';

@Component({
  standalone: true,
  selector: 'jhi-abonnement-detail',
  templateUrl: './abonnement-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class AbonnementDetailComponent {
  @Input() abonnement: IAbonnement | null = null;

  previousState(): void {
    window.history.back();
  }
}
