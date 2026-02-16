import { IContact, NewContact } from './contact.model';

export const sampleWithRequiredData: IContact = {
  id: 12853,
};

export const sampleWithPartialData: IContact = {
  id: 27736,
  conid: 4581,
};

export const sampleWithFullData: IContact = {
  id: 9241,
  conid: 6893,
  connom: 'émérite parce que hors de',
  conprenom: 'depuis',
  contelephone: 'en outre de',
  statuttraitement: 17687,
};

export const sampleWithNewData: NewContact = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
