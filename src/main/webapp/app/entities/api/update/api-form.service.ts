import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IApi, NewApi } from '../api.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IApi for edit and NewApiFormGroupInput for create.
 */
type ApiFormGroupInput = IApi | PartialWithRequiredKeyOf<NewApi>;

type ApiFormDefaults = Pick<NewApi, 'id'>;

type ApiFormGroupContent = {
  id: FormControl<IApi['id'] | NewApi['id']>;
  apiNom: FormControl<IApi['apiNom']>;
  apiUrl: FormControl<IApi['apiUrl']>;
  apiVersion: FormControl<IApi['apiVersion']>;
};

export type ApiFormGroup = FormGroup<ApiFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ApiFormService {
  createApiFormGroup(api: ApiFormGroupInput = { id: null }): ApiFormGroup {
    const apiRawValue = {
      ...this.getFormDefaults(),
      ...api,
    };
    return new FormGroup<ApiFormGroupContent>({
      id: new FormControl(
        { value: apiRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      apiNom: new FormControl(apiRawValue.apiNom),
      apiUrl: new FormControl(apiRawValue.apiUrl),
      apiVersion: new FormControl(apiRawValue.apiVersion),
    });
  }

  getApi(form: ApiFormGroup): IApi | NewApi {
    return form.getRawValue() as IApi | NewApi;
  }

  resetForm(form: ApiFormGroup, api: ApiFormGroupInput): void {
    const apiRawValue = { ...this.getFormDefaults(), ...api };
    form.reset(
      {
        ...apiRawValue,
        id: { value: apiRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): ApiFormDefaults {
    return {
      id: null,
    };
  }
}
