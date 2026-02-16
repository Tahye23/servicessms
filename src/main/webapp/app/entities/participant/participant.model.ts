import { IContact } from 'app/entities/contact/contact.model';
import { ICompany } from 'app/entities/company/company.model';

export interface IParticipant {
  id: number;
  patcontact?: IContact | null;
  patenquette?: ICompany | null;
}

export type NewParticipant = Omit<IParticipant, 'id'> & { id: null };
