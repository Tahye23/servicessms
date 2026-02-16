import { IApi } from 'app/entities/api/api.model';
import { ITokensApp } from 'app/entities/tokens-app/tokens-app.model';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';

export interface IUserTokenApi {
  id: number;
  api?: IApi | null;
  token?: ITokensApp | null;
  user?: IExtendedUser | null;
}

export type NewUserTokenApi = Omit<IUserTokenApi, 'id'> & { id: null };
