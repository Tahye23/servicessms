import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IEntitedetest, NewEntitedetest } from '../entitedetest.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IEntitedetest for edit and NewEntitedetestFormGroupInput for create.
 */
type EntitedetestFormGroupInput = IEntitedetest | PartialWithRequiredKeyOf<NewEntitedetest>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IEntitedetest | NewEntitedetest> = Omit<T, 'champdate'> & {
  champdate?: string | null;
};

type EntitedetestFormRawValue = FormValueOf<IEntitedetest>;

type NewEntitedetestFormRawValue = FormValueOf<NewEntitedetest>;

type EntitedetestFormDefaults = Pick<NewEntitedetest, 'id' | 'chamb' | 'champdate'>;

type EntitedetestFormGroupContent = {
  id: FormControl<EntitedetestFormRawValue['id'] | NewEntitedetest['id']>;
  identite: FormControl<EntitedetestFormRawValue['identite']>;
  nom: FormControl<EntitedetestFormRawValue['nom']>;
  nombrec: FormControl<EntitedetestFormRawValue['nombrec']>;
  chamb: FormControl<EntitedetestFormRawValue['chamb']>;
  champdate: FormControl<EntitedetestFormRawValue['champdate']>;
};

export type EntitedetestFormGroup = FormGroup<EntitedetestFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class EntitedetestFormService {
  createEntitedetestFormGroup(entitedetest: EntitedetestFormGroupInput = { id: null }): EntitedetestFormGroup {
    const entitedetestRawValue = this.convertEntitedetestToEntitedetestRawValue({
      ...this.getFormDefaults(),
      ...entitedetest,
    });
    return new FormGroup<EntitedetestFormGroupContent>({
      id: new FormControl(
        { value: entitedetestRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      identite: new FormControl(entitedetestRawValue.identite),
      nom: new FormControl(entitedetestRawValue.nom),
      nombrec: new FormControl(entitedetestRawValue.nombrec),
      chamb: new FormControl(entitedetestRawValue.chamb),
      champdate: new FormControl(entitedetestRawValue.champdate),
    });
  }

  getEntitedetest(form: EntitedetestFormGroup): IEntitedetest | NewEntitedetest {
    return this.convertEntitedetestRawValueToEntitedetest(form.getRawValue() as EntitedetestFormRawValue | NewEntitedetestFormRawValue);
  }

  resetForm(form: EntitedetestFormGroup, entitedetest: EntitedetestFormGroupInput): void {
    const entitedetestRawValue = this.convertEntitedetestToEntitedetestRawValue({ ...this.getFormDefaults(), ...entitedetest });
    form.reset(
      {
        ...entitedetestRawValue,
        id: { value: entitedetestRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): EntitedetestFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      chamb: false,
      champdate: currentTime,
    };
  }

  private convertEntitedetestRawValueToEntitedetest(
    rawEntitedetest: EntitedetestFormRawValue | NewEntitedetestFormRawValue,
  ): IEntitedetest | NewEntitedetest {
    return {
      ...rawEntitedetest,
      champdate: dayjs(rawEntitedetest.champdate, DATE_TIME_FORMAT),
    };
  }

  private convertEntitedetestToEntitedetestRawValue(
    entitedetest: IEntitedetest | (Partial<NewEntitedetest> & EntitedetestFormDefaults),
  ): EntitedetestFormRawValue | PartialWithRequiredKeyOf<NewEntitedetestFormRawValue> {
    return {
      ...entitedetest,
      champdate: entitedetest.champdate ? entitedetest.champdate.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
