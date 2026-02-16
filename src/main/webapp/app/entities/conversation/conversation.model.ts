import dayjs from 'dayjs/esm';
import { IContact } from 'app/entities/contact/contact.model';
import { IQuestion } from 'app/entities/question/question.model';
import { ICompany } from 'app/entities/company/company.model';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';

export interface IConversation {
  id: number;
  covdatedebut?: dayjs.Dayjs | null;
  covdatefin?: dayjs.Dayjs | null;
  contact?: IContact | null;
  question?: IQuestion | null;
  covenquette?: ICompany | null;
  covstate?: IReferentiel | null;
}

export type NewConversation = Omit<IConversation, 'id'> & { id: null };
