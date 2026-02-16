import { IQuestion } from 'app/entities/question/question.model';

export interface IChoix {
  id: number;
  chovaleur?: string | null;
  choquestion?: IQuestion | null;
  choquestionSuivante?: IQuestion | null;
}

export type NewChoix = Omit<IChoix, 'id'> & { id: null };
