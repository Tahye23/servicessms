import dayjs from 'dayjs/esm';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';

export interface ICompany {
  id: number;
  name?: string | null;
  activity?: string | null;
  camtitre?: string | null;
  camdatecreation?: dayjs.Dayjs | null;
  camdatefin?: dayjs.Dayjs | null;
  camispub?: boolean | null;
  camUser?: IExtendedUser | null;
  camstatus?: IReferentiel | null;
}

export type NewCompany = Omit<ICompany, 'id'> & { id: null };
