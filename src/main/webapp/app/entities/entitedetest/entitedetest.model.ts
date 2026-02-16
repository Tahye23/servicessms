import dayjs from 'dayjs/esm';

export interface IEntitedetest {
  id: number;
  identite?: number | null;
  nom?: string | null;
  nombrec?: number | null;
  chamb?: boolean | null;
  champdate?: dayjs.Dayjs | null;
}

export type NewEntitedetest = Omit<IEntitedetest, 'id'> & { id: null };
