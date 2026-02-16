import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IApplication, NewApplication } from '../application.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IApplication for edit and NewApplicationFormGroupInput for create.
 */
type ApplicationFormGroupInput = IApplication | PartialWithRequiredKeyOf<NewApplication>;

type ApplicationFormDefaults = Pick<NewApplication, 'id'>;

type ApplicationFormGroupContent = {
  id: FormControl<IApplication['id'] | NewApplication['id']>;
  name: FormControl<IApplication['name']>;
  description: FormControl<IApplication['description']>;
  userId: FormControl<IApplication['userId']>;

  // Nouveaux champs pour la gestion API
  environment: FormControl<string | null>;
  webhookUrl: FormControl<string | null>;
  webhookSecret: FormControl<string | null>;
  dailyLimit: FormControl<number | null>;
  monthlyLimit: FormControl<number | null>;

  // Champs pour les services autorisés (checkboxes)
  allowSms: FormControl<boolean>;
  allowWhatsapp: FormControl<boolean>;
  allowEmail: FormControl<boolean>;
  allowVoice: FormControl<boolean>;

  // ✅ NOUVEAUX CHAMPS POUR LE TOKEN
  tokenDateExpiration: FormControl<string | null>;
  tokenNeverExpires: FormControl<boolean>;

  // Relations existantes
  api: FormControl<IApplication['api']>;
  planabonnement: FormControl<IApplication['planabonnement']>;
  utilisateur: FormControl<IApplication['utilisateur']>;
};

export type ApplicationFormGroup = FormGroup<ApplicationFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ApplicationFormService {
  createApplicationFormGroup(application: ApplicationFormGroupInput = { id: null }): ApplicationFormGroup {
    const formDefaults = this.getFormDefaults();
    const extendedDefaults = this.getExtendedDefaults();

    const applicationRawValue = {
      ...formDefaults,
      ...extendedDefaults,
      ...application,
    };

    return new FormGroup<ApplicationFormGroupContent>({
      id: new FormControl(
        { value: applicationRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        },
      ),
      name: new FormControl(applicationRawValue.name, {
        validators: [Validators.required, Validators.minLength(2), Validators.maxLength(100), this.noSpecialCharactersValidator],
      }),
      description: new FormControl(applicationRawValue.description, {
        validators: [Validators.maxLength(500)],
      }),
      userId: new FormControl(applicationRawValue.userId),

      // Nouveaux champs - Valeurs normalisées
      environment: new FormControl<string | null>(applicationRawValue.environment, { validators: [Validators.required] }),
      webhookUrl: new FormControl<string | null>(this.normalizeValue(applicationRawValue.webhookUrl), {
        validators: [Validators.pattern(/^https?:\/\/.+/)],
      }),
      webhookSecret: new FormControl<string | null>(this.normalizeValue(applicationRawValue.webhookSecret), {
        validators: [Validators.minLength(8)],
      }),
      // ✅ FIXE: Convertir en nombre explicitement
      dailyLimit: new FormControl<number | null>(this.normalizeNumberValue(applicationRawValue.dailyLimit), {
        validators: [Validators.min(1), Validators.max(1000000)],
      }),
      // ✅ FIXE: Convertir en nombre explicitement
      monthlyLimit: new FormControl<number | null>(this.normalizeNumberValue(applicationRawValue.monthlyLimit), {
        validators: [Validators.min(1), Validators.max(10000000)],
      }),

      // Services autorisés (checkboxes)
      allowSms: new FormControl<boolean>(this.isServiceAllowed(applicationRawValue.allowedServices, 'sms'), { nonNullable: true }),
      allowWhatsapp: new FormControl<boolean>(this.isServiceAllowed(applicationRawValue.allowedServices, 'whatsapp'), {
        nonNullable: true,
      }),
      allowEmail: new FormControl<boolean>(this.isServiceAllowed(applicationRawValue.allowedServices, 'email'), { nonNullable: true }),
      allowVoice: new FormControl<boolean>(this.isServiceAllowed(applicationRawValue.allowedServices, 'voice'), { nonNullable: true }),

      // ✅ CHAMPS POUR LE TOKEN
      tokenDateExpiration: new FormControl<string | null>(null),
      tokenNeverExpires: new FormControl<boolean>(false, { nonNullable: true }),

      // Relations existantes
      api: new FormControl(applicationRawValue.api),
      planabonnement: new FormControl(applicationRawValue.planabonnement),
      utilisateur: new FormControl(applicationRawValue.utilisateur),
    });
  }

  getApplication(form: ApplicationFormGroup): IApplication | NewApplication {
    const rawValue = form.getRawValue() as any;

    // Construire la liste des services autorisés
    const allowedServices: string[] = [];
    if (rawValue.allowSms) allowedServices.push('sms');
    if (rawValue.allowWhatsapp) allowedServices.push('whatsapp');
    if (rawValue.allowEmail) allowedServices.push('email');
    if (rawValue.allowVoice) allowedServices.push('voice');

    // ✅ PRÉPARER LES DONNÉES POUR LE TOKEN
    const application = {
      ...rawValue,
      // ✅ FIXE: Assurer que allowedServices n'est jamais vide ou null
      allowedServices: allowedServices.length > 0 ? allowedServices : [],
      isActive: rawValue.id ? rawValue.isActive : true,

      // ✅ AJOUTER LES CHAMPS POUR LE TOKEN (seulement à la création)
      tokenDateExpiration: rawValue.tokenNeverExpires ? null : rawValue.tokenDateExpiration,
      tokenNeverExpires: rawValue.tokenNeverExpires,

      // ✅ FIXE: Convertir les limites en nombres
      dailyLimit: this.normalizeNumberValue(rawValue.dailyLimit),
      monthlyLimit: this.normalizeNumberValue(rawValue.monthlyLimit),

      // Supprimer les champs de checkbox qui ne font pas partie du modèle
      allowSms: undefined,
      allowWhatsapp: undefined,
      allowEmail: undefined,
      allowVoice: undefined,
    };

    // Nettoyer les valeurs null/undefined
    Object.keys(application).forEach(key => {
      if (application[key] === '' || application[key] === undefined) {
        // Ne pas convertir les nombres en null si c'est 0
        if (typeof application[key] === 'number' && application[key] === 0) {
          return;
        }
        application[key] = null;
      }
    });

    return application as IApplication | NewApplication;
  }

  resetForm(form: ApplicationFormGroup, application: ApplicationFormGroupInput): void {
    const formDefaults = this.getFormDefaults();
    const extendedDefaults = this.getExtendedDefaults();

    const applicationRawValue = {
      ...formDefaults,
      ...extendedDefaults,
      ...application,
    };

    // Normaliser les valeurs undefined en null
    const normalizedValues = {
      ...applicationRawValue,
      webhookUrl: this.normalizeValue(applicationRawValue.webhookUrl),
      webhookSecret: this.normalizeValue(applicationRawValue.webhookSecret),
      // ✅ FIXE: Convertir en nombres
      dailyLimit: this.normalizeNumberValue(applicationRawValue.dailyLimit),
      monthlyLimit: this.normalizeNumberValue(applicationRawValue.monthlyLimit),
    };

    form.reset({
      ...normalizedValues,
      id: { value: normalizedValues.id, disabled: true },
      // Mettre à jour les checkboxes des services
      allowSms: this.isServiceAllowed(normalizedValues.allowedServices, 'sms'),
      allowWhatsapp: this.isServiceAllowed(normalizedValues.allowedServices, 'whatsapp'),
      allowEmail: this.isServiceAllowed(normalizedValues.allowedServices, 'email'),
      allowVoice: this.isServiceAllowed(normalizedValues.allowedServices, 'voice'),
      // Réinitialiser les champs du token
      tokenDateExpiration: null,
      tokenNeverExpires: false,
    } as any);
  }

  private getFormDefaults(): ApplicationFormDefaults {
    return {
      id: null,
    };
  }

  /**
   * Normalise une valeur qui peut être undefined en null
   */
  private normalizeValue<T>(value: T | undefined): T | null {
    return value === undefined ? null : value;
  }

  /**
   * ✅ NOUVEAU: Normalise une valeur numérique
   */
  private normalizeNumberValue(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const num = typeof value === 'string' ? parseInt(value, 10) : value;
    return isNaN(num) ? null : num;
  }

  /**
   * Obtient les valeurs par défaut pour les nouveaux champs
   */
  private getExtendedDefaults(): {
    environment: string;
    webhookUrl: null;
    webhookSecret: null;
    dailyLimit: null;
    monthlyLimit: null;
    allowedServices: null;
  } {
    return {
      environment: 'development',
      webhookUrl: null,
      webhookSecret: null,
      dailyLimit: null,
      monthlyLimit: null,
      allowedServices: null,
    };
  }

  /**
   * Validateur personnalisé pour éviter les caractères spéciaux
   */
  private noSpecialCharactersValidator = (control: any) => {
    if (!control.value) return null;

    const forbiddenChars = /[<>\"'&]/;
    if (forbiddenChars.test(control.value)) {
      return { forbiddenCharacters: true };
    }
    return null;
  };

  /**
   * Vérifie si un service est autorisé
   */
  private isServiceAllowed(allowedServices: string[] | null | undefined, service: string): boolean {
    return allowedServices ? allowedServices.includes(service) : false;
  }

  /**
   * Valide le nom de l'application
   */
  validateApplicationName(name: string): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (!name || name.trim().length === 0) {
      errors.push('Le nom est obligatoire');
    } else {
      if (name.length < 2) {
        errors.push('Le nom doit contenir au moins 2 caractères');
      }
      if (name.length > 100) {
        errors.push('Le nom ne peut pas dépasser 100 caractères');
      }

      // Vérifier les caractères interdits
      const forbiddenChars = /[<>\"'&]/;
      if (forbiddenChars.test(name)) {
        errors.push('Le nom contient des caractères non autorisés');
      }

      // Vérifier les espaces en début/fin
      if (name !== name.trim()) {
        errors.push('Le nom ne peut pas commencer ou finir par des espaces');
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Valide l'URL du webhook
   */
  validateWebhookUrl(url: string): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (url && url.trim().length > 0) {
      // Vérifier le format URL
      const urlPattern = /^https?:\/\/[^\s/$.?#].[^\s]*$/;
      if (!urlPattern.test(url)) {
        errors.push("L'URL doit être valide et commencer par http:// ou https://");
      }

      // Recommander HTTPS pour la sécurité
      if (url.startsWith('http://')) {
        errors.push("Il est recommandé d'utiliser HTTPS pour la sécurité");
      }

      // Vérifier la longueur
      if (url.length > 2000) {
        errors.push("L'URL ne peut pas dépasser 2000 caractères");
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Valide les limites d'utilisation
   */
  validateUsageLimits(dailyLimit: number | null, monthlyLimit: number | null): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    if (dailyLimit !== null && dailyLimit !== undefined) {
      if (dailyLimit < 1) {
        errors.push("La limite journalière doit être d'au moins 1");
      }
      if (dailyLimit > 1000000) {
        errors.push('La limite journalière ne peut pas dépasser 1,000,000');
      }
    }

    if (monthlyLimit !== null && monthlyLimit !== undefined) {
      if (monthlyLimit < 1) {
        errors.push("La limite mensuelle doit être d'au moins 1");
      }
      if (monthlyLimit > 10000000) {
        errors.push('La limite mensuelle ne peut pas dépasser 10,000,000');
      }
    }

    // Vérifier la cohérence entre les limites
    if (dailyLimit && monthlyLimit && dailyLimit * 30 > monthlyLimit) {
      errors.push('La limite journalière multipliée par 30 ne devrait pas dépasser la limite mensuelle');
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Génère des suggestions de noms d'application
   */
  generateNameSuggestions(environment: string): string[] {
    const suggestions = {
      development: ['Mon App Dev', 'Application Test', 'Projet Développement', 'API Client Dev', 'Test Application'],
      staging: ['App Staging', 'Test Production', 'Application Pré-prod', 'Validation App', 'Staging Client'],
      production: ['Application Principale', 'API Client Prod', 'App Production', 'Client Principal', 'Application Live'],
    };

    return suggestions[environment as keyof typeof suggestions] || suggestions.development;
  }

  /**
   * Obtient les environnements disponibles avec leurs descriptions
   */
  getEnvironmentOptions(): { value: string; label: string; description: string; recommended?: boolean }[] {
    return [
      {
        value: 'development',
        label: 'Développement',
        description: 'Pour les tests et le développement local',
        recommended: true,
      },
      {
        value: 'staging',
        label: 'Test/Staging',
        description: 'Environnement de pré-production pour les tests',
      },
      {
        value: 'production',
        label: 'Production',
        description: 'Environnement de production en live',
      },
    ];
  }

  /**
   * Obtient les services disponibles avec leurs métadonnées
   */
  getServiceOptions(): {
    value: string;
    label: string;
    description: string;
    icon: string;
    popular?: boolean;
  }[] {
    return [
      {
        value: 'sms',
        label: 'SMS',
        description: 'Envoi de messages SMS classiques',
        icon: '💬',
        popular: true,
      },
      {
        value: 'whatsapp',
        label: 'WhatsApp',
        description: 'Messages via WhatsApp Business API',
        icon: '📱',
        popular: true,
      },
      {
        value: 'email',
        label: 'Email',
        description: "Envoi d'emails transactionnels",
        icon: '📧',
      },
      {
        value: 'voice',
        label: 'Messages Vocaux',
        description: 'Appels et messages vocaux automatisés',
        icon: '🎤',
      },
    ];
  }

  /**
   * Calcule les limites recommandées selon l'environnement
   */
  getRecommendedLimits(environment: string): { daily: number; monthly: number } {
    const limits = {
      development: { daily: 100, monthly: 1000 },
      staging: { daily: 500, monthly: 10000 },
      production: { daily: 5000, monthly: 100000 },
    };

    return limits[environment as keyof typeof limits] || limits.development;
  }

  /**
   * Valide l'ensemble du formulaire
   */
  validateForm(formValue: any): { isValid: boolean; errors: { [key: string]: string[] } } {
    const errors: { [key: string]: string[] } = {};

    // Valider le nom
    const nameValidation = this.validateApplicationName(formValue.name);
    if (!nameValidation.isValid) {
      errors['name'] = nameValidation.errors;
    }

    // Valider l'URL webhook si fournie
    if (formValue.webhookUrl) {
      const webhookValidation = this.validateWebhookUrl(formValue.webhookUrl);
      if (!webhookValidation.isValid) {
        errors['webhookUrl'] = webhookValidation.errors;
      }
    }

    // Valider les limites
    const limitsValidation = this.validateUsageLimits(formValue.dailyLimit, formValue.monthlyLimit);
    if (!limitsValidation.isValid) {
      errors['limits'] = limitsValidation.errors;
    }

    // Vérifier qu'au moins un service est sélectionné
    const hasSelectedServices = formValue.allowSms || formValue.allowWhatsapp || formValue.allowEmail || formValue.allowVoice;
    if (!hasSelectedServices) {
      errors['services'] = ['Au moins un service doit être sélectionné'];
    }

    // ✅ VALIDER LA DATE D'EXPIRATION DU TOKEN
    if (!formValue.tokenNeverExpires && !formValue.tokenDateExpiration) {
      errors['tokenDateExpiration'] = ["La date d'expiration est obligatoire si le token expire"];
    }

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  }

  /**
   * Nettoie et formate les données avant soumission
   */
  cleanFormData(formValue: any): any {
    const cleaned = { ...formValue };

    // Nettoyer les chaînes vides
    Object.keys(cleaned).forEach(key => {
      if (typeof cleaned[key] === 'string' && cleaned[key].trim() === '') {
        cleaned[key] = null;
      }
    });

    // Nettoyer le nom
    if (cleaned.name) {
      cleaned.name = cleaned.name.trim();
    }

    // Nettoyer la description
    if (cleaned.description) {
      cleaned.description = cleaned.description.trim();
    }

    // Nettoyer l'URL webhook
    if (cleaned.webhookUrl) {
      cleaned.webhookUrl = cleaned.webhookUrl.trim();
    }

    // ✅ Convertir les limites en nombres
    cleaned.dailyLimit = this.normalizeNumberValue(cleaned.dailyLimit);
    cleaned.monthlyLimit = this.normalizeNumberValue(cleaned.monthlyLimit);

    return cleaned;
  }
}
