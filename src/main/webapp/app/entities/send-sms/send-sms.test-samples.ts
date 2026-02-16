import dayjs from 'dayjs/esm';

import { ISendSms, NewSendSms } from './send-sms.model';

export const sampleWithRequiredData: ISendSms = {
  id: 31697,
};

export const sampleWithPartialData: ISendSms = {
  id: 23854,
  sendateEnvoi: dayjs('2024-03-04T23:29'),
  isbulk: true,
  Titre: 'via',
};

export const sampleWithFullData: ISendSms = {
  id: 3174,
  sender: 'lors de triathlète pacifique',
  receiver: 'retenir',
  msgdata: 'au cas où groin groin depuis',
  sendateEnvoi: dayjs('2024-03-04T14:50'),
  dialogue: 'étendre gai',
  isSent: false,
  isbulk: true,
  Titre: 'parce que visiter',
};

export const sampleWithNewData: NewSendSms = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
