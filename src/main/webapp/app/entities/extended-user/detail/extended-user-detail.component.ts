import { Component, Input } from '@angular/core';
import { IExtendedUser } from '../extended-user.model';
import { DatePipe, NgIf, SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AlertErrorComponent } from '../../../shared/alert/alert-error.component';
import { AlertComponent } from '../../../shared/alert/alert.component';

@Component({
  standalone: true,
  selector: 'jhi-extended-user-detail',
  templateUrl: './extended-user-detail.component.html',
  imports: [
    DatePipe,
    RouterLink,
    NgIf,
    AlertErrorComponent,
    AlertComponent,
    SlicePipe,
    /* tes imports ici */
  ],
})
export class ExtendedUserDetailComponent {
  @Input() extendedUser: IExtendedUser | null = null;

  previousState(): void {
    window.history.back();
  }

  protected readonly Math = Math;
}
