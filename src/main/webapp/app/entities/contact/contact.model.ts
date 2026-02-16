import { CountryISO } from 'ngx-intl-tel-input';

export interface IContact {
  id: number;
  conid?: number | null;
  connom?: string | null;
  conprenom?: string | null;
  contelephone?: string | PhoneInput | null;
  statuttraitement?: number | null;
  groupe?: any;
  groupes?: any[] | null;
  user_login?: string | null;
  customFields?: string | null;

  hasWhatsapp?: boolean | null;
  totalSmsSent?: number | null;
  totalSmsSuccess?: number | null;
  totalSmsFailed?: number | null;
  totalWhatsappSent?: number | null;
  totalWhatsappSuccess?: number | null;
  totalWhatsappFailed?: number | null;
  selected?: boolean;
}

export interface PhoneInput {
  number: string;
  nationalNumber: string;
  internationalNumber: string;
  e164Number: string;
  dialCode: string;
  countryCode: CountryISO | null;
}
export interface BulkLinkFiltersPayload {
  nom?: string;
  prenom?: string;
  telephone?: string;

  statut?: number;
  hasWhatsapp?: boolean;

  minSmsSent?: number;
  maxSmsSent?: number;
  minWhatsappSent?: number;
  maxWhatsappSent?: number;
  hasReceivedMessages?: boolean;

  campaignId?: number;
  smsStatus?: string;
  deliveryStatus?: string;
  lastErrorContains?: string;
}

export interface AdvancedContactFilters {
  nom?: string;
  prenom?: string;
  telephone?: string;

  statut?: string;
  hasWhatsapp?: string;

  minSmsSent?: number;
  maxSmsSent?: number;
  minWhatsappSent?: number;
  maxWhatsappSent?: number;
  hasReceivedMessages?: string;

  nomFilterType?: FilterType;
  prenomFilterType?: FilterType;
  telephoneFilterType?: FilterType;
  campaignId?: number | null;
  smsStatus?: string;
  deliveryStatus?: string;
  lastErrorContains?: string;
}

export enum FilterType {
  CONTAINS = 'contains',
  STARTS_WITH = 'startsWith',
  ENDS_WITH = 'endsWith',
  EXACT = 'exact',
  NOT_CONTAINS = 'notContains',
  NOT_STARTS_WITH = 'notStartsWith',
  NOT_ENDS_WITH = 'notEndsWith',
  NOT_EXACT = 'notExact',
}

export interface MessageStatistics {
  totalContacts: number;
  contactsWithWhatsapp: number;
  totalSmsSent: number;
  totalWhatsappSent: number;
  totalSmsSuccess: number;
  totalSmsFailed: number;
  totalWhatsappSuccess: number;
  totalWhatsappFailed: number;
  averageMessagesPerContact: number;
}

export interface ProgressStatus {
  total: number;
  current: number;
  inserted: number;
  percentage: number;
  insertionRate: number;
  estimatedTimeRemaining: number;
  completed: boolean;
}

export interface CustomFieldPayload {
  apiName: string;
  label: string;
  maxLength: number;
}

export interface ImportHistory {
  id: number;
  userLogin: string;
  bulkId: string;
  importDate: string;
  totalLines: number;
  insertedCount: number;
  rejectedCount: number;
  duplicateCount: number;
  status: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export type NewContact = Omit<IContact, 'id'> & { id: null };
