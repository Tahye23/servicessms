import { IApplication, NewApplication } from './application.model';

export const sampleWithRequiredData: IApplication = {
  id: 7147,
};

export const sampleWithPartialData: IApplication = {
  id: 678,
};

export const sampleWithFullData: IApplication = {
  id: 22411,
  name: 'pin-pon à cause de badaboum',
  description: 'concentrer ruiner vaste',
  userId: 6110,
};

export const sampleWithNewData: NewApplication = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
