import { IParticipant, NewParticipant } from './participant.model';

export const sampleWithRequiredData: IParticipant = {
  id: 24241,
};

export const sampleWithPartialData: IParticipant = {
  id: 14979,
};

export const sampleWithFullData: IParticipant = {
  id: 23359,
};

export const sampleWithNewData: NewParticipant = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
