import { IUser } from 'app/entities/user/user.model';

export interface IExtendedUser {
  id: number;
  phoneNumber?: string | null;
  address?: string | null;
  city?: string | null;
  country?: string | null;
  postalCode?: string | null;
  companyName?: string | null;
  website?: string | null;
  createdDate?: string | null; // ISO datetime
  updatedDate?: string | null; // ISO datetime

  smsQuota?: number | null;
  whatsappQuota?: number | null;
  smsUsedThisMonth?: number | null;
  whatsappUsedThisMonth?: number | null;
  lastQuotaReset?: string | null; // ISO date YYYY-MM-DD

  timezone?: string | null;
  language?: string | null;
  notificationsEmail?: boolean | null;
  notificationsSms?: boolean | null;
  marketingEmails?: boolean | null;

  apiKey?: string | null;
  apiKeyCreatedDate?: string | null;
  apiKeyLastUsed?: string | null;

  billingAddress?: string | null;
  billingCity?: string | null;
  billingCountry?: string | null;
  billingPostalCode?: string | null;
  taxId?: string | null;
  paymentMethodId?: string | null;

  lastLogin?: string | null;
  loginCount?: number | null;
  totalMessagesSent?: number | null;
  accountCreated?: string | null;
  subscriptionStartDate?: string | null;

  user?: Pick<IUser, 'id' | 'login'> | null;
}

export type NewExtendedUser = Omit<IExtendedUser, 'id'> & { id: null };
