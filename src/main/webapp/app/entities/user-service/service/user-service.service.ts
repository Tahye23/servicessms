import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IUserService, NewUserService } from '../user-service.model';

export type PartialUpdateUserService = Partial<IUserService> & Pick<IUserService, 'id'>;

export type EntityResponseType = HttpResponse<IUserService>;
export type EntityArrayResponseType = HttpResponse<IUserService[]>;

@Injectable({ providedIn: 'root' })
export class UserServiceService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/user-services');

  create(userService: NewUserService): Observable<EntityResponseType> {
    return this.http.post<IUserService>(this.resourceUrl, userService, { observe: 'response' });
  }

  update(userService: IUserService): Observable<EntityResponseType> {
    return this.http.put<IUserService>(`${this.resourceUrl}/${this.getUserServiceIdentifier(userService)}`, userService, {
      observe: 'response',
    });
  }

  partialUpdate(userService: PartialUpdateUserService): Observable<EntityResponseType> {
    return this.http.patch<IUserService>(`${this.resourceUrl}/${this.getUserServiceIdentifier(userService)}`, userService, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IUserService>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IUserService[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getUserServiceIdentifier(userService: Pick<IUserService, 'id'>): number {
    return userService.id;
  }

  compareUserService(o1: Pick<IUserService, 'id'> | null, o2: Pick<IUserService, 'id'> | null): boolean {
    return o1 && o2 ? this.getUserServiceIdentifier(o1) === this.getUserServiceIdentifier(o2) : o1 === o2;
  }

  addUserServiceToCollectionIfMissing<Type extends Pick<IUserService, 'id'>>(
    userServiceCollection: Type[],
    ...userServicesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const userServices: Type[] = userServicesToCheck.filter(isPresent);
    if (userServices.length > 0) {
      const userServiceCollectionIdentifiers = userServiceCollection.map(userServiceItem => this.getUserServiceIdentifier(userServiceItem));
      const userServicesToAdd = userServices.filter(userServiceItem => {
        const userServiceIdentifier = this.getUserServiceIdentifier(userServiceItem);
        if (userServiceCollectionIdentifiers.includes(userServiceIdentifier)) {
          return false;
        }
        userServiceCollectionIdentifiers.push(userServiceIdentifier);
        return true;
      });
      return [...userServicesToAdd, ...userServiceCollection];
    }
    return userServiceCollection;
  }
}
