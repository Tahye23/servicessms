import { IQuestion, NewQuestion } from './question.model';

export const sampleWithRequiredData: IQuestion = {
  id: 11708,
};

export const sampleWithPartialData: IQuestion = {
  id: 4842,
  qusordre: 16406,
  qusmessage: 'solitaire',
};

export const sampleWithFullData: IQuestion = {
  id: 32689,
  qusordre: 32451,
  qusmessage: 'enfin tic-tac pin-pon',
};

export const sampleWithNewData: NewQuestion = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
