import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IChoix, NewChoix } from '../choix.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IChoix for edit and NewChoixFormGroupInput for create.
 */
type ChoixFormGroupInput = IChoix | PartialWithRequiredKeyOf<NewChoix>;

type ChoixFormDefaults = Pick<NewChoix, 'id'>;

type ChoixFormGroupContent = {
  id: FormControl<IChoix['id'] | NewChoix['id']>;
  chovaleur: FormControl<IChoix['chovaleur']>;
  choquestion: FormControl<IChoix['choquestion']>;
  choquestionSuivante: FormControl<IChoix['choquestionSuivante']>;
};

export type ChoixFormGroup = FormGroup<ChoixFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ChoixFormService {
  createChoixFormGroup(choix: ChoixFormGroupInput = { id: null }): ChoixFormGroup {
    const choixRawValue = {
      ...this.getFormDefaults(),
      ...choix,
    };
    return new FormGroup<ChoixFormGroupContent>({
      id: new FormControl(
        { value: choixRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      chovaleur: new FormControl(choixRawValue.chovaleur),
      choquestion: new FormControl(choixRawValue.choquestion),
      choquestionSuivante: new FormControl(choixRawValue.choquestionSuivante),
    });
  }

  getChoix(form: ChoixFormGroup): IChoix | NewChoix {
    return form.getRawValue() as IChoix | NewChoix;
  }

  resetForm(form: ChoixFormGroup, choix: ChoixFormGroupInput): void {
    const choixRawValue = { ...this.getFormDefaults(), ...choix };
    form.reset(
      {
        ...choixRawValue,
        id: { value: choixRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): ChoixFormDefaults {
    return {
      id: null,
    };
  }
}
