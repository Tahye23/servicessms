import { IUser } from 'app/entities/user/user.model';
import { IPlanabonnement } from '../planabonnement/planabonnement.model';
import { IExtendedUser } from '../extended-user/extended-user.model';

export enum SubscriptionStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  SUSPENDED = 'SUSPENDED',
  CANCELLED = 'CANCELLED',
  TRIAL = 'TRIAL',
  PENDING_PAYMENT = 'PENDING_PAYMENT',
}

export interface IAbonnement {
  id: number;
  user?: IExtendedUser | null;
  userId?: number | null;
  plan?: IPlanabonnement | null;
  planId?: number | null;
  status?: SubscriptionStatus | null;
  startDate?: string | null;
  endDate?: string | null;
  createdDate?: string | null;
  updatedDate?: string | null;

  smsUsed?: number | null;
  whatsappUsed?: number | null;
  apiCallsToday?: number | null;
  storageUsedMb?: number | null;
  lastApiCallDate?: string | null;

  paymentMethod?: string | null;
  transactionId?: string | null;
  autoRenew?: boolean | null;

  isTrial?: boolean | null;
  trialEndDate?: string | null;

  isCustomPlan?: boolean | null;
  customPrice?: number | null;
  customPeriod?: string | null;
  customName?: string | null;
  customDescription?: string | null;

  customSmsLimit?: number | null;
  customWhatsappLimit?: number | null;
  customUsersLimit?: number | null;
  customTemplatesLimit?: number | null;
  customApiCallsLimit?: number | null;
  customStorageLimitMb?: number | null;

  customCanManageUsers?: boolean | null;
  customCanManageTemplates?: boolean | null;
  customCanViewConversations?: boolean | null;
  customCanViewAnalytics?: boolean | null;
  customPrioritySupport?: boolean | null;

  bonusSmsEnabled?: boolean | null;
  bonusSmsAmount?: number | null;
  bonusWhatsappEnabled?: boolean | null;
  bonusWhatsappAmount?: number | null;

  allowSmsCarryover?: boolean | null;
  allowWhatsappCarryover?: boolean | null;
  carriedOverSms?: number | null;
  carriedOverWhatsapp?: number | null;

  active?: boolean | null;

  canViewDashboard?: boolean | null;
  canManageAPI?: boolean | null;
  apiCallLimitPerDay?: number | null;
  apiCallsUsedToday?: number | null;
  apiAccessToken?: string | null;
  apiAccessEnabled?: boolean | null;
}

export type NewAbonnement = Omit<IAbonnement, 'id'> & { id: null };
