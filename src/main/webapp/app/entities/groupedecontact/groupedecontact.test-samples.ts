import { IGroupedecontact, NewGroupedecontact } from './groupedecontact.model';

export const sampleWithRequiredData: IGroupedecontact = {
  id: 18436,
};

export const sampleWithPartialData: IGroupedecontact = {
  id: 16713,
};

export const sampleWithFullData: IGroupedecontact = {
  id: 5829,
};

export const sampleWithNewData: NewGroupedecontact = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
