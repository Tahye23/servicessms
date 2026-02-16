import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { ITokensApp } from '../tokens-app.model';

@Component({
  standalone: true,
  selector: 'jhi-tokens-app-detail',
  templateUrl: './tokens-app-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class TokensAppDetailComponent {
  tokensApp = input<ITokensApp | null>(null);

  previousState(): void {
    window.history.back();
  }
}
