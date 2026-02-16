import dayjs from 'dayjs/esm';

export interface IPlanabonnement {
  id: number;
  abpName?: string | null;
  abpDescription?: string | null;
  abpPrice?: number | null;
  abpCurrency?: string | null;
  abpPeriod?: string | null;
  abpFeatures?: string | null;
  abpButtonText?: string | null;
  buttonClass?: string | null;
  abpPopular?: boolean | null;
  createdDate?: dayjs.Dayjs | null;
  updatedDate?: dayjs.Dayjs | null;
  active?: boolean | null;
  customPlan?: boolean | null;
  // Nouveaux champs pour le système d'abonnement
  planType?: string | null; // 'FREE' | 'SMS' | 'WHATSAPP' | 'PREMIUM' | 'ENTERPRISE'
  smsLimit?: number | null;
  whatsappLimit?: number | null;
  usersLimit?: number | null;
  templatesLimit?: number | null;
  canManageUsers?: boolean | null;
  canManageTemplates?: boolean | null;
  canViewDashboard?: boolean | null;
  canManageAPI?: boolean | null;
  canViewConversations?: boolean | null;
  canViewAnalytics?: boolean | null;
  prioritySupport?: boolean | null;
  maxApiCallsPerDay?: number | null;
  storageLimitMb?: number | null;
  sortOrder?: number | null;
}

export type NewPlanabonnement = Omit<IPlanabonnement, 'id'> & { id: null };

// Énumérations pour les types
export enum PlanType {
  FREE = 'FREE',
  SMS = 'SMS',
  WHATSAPP = 'WHATSAPP',
  PREMIUM = 'PREMIUM',
  ENTERPRISE = 'ENTERPRISE',
}

export enum PlanPeriod {
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
  LIFETIME = 'LIFETIME',
}

// Interface pour l'affichage des fonctionnalités
export interface IPlanFeature {
  name: string;
  included: boolean;
  description?: string;
}

// Interface pour les statistiques d'usage d'un plan
export interface IPlanUsage {
  planId: number;
  smsUsed: number;
  whatsappUsed: number;
  apiCallsUsed: number;
  storageUsed: number;
  usersCount: number;
  templatesCount: number;
}
