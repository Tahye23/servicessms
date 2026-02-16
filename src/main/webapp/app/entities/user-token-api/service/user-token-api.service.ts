import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IUserTokenApi, NewUserTokenApi } from '../user-token-api.model';

export type PartialUpdateUserTokenApi = Partial<IUserTokenApi> & Pick<IUserTokenApi, 'id'>;

export type EntityResponseType = HttpResponse<IUserTokenApi>;
export type EntityArrayResponseType = HttpResponse<IUserTokenApi[]>;

@Injectable({ providedIn: 'root' })
export class UserTokenApiService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/user-token-apis');

  create(userTokenApi: NewUserTokenApi): Observable<EntityResponseType> {
    return this.http.post<IUserTokenApi>(this.resourceUrl, userTokenApi, { observe: 'response' });
  }

  update(userTokenApi: IUserTokenApi): Observable<EntityResponseType> {
    return this.http.put<IUserTokenApi>(`${this.resourceUrl}/${this.getUserTokenApiIdentifier(userTokenApi)}`, userTokenApi, {
      observe: 'response',
    });
  }

  partialUpdate(userTokenApi: PartialUpdateUserTokenApi): Observable<EntityResponseType> {
    return this.http.patch<IUserTokenApi>(`${this.resourceUrl}/${this.getUserTokenApiIdentifier(userTokenApi)}`, userTokenApi, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IUserTokenApi>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IUserTokenApi[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getUserTokenApiIdentifier(userTokenApi: Pick<IUserTokenApi, 'id'>): number {
    return userTokenApi.id;
  }

  compareUserTokenApi(o1: Pick<IUserTokenApi, 'id'> | null, o2: Pick<IUserTokenApi, 'id'> | null): boolean {
    return o1 && o2 ? this.getUserTokenApiIdentifier(o1) === this.getUserTokenApiIdentifier(o2) : o1 === o2;
  }

  addUserTokenApiToCollectionIfMissing<Type extends Pick<IUserTokenApi, 'id'>>(
    userTokenApiCollection: Type[],
    ...userTokenApisToCheck: (Type | null | undefined)[]
  ): Type[] {
    const userTokenApis: Type[] = userTokenApisToCheck.filter(isPresent);
    if (userTokenApis.length > 0) {
      const userTokenApiCollectionIdentifiers = userTokenApiCollection.map(userTokenApiItem =>
        this.getUserTokenApiIdentifier(userTokenApiItem),
      );
      const userTokenApisToAdd = userTokenApis.filter(userTokenApiItem => {
        const userTokenApiIdentifier = this.getUserTokenApiIdentifier(userTokenApiItem);
        if (userTokenApiCollectionIdentifiers.includes(userTokenApiIdentifier)) {
          return false;
        }
        userTokenApiCollectionIdentifiers.push(userTokenApiIdentifier);
        return true;
      });
      return [...userTokenApisToAdd, ...userTokenApiCollection];
    }
    return userTokenApiCollection;
  }
}
