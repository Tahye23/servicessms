import { IReponse, NewReponse } from './reponse.model';

export const sampleWithRequiredData: IReponse = {
  id: 20068,
};

export const sampleWithPartialData: IReponse = {
  id: 30395,
};

export const sampleWithFullData: IReponse = {
  id: 18195,
  repvaleur: 'oui aïe tchou tchouu',
};

export const sampleWithNewData: NewReponse = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
