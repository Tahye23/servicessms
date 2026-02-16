import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IUserService } from '../user-service.model';

@Component({
  standalone: true,
  selector: 'jhi-user-service-detail',
  templateUrl: './user-service-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class UserServiceDetailComponent {
  userService = input<IUserService | null>(null);

  previousState(): void {
    window.history.back();
  }
}
