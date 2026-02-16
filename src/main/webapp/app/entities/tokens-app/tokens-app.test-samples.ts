import dayjs from 'dayjs/esm';

import { ITokensApp, NewTokensApp } from './tokens-app.model';

export const sampleWithRequiredData: ITokensApp = {
  id: 17364,
};

export const sampleWithPartialData: ITokensApp = {
  id: 2673,
  token: 'dense étudier compter',
};

export const sampleWithFullData: ITokensApp = {
  id: 22107,
  dateExpiration: dayjs('2024-03-05T19:21'),
  token: 'pendant que',
};

export const sampleWithNewData: NewTokensApp = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
