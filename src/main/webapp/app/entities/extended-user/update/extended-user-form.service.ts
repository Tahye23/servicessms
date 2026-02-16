import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IExtendedUser, NewExtendedUser } from '../extended-user.model';

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

type ExtendedUserFormGroupInput = IExtendedUser | PartialWithRequiredKeyOf<NewExtendedUser>;

type ExtendedUserFormDefaults = Pick<
  NewExtendedUser,
  | 'id'
  | 'notificationsEmail'
  | 'notificationsSms'
  | 'marketingEmails'
  | 'smsQuota'
  | 'whatsappQuota'
  | 'smsUsedThisMonth'
  | 'whatsappUsedThisMonth'
>;

type ExtendedUserFormGroupContent = {
  id: FormControl<IExtendedUser['id'] | NewExtendedUser['id']>;
  phoneNumber: FormControl<IExtendedUser['phoneNumber']>;
  address: FormControl<IExtendedUser['address']>;
  city: FormControl<IExtendedUser['city']>;
  country: FormControl<IExtendedUser['country']>;
  postalCode: FormControl<IExtendedUser['postalCode']>;
  companyName: FormControl<IExtendedUser['companyName']>;
  website: FormControl<IExtendedUser['website']>;
  notificationsEmail: FormControl<IExtendedUser['notificationsEmail']>;
  notificationsSms: FormControl<IExtendedUser['notificationsSms']>;
  marketingEmails: FormControl<IExtendedUser['marketingEmails']>;
  smsQuota: FormControl<IExtendedUser['smsQuota']>;
  whatsappQuota: FormControl<IExtendedUser['whatsappQuota']>;
  smsUsedThisMonth: FormControl<IExtendedUser['smsUsedThisMonth']>;
  whatsappUsedThisMonth: FormControl<IExtendedUser['whatsappUsedThisMonth']>;
  user: FormControl<IExtendedUser['user']>;
};

export type ExtendedUserFormGroup = FormGroup<ExtendedUserFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ExtendedUserFormService {
  createExtendedUserFormGroup(extendedUser: ExtendedUserFormGroupInput = { id: null }): ExtendedUserFormGroup {
    const extendedUserRawValue = {
      ...this.getFormDefaults(),
      ...extendedUser,
    };
    return new FormGroup<ExtendedUserFormGroupContent>({
      id: new FormControl(
        { value: extendedUserRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      phoneNumber: new FormControl(extendedUserRawValue.phoneNumber),
      address: new FormControl(extendedUserRawValue.address),
      city: new FormControl(extendedUserRawValue.city),
      country: new FormControl(extendedUserRawValue.country),
      postalCode: new FormControl(extendedUserRawValue.postalCode),
      companyName: new FormControl(extendedUserRawValue.companyName),
      website: new FormControl(extendedUserRawValue.website),
      notificationsEmail: new FormControl(extendedUserRawValue.notificationsEmail),
      notificationsSms: new FormControl(extendedUserRawValue.notificationsSms),
      marketingEmails: new FormControl(extendedUserRawValue.marketingEmails),
      smsQuota: new FormControl(extendedUserRawValue.smsQuota),
      whatsappQuota: new FormControl(extendedUserRawValue.whatsappQuota),
      smsUsedThisMonth: new FormControl(extendedUserRawValue.smsUsedThisMonth),
      whatsappUsedThisMonth: new FormControl(extendedUserRawValue.whatsappUsedThisMonth),
      user: new FormControl(extendedUserRawValue.user),
    });
  }

  getExtendedUser(form: ExtendedUserFormGroup): IExtendedUser | NewExtendedUser {
    return form.getRawValue() as IExtendedUser | NewExtendedUser;
  }

  resetForm(form: ExtendedUserFormGroup, extendedUser: ExtendedUserFormGroupInput): void {
    const extendedUserRawValue = { ...this.getFormDefaults(), ...extendedUser };
    form.reset({
      ...extendedUserRawValue,
      id: { value: extendedUserRawValue.id, disabled: true },
    } as any);
  }

  private getFormDefaults(): ExtendedUserFormDefaults {
    return {
      id: null,
      notificationsEmail: true,
      notificationsSms: false,
      marketingEmails: true,
      smsQuota: 0,
      whatsappQuota: 0,
      smsUsedThisMonth: 0,
      whatsappUsedThisMonth: 0,
    };
  }
}
