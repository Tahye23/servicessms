import { IService } from 'app/entities/service/service.model';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';

export interface IUserService {
  id: number;
  urSService?: string | null;
  urSUser?: string | null;
  service?: IService | null;
  user?: IExtendedUser | null;
}

export type NewUserService = Omit<IUserService, 'id'> & { id: null };
