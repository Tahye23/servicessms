import { IApi } from 'app/entities/api/api.model';
import { IPlanabonnement } from 'app/entities/planabonnement/planabonnement.model';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';

/**
 * Modèle pour un Token d'Application
 */
export interface ITokensApp {
  id?: number;
  token?: string | null;
  dateExpiration?: string | null;
  isExpired?: boolean | null;
  active?: boolean | null;
  createdAt?: string | null;
  lastUsedAt?: string | null;
  userLogin?: string | null;
  application?: IApplication | null;
}

/**
 * Modèle pour une Application
 */
export interface IApplication {
  id: number;
  name?: string | null;
  description?: string | null;
  userId?: number | null;

  //   API MANAGEMENT
  //  SUPPRIMÉ: apiToken - Utiliser TokensApp à la place
  //  NOUVEAU: tokens - Collection de TokensApp
  tokens?: ITokensApp[] | null;

  webhookUrl?: string | null;
  webhookSecret?: string | null;
  isActive?: boolean | null;
  environment?: 'development' | 'staging' | 'production' | null;

  //   STATISTIQUES D'UTILISATION
  totalApiCalls?: number | null;
  lastApiCall?: string | null;

  //   PERMISSIONS ET LIMITES
  allowedServices?: string[] | null; // ['sms', 'whatsapp', 'email']
  dailyLimit?: number | null;
  monthlyLimit?: number | null;
  currentDailyUsage?: number | null;
  currentMonthlyUsage?: number | null;
  tokenDateExpiration?: string | null;
  tokenNeverExpires?: boolean | null;

  //   MÉTADONNÉES
  createdAt?: string | null;
  updatedAt?: string | null;

  //   RELATIONS EXISTANTES
  api?: IApi | null;
  planabonnement?: IPlanabonnement | null;
  utilisateur?: IExtendedUser | null;
}

export type NewApplication = Omit<IApplication, 'id'> & { id: null };

/**
 * Statistiques des applications
 */
export interface IApplicationStats {
  totalApplications: number;
  activeApplications: number;
  todayApiCalls: number;
  monthlyApiCalls: number;
  topServices: { service: string; calls: number }[];
}

/**
 * Réponse de Token d'API
 */
export interface ITokenResponse {
  id?: number;
  token: string;
  dateExpiration: string;
  active: boolean;
  createdAt: string;
}

/**
 * Réponse pour créer/régénérer un token
 */
export interface ITokenCreateResponse {
  id: number;
  token: string;
  dateExpiration: string;
  active: boolean;
  createdAt: string;
  lastUsedAt: string | null;
}

/**
 * Statistiques des tokens pour une application
 */
export interface ITokenStats {
  total: number;
  active: number;
  inactive: number;
  expired: number;
}

/**
 * Requête pour valider un token
 */
export interface ITokenValidationRequest {
  token: string;
}

/**
 * Réponse de validation de token
 */
export interface ITokenValidationResponse {
  valid: boolean;
  applicationId?: number;
  applicationName?: string;
  message?: string;
  reason?: string;
  expiresAt?: string;
}

/**
 * Requête pour créer un token avec expiration personnalisée
 */
export interface ICreateTokenRequest {
  applicationId: number;
  dateExpiration: string;
  active?: boolean;
}

/**
 * Test de Webhook
 */
export interface IWebhookTest {
  url: string;
  secret?: string;
  testPayload: any;
  response?: {
    status: number;
    body: string;
    responseTime: number;
  };
}
