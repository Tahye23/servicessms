import { IApi, NewApi } from './api.model';

export const sampleWithRequiredData: IApi = {
  id: 30125,
};

export const sampleWithPartialData: IApi = {
  id: 25057,
  apiUrl: 'ici jusqu’à ce que',
  apiVersion: 25214,
};

export const sampleWithFullData: IApi = {
  id: 30722,
  apiNom: 'insulter sombre ouah',
  apiUrl: 'parmi sauf à',
  apiVersion: 3536,
};

export const sampleWithNewData: NewApi = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
