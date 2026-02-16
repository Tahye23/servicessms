import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IService } from 'app/entities/service/service.model';
import { ServiceService } from 'app/entities/service/service/service.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { UserServiceService } from '../service/user-service.service';
import { IUserService } from '../user-service.model';
import { UserServiceFormService, UserServiceFormGroup } from './user-service-form.service';

@Component({
  standalone: true,
  selector: 'jhi-user-service-update',
  templateUrl: './user-service-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class UserServiceUpdateComponent implements OnInit {
  isSaving = false;
  userService: IUserService | null = null;

  servicesSharedCollection: IService[] = [];
  extendedUsersSharedCollection: IExtendedUser[] = [];

  protected userServiceService = inject(UserServiceService);
  protected userServiceFormService = inject(UserServiceFormService);
  protected serviceService = inject(ServiceService);
  protected extendedUserService = inject(ExtendedUserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: UserServiceFormGroup = this.userServiceFormService.createUserServiceFormGroup();

  compareService = (o1: IService | null, o2: IService | null): boolean => this.serviceService.compareService(o1, o2);

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ userService }) => {
      this.userService = userService;
      if (userService) {
        this.updateForm(userService);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const userService = this.userServiceFormService.getUserService(this.editForm);
    if (userService.id !== null) {
      this.subscribeToSaveResponse(this.userServiceService.update(userService));
    } else {
      this.subscribeToSaveResponse(this.userServiceService.create(userService));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IUserService>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(userService: IUserService): void {
    this.userService = userService;
    this.userServiceFormService.resetForm(this.editForm, userService);

    this.servicesSharedCollection = this.serviceService.addServiceToCollectionIfMissing<IService>(
      this.servicesSharedCollection,
      userService.service,
    );
    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      userService.user,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.serviceService
      .query()
      .pipe(map((res: HttpResponse<IService[]>) => res.body ?? []))
      .pipe(
        map((services: IService[]) => this.serviceService.addServiceToCollectionIfMissing<IService>(services, this.userService?.service)),
      )
      .subscribe((services: IService[]) => (this.servicesSharedCollection = services));

    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.userService?.user),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));
  }
}
