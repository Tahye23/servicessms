import { IExtendedUser } from '../extended-user/extended-user.model';
import { FilterType } from '../contact/contact.model';

export interface IGroupe {
  id: number;
  grotitre?: string | null;
  user?: string | null;
  extendedUser?: IExtendedUser | null;
  groupType?: 'production' | 'test' | 'whatsapp' | null;
}

export type NewGroupe = Omit<IGroupe, 'id'> & { id: null };

export interface ICampaignSummary {
  id: number;
  name: string;
}

export interface AdvancedFiltersPayload {
  search?: string | null;

  // Filtres texte simples
  nom?: string | null;
  prenom?: string | null;
  telephone?: string | null;

  // Filtres de statut
  statut?: number | null; // Integer côté back
  hasWhatsapp?: boolean | null; // Boolean côté back

  // Compteurs
  minSmsSent?: number | null;
  maxSmsSent?: number | null;
  minWhatsappSent?: number | null;
  maxWhatsappSent?: number | null;

  // Filtres booléens messages
  hasReceivedMessages?: boolean | null; // Boolean côté back

  // Filtres texte avancés (types d'opérateurs)
  nomFilterType?: FilterType; // Enum côté front
  prenomFilterType?: FilterType;
  telephoneFilterType?: FilterType;

  // Campagnes / Statuts SMS
  campaignId?: number | null; // Long côté back
  smsStatus?: string | null;
  deliveryStatus?: string | null;
  lastErrorContains?: string | null;
}
