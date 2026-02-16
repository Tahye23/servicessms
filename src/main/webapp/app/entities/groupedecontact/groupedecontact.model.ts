import { IGroupe } from 'app/entities/groupe/groupe.model';
import { IContact } from 'app/entities/contact/contact.model';

export interface IGroupedecontact {
  id: number;
  cgrgroupe?: IGroupe | null;
  contact?: IContact | null;
}

export type NewGroupedecontact = Omit<IGroupedecontact, 'id'> & { id: null };
