import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IExtendedUser } from '../extended-user.model';
import { ExtendedUserService } from '../service/extended-user.service';
import { ExtendedUserFormService, ExtendedUserFormGroup } from './extended-user-form.service';

@Component({
  standalone: true,
  selector: 'jhi-extended-user-update',
  templateUrl: './extended-user-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class ExtendedUserUpdateComponent implements OnInit {
  isSaving = false;
  extendedUser: IExtendedUser | null = null;

  usersSharedCollection: IUser[] = [];

  protected extendedUserService = inject(ExtendedUserService);
  protected extendedUserFormService = inject(ExtendedUserFormService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  editForm: ExtendedUserFormGroup = this.extendedUserFormService.createExtendedUserFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ extendedUser }) => {
      this.extendedUser = extendedUser;
      if (extendedUser) {
        this.updateForm(extendedUser);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const extendedUser = this.extendedUserFormService.getExtendedUser(this.editForm);
    if (extendedUser.id !== null) {
      this.subscribeToSaveResponse(this.extendedUserService.update(extendedUser));
    } else {
      this.subscribeToSaveResponse(this.extendedUserService.create(extendedUser));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IExtendedUser>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Empty for inheritance
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(extendedUser: IExtendedUser): void {
    this.extendedUser = extendedUser;
    this.extendedUserFormService.resetForm(this.editForm, extendedUser);

    this.usersSharedCollection = this.userService.addUserToCollectionIfMissing<IUser>(this.usersSharedCollection, extendedUser.user);
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map(res => res.body ?? []))
      .pipe(map(users => this.userService.addUserToCollectionIfMissing<IUser>(users, this.extendedUser?.user)))
      .subscribe(users => (this.usersSharedCollection = users));
  }
}
