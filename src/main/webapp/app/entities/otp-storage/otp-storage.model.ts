import dayjs from 'dayjs/esm';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';

export interface IOTPStorage {
  id: number;
  otsOTP?: string | null;
  phoneNumber?: string | null;
  otsdateexpir?: dayjs.Dayjs | null;
  isOtpUsed?: boolean | null;
  isExpired?: boolean | null;
  user?: IExtendedUser | null;
}

export type NewOTPStorage = Omit<IOTPStorage, 'id'> & { id: null };
