import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators, AbstractControl } from '@angular/forms';

import { IPlanabonnement, NewPlanabonnement } from '../planabonnement.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IPlanabonnement for edit and NewPlanabonnementFormGroupInput for create.
 */
type PlanabonnementFormGroupInput = IPlanabonnement | PartialWithRequiredKeyOf<NewPlanabonnement>;

type PlanabonnementFormDefaults = Pick<NewPlanabonnement, 'id'>;

type PlanabonnementFormGroupContent = {
  id: FormControl<IPlanabonnement['id'] | NewPlanabonnement['id']>;

  // Champs de base
  abpName: FormControl<IPlanabonnement['abpName']>;
  abpDescription: FormControl<IPlanabonnement['abpDescription']>;
  abpPrice: FormControl<IPlanabonnement['abpPrice']>;
  abpCurrency: FormControl<IPlanabonnement['abpCurrency']>;
  abpPeriod: FormControl<IPlanabonnement['abpPeriod']>;
  abpFeatures: FormControl<IPlanabonnement['abpFeatures']>;
  abpButtonText: FormControl<IPlanabonnement['abpButtonText']>;
  buttonClass: FormControl<IPlanabonnement['buttonClass']>;
  abpPopular: FormControl<IPlanabonnement['abpPopular']>;
  active: FormControl<IPlanabonnement['active']>;

  // Type et limites
  planType: FormControl<IPlanabonnement['planType']>;
  smsLimit: FormControl<IPlanabonnement['smsLimit']>;
  whatsappLimit: FormControl<IPlanabonnement['whatsappLimit']>;
  usersLimit: FormControl<IPlanabonnement['usersLimit']>;
  templatesLimit: FormControl<IPlanabonnement['templatesLimit']>;

  // Permissions
  canManageUsers: FormControl<IPlanabonnement['canManageUsers']>;
  canViewDashboard: FormControl<IPlanabonnement['canViewDashboard']>;
  canManageAPI: FormControl<IPlanabonnement['canManageAPI']>;
  canManageTemplates: FormControl<IPlanabonnement['canManageTemplates']>;
  canViewConversations: FormControl<IPlanabonnement['canViewConversations']>;
  canViewAnalytics: FormControl<IPlanabonnement['canViewAnalytics']>;
  prioritySupport: FormControl<IPlanabonnement['prioritySupport']>;

  // Limites techniques
  maxApiCallsPerDay: FormControl<IPlanabonnement['maxApiCallsPerDay']>;
  storageLimitMb: FormControl<IPlanabonnement['storageLimitMb']>;
  sortOrder: FormControl<IPlanabonnement['sortOrder']>;
  customPlan: FormControl<IPlanabonnement['customPlan']>;
};

export type PlanabonnementFormGroup = FormGroup<PlanabonnementFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class PlanabonnementFormService {
  createPlanabonnementFormGroup(planabonnement: Partial<IPlanabonnement> | NewPlanabonnement = { id: null }): PlanabonnementFormGroup {
    const planabonnementRawValue = {
      ...this.getFormDefaults(),
      ...planabonnement,
    };

    return new FormGroup<PlanabonnementFormGroupContent>({
      id: new FormControl(
        { value: planabonnementRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),

      // === CHAMPS DE BASE ===
      abpName: new FormControl(planabonnementRawValue.abpName ?? '', {
        validators: [Validators.required, Validators.minLength(2), Validators.maxLength(255)],
      }),
      canViewDashboard: new FormControl(planabonnementRawValue.canViewDashboard ?? true),
      canManageAPI: new FormControl(planabonnementRawValue.canManageAPI ?? false),

      abpDescription: new FormControl(planabonnementRawValue.abpDescription ?? '', {
        validators: [Validators.maxLength(1000)],
      }),

      abpPrice: new FormControl(planabonnementRawValue.abpPrice ?? 0, {
        validators: [Validators.min(0)],
      }),

      abpCurrency: new FormControl(planabonnementRawValue.abpCurrency ?? 'MRU', {
        validators: [Validators.pattern(/^[A-Z]{3}$/)],
      }),

      abpPeriod: new FormControl(planabonnementRawValue.abpPeriod ?? '', {
        validators: [Validators.pattern(/^(MONTHLY|YEARLY|LIFETIME)$/)],
      }),

      abpFeatures: new FormControl(planabonnementRawValue.abpFeatures ?? ''),

      abpButtonText: new FormControl(planabonnementRawValue.abpButtonText ?? ''),

      buttonClass: new FormControl(planabonnementRawValue.buttonClass ?? ''),

      abpPopular: new FormControl(planabonnementRawValue.abpPopular ?? false),

      active: new FormControl(planabonnementRawValue.active ?? true),

      // === TYPE ET LIMITES ===
      planType: new FormControl(planabonnementRawValue.planType ?? '', {
        validators: [Validators.required, Validators.pattern(/^(FREE|SMS|WHATSAPP|PREMIUM|ENTERPRISE)$/)],
      }),

      smsLimit: new FormControl(planabonnementRawValue.smsLimit ?? 0, {
        validators: [Validators.min(-1)], // -1 pour illimité
      }),

      whatsappLimit: new FormControl(planabonnementRawValue.whatsappLimit ?? 0, {
        validators: [Validators.min(-1)], // -1 pour illimité
      }),

      usersLimit: new FormControl(planabonnementRawValue.usersLimit ?? 1, {
        validators: [this.usersLimitValidator],
      }),

      templatesLimit: new FormControl(planabonnementRawValue.templatesLimit ?? 0, {
        validators: [Validators.min(-1)], // -1 pour illimité
      }),

      // === PERMISSIONS ===
      canManageUsers: new FormControl(planabonnementRawValue.canManageUsers ?? false),
      canManageTemplates: new FormControl(planabonnementRawValue.canManageTemplates ?? false),

      canViewConversations: new FormControl(planabonnementRawValue.canViewConversations ?? false),

      canViewAnalytics: new FormControl(planabonnementRawValue.canViewAnalytics ?? false),

      prioritySupport: new FormControl(planabonnementRawValue.prioritySupport ?? false),

      // === LIMITES TECHNIQUES ===
      maxApiCallsPerDay: new FormControl(planabonnementRawValue.maxApiCallsPerDay ?? 100, {
        validators: [Validators.min(-1)], // -1 pour illimité
      }),

      storageLimitMb: new FormControl(planabonnementRawValue.storageLimitMb ?? 100, {
        validators: [Validators.min(1)],
      }),

      sortOrder: new FormControl(planabonnementRawValue.sortOrder ?? 0, {
        validators: [Validators.min(0)],
      }),
      customPlan: new FormControl(planabonnementRawValue.customPlan ?? false),
    });
  }

  /**
   * Validateur personnalisé pour la limite d'utilisateurs
   */
  private usersLimitValidator(control: AbstractControl): { [key: string]: any } | null {
    const value = control.value;

    // -1 pour illimité est accepté
    if (value === -1) {
      return null;
    }

    // Sinon, doit être au minimum 1
    if (value < 1) {
      return { minUsers: { value: value, min: 1 } };
    }

    return null;
  }

  getPlanabonnement(form: PlanabonnementFormGroup): IPlanabonnement | NewPlanabonnement {
    const formValue = form.getRawValue();

    // Retourne tout, y compris customPlan
    return formValue as IPlanabonnement | NewPlanabonnement;
  }

  resetForm(form: PlanabonnementFormGroup, planabonnement: PlanabonnementFormGroupInput): void {
    const planabonnementRawValue = {
      ...this.getFormDefaults(),
      ...planabonnement,
    };

    form.reset(
      {
        ...planabonnementRawValue,
        id: { value: planabonnementRawValue.id, disabled: true },
        customPlan: false, // Réinitialiser le champ personnalisé
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */,
    );
  }

  private getFormDefaults(): NewPlanabonnement {
    return {
      id: null,

      // Champs de base
      abpName: '',
      abpDescription: '',
      abpPrice: 0,
      abpCurrency: 'MRU',
      abpPeriod: '',
      abpFeatures: '',
      abpButtonText: '',
      buttonClass: '',
      abpPopular: false,
      active: true,

      // Type et limites
      planType: '',
      smsLimit: 0,
      whatsappLimit: 0,
      usersLimit: 1,
      templatesLimit: 0,
      customPlan: false,
      // Permissions
      canManageUsers: false,
      canManageTemplates: false,
      canViewConversations: false,
      canViewAnalytics: false,
      prioritySupport: false,

      // Limites techniques
      maxApiCallsPerDay: 100,
      storageLimitMb: 100,
      sortOrder: 0,

      // Dates (si nécessaires)
      createdDate: null,
      updatedDate: null,
    };
  }

  /**
   * Valide les règles métier spécifiques
   */
  validateBusinessRules(form: PlanabonnementFormGroup): string[] {
    const errors: string[] = [];
    const formValue = form.getRawValue();

    // Plan gratuit ne peut pas avoir de prix
    if (formValue.planType === 'FREE' && formValue.abpPrice && formValue.abpPrice > 0) {
      errors.push('Un plan gratuit ne peut pas avoir un prix supérieur à 0');
    }

    // Vérifier la cohérence des limites
    if (formValue.usersLimit && formValue.usersLimit < 1 && formValue.usersLimit !== -1) {
      errors.push("La limite d'utilisateurs doit être au moins 1 ou -1 pour illimité");
    }

    // Plan Enterprise devrait avoir toutes les permissions
    if (formValue.planType === 'ENTERPRISE') {
      if (!formValue.canManageUsers || !formValue.canManageTemplates || !formValue.canViewConversations || !formValue.canViewAnalytics) {
        errors.push('Un plan Enterprise devrait avoir toutes les permissions activées');
      }
    }

    // Plan SMS doit avoir une limite SMS définie
    if (formValue.planType === 'SMS') {
      if (!formValue.smsLimit || (formValue.smsLimit <= 0 && formValue.smsLimit !== -1)) {
        errors.push('Un plan SMS doit avoir une limite SMS positive ou illimitée (-1)');
      }
    }

    // Plan WhatsApp doit avoir une limite WhatsApp définie
    if (formValue.planType === 'WHATSAPP') {
      if (!formValue.whatsappLimit || (formValue.whatsappLimit <= 0 && formValue.whatsappLimit !== -1)) {
        errors.push('Un plan WhatsApp doit avoir une limite WhatsApp positive ou illimitée (-1)');
      }
    }

    // Vérification du prix pour les plans payants
    if (
      formValue.planType &&
      ['SMS', 'WHATSAPP', 'PREMIUM', 'ENTERPRISE'].includes(formValue.planType) &&
      (!formValue.abpPrice || formValue.abpPrice <= 0)
    ) {
      errors.push('Les plans payants doivent avoir un prix supérieur à 0');
    }

    // Vérification de la période pour les plans payants
    if (formValue.planType && ['SMS', 'WHATSAPP', 'PREMIUM', 'ENTERPRISE'].includes(formValue.planType) && !formValue.abpPeriod) {
      errors.push('Les plans payants doivent avoir une période définie');
    }

    return errors;
  }

  /**
   * Configure automatiquement les valeurs selon le type de plan
   */
  configureByPlanType(form: PlanabonnementFormGroup, planType: string): void {
    const configurations: { [key: string]: any } = {
      FREE: {
        abpPrice: 0,
        abpPeriod: 'LIFETIME',
        abpButtonText: 'Commencer gratuitement',
        buttonClass: 'btn-success',
        smsLimit: 10,
        whatsappLimit: 10,
        usersLimit: 1,
        templatesLimit: 5,
        maxApiCallsPerDay: 50,
        storageLimitMb: 50,
        canManageUsers: false,
        canManageTemplates: false,
        canViewConversations: true,
        canViewAnalytics: false,
        prioritySupport: false,
        abpFeatures: 'Accès de base, Support communautaire, Limites réduites',
      },
      SMS: {
        abpPrice: 29.99,
        abpPeriod: 'MONTHLY',
        abpButtonText: 'Choisir SMS',
        buttonClass: 'btn-primary',
        smsLimit: 1000,
        whatsappLimit: 0,
        usersLimit: 3,
        templatesLimit: 20,
        maxApiCallsPerDay: 500,
        storageLimitMb: 200,
        canManageUsers: true,
        canManageTemplates: true,
        canViewConversations: true,
        canViewAnalytics: true,
        prioritySupport: false,
        abpFeatures: "1000 SMS/mois, Gestion d'équipe, Templates avancés, Analytics",
      },
      WHATSAPP: {
        abpPrice: 49.99,
        abpPeriod: 'MONTHLY',
        abpButtonText: 'Choisir WhatsApp',
        buttonClass: 'btn-success',
        smsLimit: 0,
        whatsappLimit: 1000,
        usersLimit: 5,
        templatesLimit: 50,
        maxApiCallsPerDay: 1000,
        storageLimitMb: 500,
        canManageUsers: true,
        canManageTemplates: true,
        canViewConversations: true,
        canViewAnalytics: true,
        prioritySupport: true,
        abpFeatures: '1000 WhatsApp/mois, Support prioritaire, Intégrations, API avancée',
      },
      PREMIUM: {
        abpPrice: 99.99,
        abpPeriod: 'MONTHLY',
        abpButtonText: 'Choisir Premium',
        buttonClass: 'btn-warning',
        abpPopular: true,
        smsLimit: 5000,
        whatsappLimit: 5000,
        usersLimit: 10,
        templatesLimit: 100,
        maxApiCallsPerDay: 5000,
        storageLimitMb: 2000,
        canManageUsers: true,
        canManageTemplates: true,
        canViewConversations: true,
        canViewAnalytics: true,
        prioritySupport: true,
        abpFeatures: 'SMS + WhatsApp illimités, Équipe étendue, Analytics avancés, Support prioritaire',
      },
      ENTERPRISE: {
        abpPrice: 299.99,
        abpPeriod: 'MONTHLY',
        abpButtonText: 'Nous contacter',
        buttonClass: 'btn-danger',
        smsLimit: -1, // Illimité
        whatsappLimit: -1,
        usersLimit: -1,
        templatesLimit: -1,
        maxApiCallsPerDay: -1,
        storageLimitMb: 10000,
        canManageUsers: true,
        canManageTemplates: true,
        canViewConversations: true,
        canViewAnalytics: true,
        prioritySupport: true,
        abpFeatures: 'Tout illimité, Support dédié, Intégrations personnalisées, SLA garanti',
      },
    };

    const config = configurations[planType];
    if (config) {
      form.patchValue(config);
    }
  }

  /**
   * Obtient les valeurs par défaut pour un type de plan spécifique
   */
  getDefaultsForPlanType(planType: string): Partial<IPlanabonnement> {
    const configurations: { [key: string]: Partial<IPlanabonnement> } = {
      FREE: { sortOrder: 1 },
      SMS: { sortOrder: 2 },
      WHATSAPP: { sortOrder: 3 },
      PREMIUM: { sortOrder: 4 },
      ENTERPRISE: { sortOrder: 5 },
    };

    return configurations[planType] || {};
  }

  /**
   * Valide si un plan peut être populaire (business rule)
   */
  canBePopopular(planType: string): boolean {
    // Seuls les plans Premium et Enterprise peuvent être populaires par défaut
    return ['PREMIUM', 'ENTERPRISE'].includes(planType);
  }

  /**
   * Génère des features automatiques selon le type de plan
   */
  generateAutoFeatures(planType: string, formValue: any): string {
    const features: string[] = [];

    // Features selon les limites
    if (formValue.smsLimit === -1) {
      features.push('SMS illimités');
    } else if (formValue.smsLimit > 0) {
      features.push(`${formValue.smsLimit} SMS inclus`);
    }

    if (formValue.whatsappLimit === -1) {
      features.push('WhatsApp illimité');
    } else if (formValue.whatsappLimit > 0) {
      features.push(`${formValue.whatsappLimit} messages WhatsApp`);
    }

    if (formValue.usersLimit === -1) {
      features.push('Utilisateurs illimités');
    } else if (formValue.usersLimit > 1) {
      features.push(`Jusqu'à ${formValue.usersLimit} utilisateurs`);
    }

    // Features selon les permissions
    if (formValue.canViewAnalytics) {
      features.push('Tableaux de bord avancés');
    }

    if (formValue.prioritySupport) {
      features.push('Support prioritaire');
    }

    if (formValue.canManageTemplates) {
      features.push('Gestion des templates');
    }

    return features.join(', ');
  }

  /**
   * Clone la configuration d'un plan existant
   */
  clonePlanConfiguration(form: PlanabonnementFormGroup, sourcePlan: IPlanabonnement): void {
    const clonedValues = {
      ...sourcePlan,
      id: null,
      abpName: `${sourcePlan.abpName} (Copie)`,
      active: false, // Désactiver la copie par défaut
      createdDate: null,
      updatedDate: null,
    };

    this.resetForm(form, clonedValues);
  }
}
