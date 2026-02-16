import { IExtendedUser, NewExtendedUser } from './extended-user.model';

export const sampleWithRequiredData: IExtendedUser = {
  id: 22995,
};

export const sampleWithPartialData: IExtendedUser = {
  id: 22527,
};

export const sampleWithFullData: IExtendedUser = {
  id: 16127,
  address: 'jusqu’à ce que hé circulaire',
};

export const sampleWithNewData: NewExtendedUser = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
