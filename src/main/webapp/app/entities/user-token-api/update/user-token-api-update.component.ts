import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IApi } from 'app/entities/api/api.model';
import { ApiService } from 'app/entities/api/service/api.service';
import { ITokensApp } from 'app/entities/tokens-app/tokens-app.model';
import { TokensAppService } from 'app/entities/tokens-app/service/tokens-app.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { UserTokenApiService } from '../service/user-token-api.service';
import { IUserTokenApi } from '../user-token-api.model';
import { UserTokenApiFormService, UserTokenApiFormGroup } from './user-token-api-form.service';

@Component({
  standalone: true,
  selector: 'jhi-user-token-api-update',
  templateUrl: './user-token-api-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class UserTokenApiUpdateComponent implements OnInit {
  isSaving = false;
  userTokenApi: IUserTokenApi | null = null;

  apisSharedCollection: IApi[] = [];
  tokensAppsSharedCollection: ITokensApp[] = [];
  extendedUsersSharedCollection: IExtendedUser[] = [];

  protected userTokenApiService = inject(UserTokenApiService);
  protected userTokenApiFormService = inject(UserTokenApiFormService);
  protected apiService = inject(ApiService);
  protected tokensAppService = inject(TokensAppService);
  protected extendedUserService = inject(ExtendedUserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: UserTokenApiFormGroup = this.userTokenApiFormService.createUserTokenApiFormGroup();

  compareApi = (o1: IApi | null, o2: IApi | null): boolean => this.apiService.compareApi(o1, o2);

  compareTokensApp = (o1: ITokensApp | null, o2: ITokensApp | null): boolean => this.tokensAppService.compareTokensApp(o1, o2);

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ userTokenApi }) => {
      this.userTokenApi = userTokenApi;
      if (userTokenApi) {
        this.updateForm(userTokenApi);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const userTokenApi = this.userTokenApiFormService.getUserTokenApi(this.editForm);
    if (userTokenApi.id !== null) {
      this.subscribeToSaveResponse(this.userTokenApiService.update(userTokenApi));
    } else {
      this.subscribeToSaveResponse(this.userTokenApiService.create(userTokenApi));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IUserTokenApi>>): void {
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

  protected updateForm(userTokenApi: IUserTokenApi): void {
    this.userTokenApi = userTokenApi;
    this.userTokenApiFormService.resetForm(this.editForm, userTokenApi);

    this.apisSharedCollection = this.apiService.addApiToCollectionIfMissing<IApi>(this.apisSharedCollection, userTokenApi.api);
    this.tokensAppsSharedCollection = this.tokensAppService.addTokensAppToCollectionIfMissing<ITokensApp>(
      this.tokensAppsSharedCollection,
      userTokenApi.token,
    );
    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      userTokenApi.user,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.apiService
      .query()
      .pipe(map((res: HttpResponse<IApi[]>) => res.body ?? []))
      .pipe(map((apis: IApi[]) => this.apiService.addApiToCollectionIfMissing<IApi>(apis, this.userTokenApi?.api)))
      .subscribe((apis: IApi[]) => (this.apisSharedCollection = apis));

    this.tokensAppService
      .query()
      .pipe(map((res: HttpResponse<ITokensApp[]>) => res.body ?? []))
      .pipe(
        map((tokensApps: ITokensApp[]) =>
          this.tokensAppService.addTokensAppToCollectionIfMissing<ITokensApp>(tokensApps, this.userTokenApi?.token),
        ),
      )
      .subscribe((tokensApps: ITokensApp[]) => (this.tokensAppsSharedCollection = tokensApps));

    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.userTokenApi?.user),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => (this.extendedUsersSharedCollection = extendedUsers));
  }
}
