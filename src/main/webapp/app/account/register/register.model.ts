// Interface pour l'inscription d'un utilisateur
export interface Registration {
  firstName: string;
  lastName: string;
  login: string;
  email: string;
  password: string;
  langKey: string;
  planType?: string;
  authorities?: string[];
}

// Interface pour la réponse après inscription
export interface RegistrationResponse {
  id: number;
  login: string;
  email: string;
  firstName: string;
  lastName: string;
  activated: boolean;
  planType: string;
  createdDate: string;
  authorities: string[];
}

// Interface pour les données de vérification
export interface VerificationData {
  email: string;
  token?: string;
  sent?: boolean;
  activated?: boolean;
}

// Interface pour la vérification de disponibilité
export interface AvailabilityCheck {
  available: boolean;
  message?: string;
}

// Enum pour les types de plans
export enum PlanType {
  FREE = 'FREE',
  BASIC = 'BASIC',
  PREMIUM = 'PREMIUM',
  ENTERPRISE = 'ENTERPRISE',
}

// Interface pour les informations de plan
export interface PlanInfo {
  type: PlanType;
  name: string;
  description: string;
  price: number;
  features: string[];
  maxUsers?: number;
  maxProjects?: number;
  support: string;
  popular?: boolean;
}

// Interface pour les erreurs d'inscription
export interface RegistrationError {
  type: string;
  field?: string;
  message: string;
  code?: number;
}

// Interface pour les résultats de validation
export interface ValidationResult {
  valid: boolean;
  message?: string;
  strength?: number;
}

// Classe pour la gestion de l'inscription
export class RegistrationRequest {
  constructor(
    public firstName: string,
    public lastName: string,
    public login: string,
    public email: string,
    public password: string,
    public langKey: string,
    public planType: string = PlanType.FREE,
    public authorities: string[] = ['ROLE_USER'],
  ) {}

  // Méthode pour valider les données
  validate(): string[] {
    const validationErrors: string[] = [];

    if (!this.firstName || this.firstName.length < 2) {
      validationErrors.push('Le prénom doit contenir au moins 2 caractères');
    }

    if (!this.lastName || this.lastName.length < 2) {
      validationErrors.push('Le nom doit contenir au moins 2 caractères');
    }

    if (!this.login || this.login.length < 3) {
      validationErrors.push("Le nom d'utilisateur doit contenir au moins 3 caractères");
    }

    if (!this.email || !this.isValidEmail(this.email)) {
      validationErrors.push("L'adresse email n'est pas valide");
    }

    if (!this.password || this.password.length < 8) {
      validationErrors.push('Le mot de passe doit contenir au moins 8 caractères');
    }

    return validationErrors;
  }

  // Méthode pour valider l'email
  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  // Méthode pour vérifier la force du mot de passe
  getPasswordStrength(): number {
    let strength = 0;

    if (this.password.length >= 8) strength++;
    if (/[a-z]/.test(this.password)) strength++;
    if (/[A-Z]/.test(this.password)) strength++;
    if (/[0-9]/.test(this.password)) strength++;
    if (/[^a-zA-Z0-9]/.test(this.password)) strength++;

    return strength;
  }

  // Méthode pour obtenir les données au format JSON
  toJSON(): Registration {
    return {
      firstName: this.firstName,
      lastName: this.lastName,
      login: this.login,
      email: this.email,
      password: this.password,
      langKey: this.langKey,
      planType: this.planType,
      authorities: this.authorities,
    };
  }
}

// Utilitaires pour la validation
export class ValidationUtils {
  // Validation du nom d'utilisateur
  static validateUsername(username: string): ValidationResult {
    if (!username) {
      return { valid: false, message: "Le nom d'utilisateur est requis" };
    }

    if (username.length < 3) {
      return { valid: false, message: 'Minimum 3 caractères requis' };
    }

    if (username.length > 50) {
      return { valid: false, message: 'Maximum 50 caractères autorisés' };
    }

    const usernameRegex = /^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$/;
    if (!usernameRegex.test(username)) {
      return { valid: false, message: "Format de nom d'utilisateur invalide" };
    }

    return { valid: true };
  }

  // Validation de l'email
  static validateEmail(email: string): ValidationResult {
    if (!email) {
      return { valid: false, message: "L'email est requis" };
    }

    if (email.length < 5) {
      return { valid: false, message: 'Minimum 5 caractères requis' };
    }

    if (email.length > 254) {
      return { valid: false, message: 'Maximum 254 caractères autorisés' };
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return { valid: false, message: "Format d'email invalide" };
    }

    return { valid: true };
  }

  // Validation du mot de passe
  static validatePassword(password: string): ValidationResult & { strength: number } {
    if (!password) {
      return { valid: false, message: 'Le mot de passe est requis', strength: 0 };
    }

    if (password.length < 8) {
      return { valid: false, message: 'Minimum 8 caractères requis', strength: 0 };
    }

    if (password.length > 50) {
      return { valid: false, message: 'Maximum 50 caractères autorisés', strength: 0 };
    }

    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^a-zA-Z0-9]/.test(password)) strength++;

    return { valid: true, strength };
  }

  // Validation de la confirmation du mot de passe
  static validatePasswordConfirmation(password: string, confirmPassword: string): ValidationResult {
    if (!confirmPassword) {
      return { valid: false, message: 'La confirmation du mot de passe est requise' };
    }

    if (password !== confirmPassword) {
      return { valid: false, message: 'Les mots de passe ne correspondent pas' };
    }

    return { valid: true };
  }

  // Validation du nom/prénom
  static validateName(name: string, fieldName: string): ValidationResult {
    if (!name) {
      return { valid: false, message: `Le ${fieldName} est requis` };
    }

    if (name.length < 2) {
      return { valid: false, message: `Le ${fieldName} doit contenir au moins 2 caractères` };
    }

    if (name.length > 50) {
      return { valid: false, message: `Le ${fieldName} ne peut pas dépasser 50 caractères` };
    }

    const nameRegex = /^[a-zA-ZÀ-ÿ\s'-]+$/;
    if (!nameRegex.test(name)) {
      return { valid: false, message: `Le ${fieldName} contient des caractères invalides` };
    }

    return { valid: true };
  }
}

// Constantes pour les plans
export const PLAN_FEATURES: Record<PlanType, PlanInfo> = {
  [PlanType.FREE]: {
    type: PlanType.FREE,
    name: 'Plan Gratuit',
    description: 'Parfait pour commencer',
    price: 0,
    features: [
      'Accès aux fonctionnalités de base',
      'Support communautaire',
      'Stockage limité à 1GB',
      "Jusqu'à 3 projets",
      'Export en format basique',
    ],
    maxUsers: 1,
    maxProjects: 3,
    support: 'Communautaire',
    popular: false,
  },
  [PlanType.BASIC]: {
    type: PlanType.BASIC,
    name: 'Plan Basic',
    description: 'Pour les petites équipes',
    price: 9.99,
    features: [
      'Toutes les fonctionnalités du plan gratuit',
      'Support par email',
      'Stockage de 50GB',
      'Projets illimités',
      'Export avancé',
      'Intégrations basiques',
    ],
    maxUsers: 5,
    maxProjects: -1, // illimité
    support: 'Email',
    popular: true,
  },
  [PlanType.PREMIUM]: {
    type: PlanType.PREMIUM,
    name: 'Plan Premium',
    description: 'Pour les équipes en croissance',
    price: 29.99,
    features: [
      'Toutes les fonctionnalités du plan Basic',
      'Support prioritaire',
      'Stockage de 500GB',
      'Fonctionnalités avancées',
      'API complète',
      'Intégrations premium',
      'Analyses détaillées',
    ],
    maxUsers: 20,
    maxProjects: -1,
    support: 'Prioritaire',
    popular: false,
  },
  [PlanType.ENTERPRISE]: {
    type: PlanType.ENTERPRISE,
    name: 'Plan Enterprise',
    description: 'Pour les grandes organisations',
    price: 99.99,
    features: [
      'Toutes les fonctionnalités du plan Premium',
      'Support dédié 24/7',
      'Stockage illimité',
      'Personnalisation complète',
      'Sécurité avancée',
      'Formation dédiée',
      'SLA garanti',
    ],
    maxUsers: -1, // illimité
    maxProjects: -1,
    support: 'Dédié 24/7',
    popular: false,
  },
};

// Types d'erreurs communes
export const REGISTRATION_ERROR_TYPES = {
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  EMAIL_ALREADY_EXISTS: 'EMAIL_ALREADY_EXISTS',
  USERNAME_ALREADY_EXISTS: 'USERNAME_ALREADY_EXISTS',
  WEAK_PASSWORD: 'WEAK_PASSWORD',
  INVALID_EMAIL: 'INVALID_EMAIL',
  NETWORK_ERROR: 'NETWORK_ERROR',
  SERVER_ERROR: 'SERVER_ERROR',
} as const;

export type RegistrationErrorType = keyof typeof REGISTRATION_ERROR_TYPES;

// Utilitaires pour les messages d'erreur
export const ERROR_MESSAGES: Record<RegistrationErrorType, string> = {
  VALIDATION_ERROR: 'Erreur de validation des données',
  EMAIL_ALREADY_EXISTS: 'Cette adresse email est déjà utilisée',
  USERNAME_ALREADY_EXISTS: "Ce nom d'utilisateur est déjà pris",
  WEAK_PASSWORD: 'Le mot de passe est trop faible',
  INVALID_EMAIL: "Format d'email invalide",
  NETWORK_ERROR: 'Erreur de connexion réseau',
  SERVER_ERROR: 'Erreur serveur temporaire',
};
