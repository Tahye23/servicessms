import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IReferentiel, NewReferentiel } from '../referentiel.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IReferentiel for edit and NewReferentielFormGroupInput for create.
 */
type ReferentielFormGroupInput = IReferentiel | PartialWithRequiredKeyOf<NewReferentiel>;

type ReferentielFormDefaults = Pick<NewReferentiel, 'id'>;

type ReferentielFormGroupContent = {
  id: FormControl<IReferentiel['id'] | NewReferentiel['id']>;
  refCode: FormControl<IReferentiel['refCode']>;
  refRadical: FormControl<IReferentiel['refRadical']>;
  refFrTitle: FormControl<IReferentiel['refFrTitle']>;
  refArTitle: FormControl<IReferentiel['refArTitle']>;
  refEnTitle: FormControl<IReferentiel['refEnTitle']>;
};

export type ReferentielFormGroup = FormGroup<ReferentielFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ReferentielFormService {
  createReferentielFormGroup(referentiel: ReferentielFormGroupInput = { id: null }): ReferentielFormGroup {
    const referentielRawValue = {
      ...this.getFormDefaults(),
      ...referentiel,
    };
    return new FormGroup<ReferentielFormGroupContent>({
      id: new FormControl(
        { value: referentielRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      refCode: new FormControl(referentielRawValue.refCode, {
        validators: [Validators.required],
      }),
      refRadical: new FormControl(referentielRawValue.refRadical, {
        validators: [Validators.required],
      }),
      refFrTitle: new FormControl(referentielRawValue.refFrTitle),
      refArTitle: new FormControl(referentielRawValue.refArTitle),
      refEnTitle: new FormControl(referentielRawValue.refEnTitle),
    });
  }

  getReferentiel(form: ReferentielFormGroup): IReferentiel | NewReferentiel {
    return form.getRawValue() as IReferentiel | NewReferentiel;
  }

  resetForm(form: ReferentielFormGroup, referentiel: ReferentielFormGroupInput): void {
    const referentielRawValue = { ...this.getFormDefaults(), ...referentiel };
    form.reset(
      {
        ...referentielRawValue,
        id: { value: referentielRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): ReferentielFormDefaults {
    return {
      id: null,
    };
  }
}
