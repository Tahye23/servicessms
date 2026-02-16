import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { IAbonnement, NewAbonnement, SubscriptionStatus } from '../abonnement.model';

type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };
type AbonnementFormGroupInput = IAbonnement | PartialWithRequiredKeyOf<NewAbonnement>;
type FormDefaults = Pick<NewAbonnement, 'id' | 'status' | 'active' | 'autoRenew' | 'isTrial' | 'isCustomPlan'>;

type AbonnementFormGroupContent = {
  id: FormControl<IAbonnement['id'] | NewAbonnement['id']>;
  user: FormControl<IAbonnement['user']>;
  plan: FormControl<IAbonnement['plan']>;
  status: FormControl<IAbonnement['status']>;
  startDate: FormControl<IAbonnement['startDate']>;
  endDate: FormControl<IAbonnement['endDate']>;
  createdDate: FormControl<IAbonnement['createdDate']>;
  updatedDate: FormControl<IAbonnement['updatedDate']>;
  active: FormControl<IAbonnement['active']>;
  planId: FormControl<IAbonnement['planId']>;
  userId: FormControl<IAbonnement['userId']>;
  isCustomPlan: FormControl<IAbonnement['isCustomPlan']>;
  customPrice: FormControl<IAbonnement['customPrice']>;
  customPeriod: FormControl<IAbonnement['customPeriod']>;
  customName: FormControl<IAbonnement['customName']>;
  customDescription: FormControl<IAbonnement['customDescription']>;
  canViewDashboard: FormControl<IAbonnement['canViewDashboard']>;
  canManageAPI: FormControl<IAbonnement['canManageAPI']>;
  apiCallLimitPerDay: FormControl<IAbonnement['apiCallLimitPerDay']>;
  apiCallsUsedToday: FormControl<IAbonnement['apiCallsUsedToday']>;
  customSmsLimit: FormControl<IAbonnement['customSmsLimit']>;
  customWhatsappLimit: FormControl<IAbonnement['customWhatsappLimit']>;
  customUsersLimit: FormControl<IAbonnement['customUsersLimit']>;
  customTemplatesLimit: FormControl<IAbonnement['customTemplatesLimit']>;
  customApiCallsLimit: FormControl<IAbonnement['customApiCallsLimit']>;
  customStorageLimitMb: FormControl<IAbonnement['customStorageLimitMb']>;

  customCanManageUsers: FormControl<IAbonnement['customCanManageUsers']>;
  customCanManageTemplates: FormControl<IAbonnement['customCanManageTemplates']>;
  customCanViewConversations: FormControl<IAbonnement['customCanViewConversations']>;
  customCanViewAnalytics: FormControl<IAbonnement['customCanViewAnalytics']>;
  customPrioritySupport: FormControl<IAbonnement['customPrioritySupport']>;

  smsUsed: FormControl<IAbonnement['smsUsed']>;
  whatsappUsed: FormControl<IAbonnement['whatsappUsed']>;
  apiCallsToday: FormControl<IAbonnement['apiCallsToday']>;
  storageUsedMb: FormControl<IAbonnement['storageUsedMb']>;
  lastApiCallDate: FormControl<IAbonnement['lastApiCallDate']>;

  bonusSmsEnabled: FormControl<IAbonnement['bonusSmsEnabled']>;
  bonusSmsAmount: FormControl<IAbonnement['bonusSmsAmount']>;
  bonusWhatsappEnabled: FormControl<IAbonnement['bonusWhatsappEnabled']>;
  bonusWhatsappAmount: FormControl<IAbonnement['bonusWhatsappAmount']>;

  allowSmsCarryover: FormControl<IAbonnement['allowSmsCarryover']>;
  allowWhatsappCarryover: FormControl<IAbonnement['allowWhatsappCarryover']>;
  carriedOverSms: FormControl<IAbonnement['carriedOverSms']>;
  carriedOverWhatsapp: FormControl<IAbonnement['carriedOverWhatsapp']>;

  paymentMethod: FormControl<IAbonnement['paymentMethod']>;
  transactionId: FormControl<IAbonnement['transactionId']>;
  autoRenew: FormControl<IAbonnement['autoRenew']>;

  isTrial: FormControl<IAbonnement['isTrial']>;
  trialEndDate: FormControl<IAbonnement['trialEndDate']>;
};

export type AbonnementFormGroup = FormGroup<AbonnementFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class AbonnementFormService {
  private getFormDefaults(): FormDefaults {
    return {
      id: null,
      status: SubscriptionStatus.ACTIVE,
      active: true,
      autoRenew: true,
      isTrial: false,
      isCustomPlan: false,
    };
  }

  createAbonnementFormGroup(abonnement: AbonnementFormGroupInput = { id: null }): AbonnementFormGroup {
    const abonnementRawValue = {
      ...this.getFormDefaults(),
      ...abonnement,
    };

    return new FormGroup<AbonnementFormGroupContent>({
      id: new FormControl({ value: abonnementRawValue.id, disabled: true }, { nonNullable: true, validators: [Validators.required] }),

      user: new FormControl(abonnementRawValue.user, { validators: [Validators.required] }),

      plan: new FormControl(abonnementRawValue.plan),

      status: new FormControl(abonnementRawValue.status, { validators: [Validators.required] }),

      startDate: new FormControl(abonnementRawValue.startDate),

      endDate: new FormControl(abonnementRawValue.endDate),

      createdDate: new FormControl(abonnementRawValue.createdDate),

      updatedDate: new FormControl(abonnementRawValue.updatedDate),
      planId: new FormControl(abonnementRawValue.planId),
      userId: new FormControl(abonnementRawValue.userId),
      active: new FormControl(abonnementRawValue.active, { nonNullable: true }),

      isCustomPlan: new FormControl(abonnementRawValue.isCustomPlan),

      customPrice: new FormControl(abonnementRawValue.customPrice),

      customPeriod: new FormControl(abonnementRawValue.customPeriod),

      customName: new FormControl(abonnementRawValue.customName, { validators: [Validators.maxLength(255)] }),

      customDescription: new FormControl(abonnementRawValue.customDescription),

      customSmsLimit: new FormControl(abonnementRawValue.customSmsLimit, { validators: [Validators.min(-1)] }),

      customWhatsappLimit: new FormControl(abonnementRawValue.customWhatsappLimit, { validators: [Validators.min(-1)] }),

      customUsersLimit: new FormControl(abonnementRawValue.customUsersLimit, { validators: [Validators.min(-1)] }),

      customTemplatesLimit: new FormControl(abonnementRawValue.customTemplatesLimit, { validators: [Validators.min(-1)] }),

      customApiCallsLimit: new FormControl(abonnementRawValue.customApiCallsLimit),

      customStorageLimitMb: new FormControl(abonnementRawValue.customStorageLimitMb, { validators: [Validators.min(1)] }),

      customCanManageUsers: new FormControl(abonnementRawValue.customCanManageUsers),

      customCanManageTemplates: new FormControl(abonnementRawValue.customCanManageTemplates),

      customCanViewConversations: new FormControl(abonnementRawValue.customCanViewConversations),

      customCanViewAnalytics: new FormControl(abonnementRawValue.customCanViewAnalytics),

      customPrioritySupport: new FormControl(abonnementRawValue.customPrioritySupport),

      smsUsed: new FormControl(abonnementRawValue.smsUsed),

      whatsappUsed: new FormControl(abonnementRawValue.whatsappUsed),

      apiCallsToday: new FormControl(abonnementRawValue.apiCallsToday),

      storageUsedMb: new FormControl(abonnementRawValue.storageUsedMb),

      lastApiCallDate: new FormControl(abonnementRawValue.lastApiCallDate),

      bonusSmsEnabled: new FormControl(abonnementRawValue.bonusSmsEnabled),

      bonusSmsAmount: new FormControl(abonnementRawValue.bonusSmsAmount),

      bonusWhatsappEnabled: new FormControl(abonnementRawValue.bonusWhatsappEnabled),

      bonusWhatsappAmount: new FormControl(abonnementRawValue.bonusWhatsappAmount),

      allowSmsCarryover: new FormControl(abonnementRawValue.allowSmsCarryover),

      allowWhatsappCarryover: new FormControl(abonnementRawValue.allowWhatsappCarryover),

      carriedOverSms: new FormControl(abonnementRawValue.carriedOverSms),

      carriedOverWhatsapp: new FormControl(abonnementRawValue.carriedOverWhatsapp),

      paymentMethod: new FormControl(abonnementRawValue.paymentMethod),

      transactionId: new FormControl(abonnementRawValue.transactionId),

      autoRenew: new FormControl(abonnementRawValue.autoRenew, { nonNullable: true }),

      isTrial: new FormControl(abonnementRawValue.isTrial, { nonNullable: true }),

      trialEndDate: new FormControl(abonnementRawValue.trialEndDate),
      canViewDashboard: new FormControl(abonnementRawValue.canViewDashboard ?? true),
      canManageAPI: new FormControl(abonnementRawValue.canManageAPI ?? false),
      apiCallLimitPerDay: new FormControl(abonnementRawValue.apiCallLimitPerDay ?? 0),
      apiCallsUsedToday: new FormControl(abonnementRawValue.apiCallsUsedToday ?? 0),
    });
  }

  getAbonnement(form: AbonnementFormGroup): IAbonnement | NewAbonnement {
    const formValue = form.getRawValue();

    return formValue as IAbonnement | NewAbonnement;
  }

  resetForm(form: AbonnementFormGroup, abonnement: AbonnementFormGroupInput): void {
    const abonnementRawValue = {
      ...this.getFormDefaults(),
      ...abonnement,
    };

    form.reset({
      ...abonnementRawValue,
      id: { value: abonnementRawValue.id, disabled: true },
    } as any);
  }

  private cleanFormData(formValue: any): any {
    const cleaned = { ...formValue };

    if (!cleaned.isCustomPlan) {
      const customFields = [
        'customPrice',
        'customPeriod',
        'customName',
        'customDescription',
        'customSmsLimit',
        'customWhatsappLimit',
        'customUsersLimit',
        'customTemplatesLimit',
        'customApiCallsLimit',
        'customStorageLimitMb',
        'customCanManageUsers',
        'customCanManageTemplates',
        'customCanViewConversations',
        'customCanViewAnalytics',
        'customPrioritySupport',
      ];
      customFields.forEach(field => (cleaned[field] = null));
    } else {
      cleaned.plan = null;
    }

    if (!cleaned.bonusSmsEnabled) cleaned.bonusSmsAmount = 0;
    if (!cleaned.bonusWhatsappEnabled) cleaned.bonusWhatsappAmount = 0;
    if (!cleaned.allowSmsCarryover) cleaned.carriedOverSms = 0;
    if (!cleaned.allowWhatsappCarryover) cleaned.carriedOverWhatsapp = 0;
    if (!cleaned.isTrial) cleaned.trialEndDate = null;

    Object.keys(cleaned).forEach(key => {
      if (cleaned[key] === '') cleaned[key] = null;
    });

    return cleaned;
  }
}
