import { IUserTokenApi, NewUserTokenApi } from './user-token-api.model';

export const sampleWithRequiredData: IUserTokenApi = {
  id: 8637,
};

export const sampleWithPartialData: IUserTokenApi = {
  id: 23679,
};

export const sampleWithFullData: IUserTokenApi = {
  id: 22961,
};

export const sampleWithNewData: NewUserTokenApi = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
