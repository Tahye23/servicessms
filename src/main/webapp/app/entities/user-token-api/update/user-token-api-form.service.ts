import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IUserTokenApi, NewUserTokenApi } from '../user-token-api.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IUserTokenApi for edit and NewUserTokenApiFormGroupInput for create.
 */
type UserTokenApiFormGroupInput = IUserTokenApi | PartialWithRequiredKeyOf<NewUserTokenApi>;

type UserTokenApiFormDefaults = Pick<NewUserTokenApi, 'id'>;

type UserTokenApiFormGroupContent = {
  id: FormControl<IUserTokenApi['id'] | NewUserTokenApi['id']>;
  api: FormControl<IUserTokenApi['api']>;
  token: FormControl<IUserTokenApi['token']>;
  user: FormControl<IUserTokenApi['user']>;
};

export type UserTokenApiFormGroup = FormGroup<UserTokenApiFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class UserTokenApiFormService {
  createUserTokenApiFormGroup(userTokenApi: UserTokenApiFormGroupInput = { id: null }): UserTokenApiFormGroup {
    const userTokenApiRawValue = {
      ...this.getFormDefaults(),
      ...userTokenApi,
    };
    return new FormGroup<UserTokenApiFormGroupContent>({
      id: new FormControl(
        { value: userTokenApiRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      api: new FormControl(userTokenApiRawValue.api),
      token: new FormControl(userTokenApiRawValue.token),
      user: new FormControl(userTokenApiRawValue.user),
    });
  }

  getUserTokenApi(form: UserTokenApiFormGroup): IUserTokenApi | NewUserTokenApi {
    return form.getRawValue() as IUserTokenApi | NewUserTokenApi;
  }

  resetForm(form: UserTokenApiFormGroup, userTokenApi: UserTokenApiFormGroupInput): void {
    const userTokenApiRawValue = { ...this.getFormDefaults(), ...userTokenApi };
    form.reset(
      {
        ...userTokenApiRawValue,
        id: { value: userTokenApiRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): UserTokenApiFormDefaults {
    return {
      id: null,
    };
  }
}
