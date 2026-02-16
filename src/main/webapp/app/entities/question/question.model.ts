import { ICompany } from 'app/entities/company/company.model';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';

export interface IQuestion {
  id: number;
  qusordre?: number | null;
  qusmessage?: string | null;
  qusenquette?: ICompany | null;
  qustypequestion?: IReferentiel | null;
}

export type NewQuestion = Omit<IQuestion, 'id'> & { id: null };
