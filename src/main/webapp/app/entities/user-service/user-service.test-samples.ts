import { IUserService, NewUserService } from './user-service.model';

export const sampleWithRequiredData: IUserService = {
  id: 20115,
};

export const sampleWithPartialData: IUserService = {
  id: 26646,
  urSService: 'chut athlète',
  urSUser: 'concernant blême',
};

export const sampleWithFullData: IUserService = {
  id: 11331,
  urSService: 'engloutir hier',
  urSUser: 'police en dedans de',
};

export const sampleWithNewData: NewUserService = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
