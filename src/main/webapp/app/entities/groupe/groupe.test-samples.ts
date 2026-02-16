import { IGroupe, NewGroupe } from './groupe.model';

export const sampleWithRequiredData: IGroupe = {
  id: 19132,
};

export const sampleWithPartialData: IGroupe = {
  id: 526,
  grotitre: 'lorsque clac',
  user: 'pour que équipe hebdomadaire',
};

export const sampleWithFullData: IGroupe = {
  id: 6105,
  grotitre: 'juriste blablabla même si',
  user: 'de manière à ce que',
};

export const sampleWithNewData: NewGroupe = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
