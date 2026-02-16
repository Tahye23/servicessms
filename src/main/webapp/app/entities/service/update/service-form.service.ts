import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IService, NewService } from '../service.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IService for edit and NewServiceFormGroupInput for create.
 */
type ServiceFormGroupInput = IService | PartialWithRequiredKeyOf<NewService>;

type ServiceFormDefaults = Pick<NewService, 'id' | 'statut'>;

type ServiceFormGroupContent = {
  id: FormControl<IService['id'] | NewService['id']>;
  servNom: FormControl<IService['servNom']>;
  servDescription: FormControl<IService['servDescription']>;
  nbrusage: FormControl<IService['nbrusage']>;
  montant: FormControl<IService['montant']>;
  statut: FormControl<IService['statut']>;
  accessType: FormControl<IService['accessType']>;
};

export type ServiceFormGroup = FormGroup<ServiceFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ServiceFormService {
  createServiceFormGroup(service: ServiceFormGroupInput = { id: null }): ServiceFormGroup {
    const serviceRawValue = {
      ...this.getFormDefaults(),
      ...service,
    };
    return new FormGroup<ServiceFormGroupContent>({
      id: new FormControl(
        { value: serviceRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      servNom: new FormControl(serviceRawValue.servNom),
      servDescription: new FormControl(serviceRawValue.servDescription),
      nbrusage: new FormControl(serviceRawValue.nbrusage),
      montant: new FormControl(serviceRawValue.montant),
      statut: new FormControl(serviceRawValue.statut),
      accessType: new FormControl(serviceRawValue.accessType),
    });
  }

  getService(form: ServiceFormGroup): IService | NewService {
    return form.getRawValue() as IService | NewService;
  }

  resetForm(form: ServiceFormGroup, service: ServiceFormGroupInput): void {
    const serviceRawValue = { ...this.getFormDefaults(), ...service };
    form.reset(
      {
        ...serviceRawValue,
        id: { value: serviceRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): ServiceFormDefaults {
    return {
      id: null,
      statut: false,
    };
  }
}
