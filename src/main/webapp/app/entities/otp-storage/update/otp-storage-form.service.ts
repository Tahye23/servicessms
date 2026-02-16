import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IOTPStorage, NewOTPStorage } from '../otp-storage.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IOTPStorage for edit and NewOTPStorageFormGroupInput for create.
 */
type OTPStorageFormGroupInput = IOTPStorage | PartialWithRequiredKeyOf<NewOTPStorage>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IOTPStorage | NewOTPStorage> = Omit<T, 'otsdateexpir'> & {
  otsdateexpir?: string | null;
};

type OTPStorageFormRawValue = FormValueOf<IOTPStorage>;

type NewOTPStorageFormRawValue = FormValueOf<NewOTPStorage>;

type OTPStorageFormDefaults = Pick<NewOTPStorage, 'id' | 'otsdateexpir' | 'isOtpUsed' | 'isExpired'>;

type OTPStorageFormGroupContent = {
  id: FormControl<OTPStorageFormRawValue['id'] | NewOTPStorage['id']>;
  otsOTP: FormControl<OTPStorageFormRawValue['otsOTP']>;
  phoneNumber: FormControl<OTPStorageFormRawValue['phoneNumber']>;
  otsdateexpir: FormControl<OTPStorageFormRawValue['otsdateexpir']>;
  isOtpUsed: FormControl<OTPStorageFormRawValue['isOtpUsed']>;
  isExpired: FormControl<OTPStorageFormRawValue['isExpired']>;
  user: FormControl<OTPStorageFormRawValue['user']>;
};

export type OTPStorageFormGroup = FormGroup<OTPStorageFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class OTPStorageFormService {
  createOTPStorageFormGroup(oTPStorage: OTPStorageFormGroupInput = { id: null }): OTPStorageFormGroup {
    const oTPStorageRawValue = this.convertOTPStorageToOTPStorageRawValue({
      ...this.getFormDefaults(),
      ...oTPStorage,
    });
    return new FormGroup<OTPStorageFormGroupContent>({
      id: new FormControl(
        { value: oTPStorageRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      otsOTP: new FormControl(oTPStorageRawValue.otsOTP),
      phoneNumber: new FormControl(oTPStorageRawValue.phoneNumber),
      otsdateexpir: new FormControl(oTPStorageRawValue.otsdateexpir),
      isOtpUsed: new FormControl(oTPStorageRawValue.isOtpUsed),
      isExpired: new FormControl(oTPStorageRawValue.isExpired),
      user: new FormControl(oTPStorageRawValue.user),
    });
  }

  getOTPStorage(form: OTPStorageFormGroup): IOTPStorage | NewOTPStorage {
    return this.convertOTPStorageRawValueToOTPStorage(form.getRawValue() as OTPStorageFormRawValue | NewOTPStorageFormRawValue);
  }

  resetForm(form: OTPStorageFormGroup, oTPStorage: OTPStorageFormGroupInput): void {
    const oTPStorageRawValue = this.convertOTPStorageToOTPStorageRawValue({ ...this.getFormDefaults(), ...oTPStorage });
    form.reset(
      {
        ...oTPStorageRawValue,
        id: { value: oTPStorageRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): OTPStorageFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      otsdateexpir: currentTime,
      isOtpUsed: false,
      isExpired: false,
    };
  }

  private convertOTPStorageRawValueToOTPStorage(
    rawOTPStorage: OTPStorageFormRawValue | NewOTPStorageFormRawValue,
  ): IOTPStorage | NewOTPStorage {
    return {
      ...rawOTPStorage,
      otsdateexpir: dayjs(rawOTPStorage.otsdateexpir, DATE_TIME_FORMAT),
    };
  }

  private convertOTPStorageToOTPStorageRawValue(
    oTPStorage: IOTPStorage | (Partial<NewOTPStorage> & OTPStorageFormDefaults),
  ): OTPStorageFormRawValue | PartialWithRequiredKeyOf<NewOTPStorageFormRawValue> {
    return {
      ...oTPStorage,
      otsdateexpir: oTPStorage.otsdateexpir ? oTPStorage.otsdateexpir.format(DATE_TIME_FORMAT) : undefined,
    };
  }
}
