import { IChoix, NewChoix } from './choix.model';

export const sampleWithRequiredData: IChoix = {
  id: 18230,
};

export const sampleWithPartialData: IChoix = {
  id: 7252,
};

export const sampleWithFullData: IChoix = {
  id: 8483,
  chovaleur: 'magnifique patientèle',
};

export const sampleWithNewData: NewChoix = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
