import { IQuestion } from 'app/entities/question/question.model';
import { IContact } from 'app/entities/contact/contact.model';

export interface IReponse {
  id: number;
  repvaleur?: string | null;
  repquestion?: IQuestion | null;
  repcontact?: IContact | null;
}

export type NewReponse = Omit<IReponse, 'id'> & { id: null };
