import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IReponse, NewReponse } from '../reponse.model';

export type PartialUpdateReponse = Partial<IReponse> & Pick<IReponse, 'id'>;

export type EntityResponseType = HttpResponse<IReponse>;
export type EntityArrayResponseType = HttpResponse<IReponse[]>;

@Injectable({ providedIn: 'root' })
export class ReponseService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/reponses');

  create(reponse: NewReponse): Observable<EntityResponseType> {
    return this.http.post<IReponse>(this.resourceUrl, reponse, { observe: 'response' });
  }

  update(reponse: IReponse): Observable<EntityResponseType> {
    return this.http.put<IReponse>(`${this.resourceUrl}/${this.getReponseIdentifier(reponse)}`, reponse, { observe: 'response' });
  }

  partialUpdate(reponse: PartialUpdateReponse): Observable<EntityResponseType> {
    return this.http.patch<IReponse>(`${this.resourceUrl}/${this.getReponseIdentifier(reponse)}`, reponse, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IReponse>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IReponse[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getReponseIdentifier(reponse: Pick<IReponse, 'id'>): number {
    return reponse.id;
  }

  compareReponse(o1: Pick<IReponse, 'id'> | null, o2: Pick<IReponse, 'id'> | null): boolean {
    return o1 && o2 ? this.getReponseIdentifier(o1) === this.getReponseIdentifier(o2) : o1 === o2;
  }

  addReponseToCollectionIfMissing<Type extends Pick<IReponse, 'id'>>(
    reponseCollection: Type[],
    ...reponsesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const reponses: Type[] = reponsesToCheck.filter(isPresent);
    if (reponses.length > 0) {
      const reponseCollectionIdentifiers = reponseCollection.map(reponseItem => this.getReponseIdentifier(reponseItem));
      const reponsesToAdd = reponses.filter(reponseItem => {
        const reponseIdentifier = this.getReponseIdentifier(reponseItem);
        if (reponseCollectionIdentifiers.includes(reponseIdentifier)) {
          return false;
        }
        reponseCollectionIdentifiers.push(reponseIdentifier);
        return true;
      });
      return [...reponsesToAdd, ...reponseCollection];
    }
    return reponseCollection;
  }
}
