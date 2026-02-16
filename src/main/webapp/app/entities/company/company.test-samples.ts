import dayjs from 'dayjs/esm';

import { ICompany, NewCompany } from './company.model';

export const sampleWithRequiredData: ICompany = {
  id: 29751,
};

export const sampleWithPartialData: ICompany = {
  id: 4272,
  camispub: true,
};

export const sampleWithFullData: ICompany = {
  id: 21074,
  name: 'direction',
  activity: 'camarade beaucoup',
  camtitre: 'fleurir',
  camdatecreation: dayjs('2024-03-15'),
  camdatefin: dayjs('2024-03-15T03:34'),
  camispub: false,
};

export const sampleWithNewData: NewCompany = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
