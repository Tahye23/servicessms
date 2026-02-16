import dayjs from 'dayjs/esm';
import { IApplication } from 'app/entities/application/application.model';

export interface ITokensApp {
  id: number;
  dateExpiration?: dayjs.Dayjs | null;
  token?: string | null;
  isExpired?: boolean | null;
  active?: boolean | null;
  createdAt?: dayjs.Dayjs | null;
  lastUsedAt?: dayjs.Dayjs | null;
  application?: IApplication | null;
}

export type NewTokensApp = Omit<ITokensApp, 'id'> & { id: null };

export interface TokenStats {
  total: number;
  active: number;
  expired: number;
  expiringSoon: number;
}

export interface TokenValidationResult {
  valid: boolean;
  expiresAt?: string;
  reason?: string;
}
