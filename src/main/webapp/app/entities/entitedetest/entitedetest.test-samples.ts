import dayjs from 'dayjs/esm';

import { IEntitedetest, NewEntitedetest } from './entitedetest.model';

export const sampleWithRequiredData: IEntitedetest = {
  id: 29524,
};

export const sampleWithPartialData: IEntitedetest = {
  id: 21438,
  chamb: false,
  champdate: dayjs('2024-01-09T00:51'),
};

export const sampleWithFullData: IEntitedetest = {
  id: 11058,
  identite: 30718,
  nom: 'partenaire fidèle multiple',
  nombrec: 31097,
  chamb: false,
  champdate: dayjs('2024-01-09T08:54'),
};

export const sampleWithNewData: NewEntitedetest = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
