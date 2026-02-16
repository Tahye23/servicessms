import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IOTPStorage } from '../otp-storage.model';
import { OTPStorageService } from '../service/otp-storage.service';
import { OTPStorageFormService, OTPStorageFormGroup } from './otp-storage-form.service';

@Component({
  standalone: true,
  selector: 'jhi-otp-storage-update',
  templateUrl: './otp-storage-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class OTPStorageUpdateComponent implements OnInit {
  isSaving = false;
  oTPStorage: IOTPStorage | null = null;

  extendedUsersSharedCollection: IExtendedUser[] = [];

  protected oTPStorageService = inject(OTPStorageService);
  protected oTPStorageFormService = inject(OTPStorageFormService);
  protected extendedUserService = inject(ExtendedUserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: OTPStorageFormGroup = this.oTPStorageFormService.createOTPStorageFormGroup();

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ oTPStorage }) => {
      this.oTPStorage = oTPStorage;
      if (oTPStorage) {
        this.updateForm(oTPStorage);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const oTPStorage = this.oTPStorageFormService.getOTPStorage(this.editForm);
    if (oTPStorage.id !== null) {
      this.subscribeToSaveResponse(this.oTPStorageService.update(oTPStorage));
    } else {
      this.subscribeToSaveResponse(this.oTPStorageService.create(oTPStorage));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IOTPStorage>>): void {
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

  protected updateForm(oTPStorage: IOTPStorage): void {
    this.oTPStorage = oTPStorage;
    this.oTPStorageFormService.resetForm(this.editForm, oTPStorage);

    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      oTPStorage.user,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.oTPStorage?.user),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));
  }
}
