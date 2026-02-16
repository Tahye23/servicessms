import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IUserService, NewUserService } from '../user-service.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IUserService for edit and NewUserServiceFormGroupInput for create.
 */
type UserServiceFormGroupInput = IUserService | PartialWithRequiredKeyOf<NewUserService>;

type UserServiceFormDefaults = Pick<NewUserService, 'id'>;

type UserServiceFormGroupContent = {
  id: FormControl<IUserService['id'] | NewUserService['id']>;
  urSService: FormControl<IUserService['urSService']>;
  urSUser: FormControl<IUserService['urSUser']>;
  service: FormControl<IUserService['service']>;
  user: FormControl<IUserService['user']>;
};

export type UserServiceFormGroup = FormGroup<UserServiceFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class UserServiceFormService {
  createUserServiceFormGroup(userService: UserServiceFormGroupInput = { id: null }): UserServiceFormGroup {
    const userServiceRawValue = {
      ...this.getFormDefaults(),
      ...userService,
    };
    return new FormGroup<UserServiceFormGroupContent>({
      id: new FormControl(
        { value: userServiceRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      urSService: new FormControl(userServiceRawValue.urSService),
      urSUser: new FormControl(userServiceRawValue.urSUser),
      service: new FormControl(userServiceRawValue.service),
      user: new FormControl(userServiceRawValue.user),
    });
  }

  getUserService(form: UserServiceFormGroup): IUserService | NewUserService {
    return form.getRawValue() as IUserService | NewUserService;
  }

  resetForm(form: UserServiceFormGroup, userService: UserServiceFormGroupInput): void {
    const userServiceRawValue = { ...this.getFormDefaults(), ...userService };
    form.reset(
      {
        ...userServiceRawValue,
        id: { value: userServiceRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): UserServiceFormDefaults {
    return {
      id: null,
    };
  }
}
