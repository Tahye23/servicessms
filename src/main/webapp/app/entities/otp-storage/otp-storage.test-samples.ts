import dayjs from 'dayjs/esm';

import { IOTPStorage, NewOTPStorage } from './otp-storage.model';

export const sampleWithRequiredData: IOTPStorage = {
  id: 20415,
};

export const sampleWithPartialData: IOTPStorage = {
  id: 21007,
  otsOTP: 'après décider',
  isOtpUsed: true,
};

export const sampleWithFullData: IOTPStorage = {
  id: 29378,
  otsOTP: 'fonctionnaire bè prou',
  phoneNumber: 'certainement',
  otsdateexpir: dayjs('2024-07-21T23:41'),
  isOtpUsed: false,
  isExpired: true,
};

export const sampleWithNewData: NewOTPStorage = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
