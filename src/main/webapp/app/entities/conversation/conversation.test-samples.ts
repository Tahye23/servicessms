import dayjs from 'dayjs/esm';

import { IConversation, NewConversation } from './conversation.model';

export const sampleWithRequiredData: IConversation = {
  id: 25769,
};

export const sampleWithPartialData: IConversation = {
  id: 32116,
  covdatedebut: dayjs('2024-07-21T23:29'),
};

export const sampleWithFullData: IConversation = {
  id: 9753,
  covdatedebut: dayjs('2024-07-21T15:16'),
  covdatefin: dayjs('2024-07-22T04:03'),
};

export const sampleWithNewData: NewConversation = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
