import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IUserTokenApi } from '../user-token-api.model';
import { ToastComponent } from '../../toast/toast.component';

@Component({
  standalone: true,
  selector: 'jhi-user-token-api-detail',
  templateUrl: './user-token-api-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe, ToastComponent],
})
export class UserTokenApiDetailComponent {
  userTokenApi = input<IUserTokenApi | null>(null);

  previousState(): void {
    window.history.back();
  }
}
