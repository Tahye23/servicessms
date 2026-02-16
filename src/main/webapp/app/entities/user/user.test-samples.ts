import { IUser } from './user.model';

export const sampleWithRequiredData: IUser = {
  id: 28423,
  login: 'W.@GK\\*XuFIr\\ubRw0tL\\r0',
};

export const sampleWithPartialData: IUser = {
  id: 25339,
  login: 'CLo',
};

export const sampleWithFullData: IUser = {
  id: 29646,
  login: 'Aa9a',
};
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
