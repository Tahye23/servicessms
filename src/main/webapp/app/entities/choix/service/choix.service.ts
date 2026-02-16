import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IChoix, NewChoix } from '../choix.model';

export type PartialUpdateChoix = Partial<IChoix> & Pick<IChoix, 'id'>;

export type EntityResponseType = HttpResponse<IChoix>;
export type EntityArrayResponseType = HttpResponse<IChoix[]>;

@Injectable({ providedIn: 'root' })
export class ChoixService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/choixes');

  create(choix: NewChoix): Observable<EntityResponseType> {
    return this.http.post<IChoix>(this.resourceUrl, choix, { observe: 'response' });
  }

  update(choix: IChoix): Observable<EntityResponseType> {
    return this.http.put<IChoix>(`${this.resourceUrl}/${this.getChoixIdentifier(choix)}`, choix, { observe: 'response' });
  }

  partialUpdate(choix: PartialUpdateChoix): Observable<EntityResponseType> {
    return this.http.patch<IChoix>(`${this.resourceUrl}/${this.getChoixIdentifier(choix)}`, choix, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IChoix>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IChoix[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getChoixIdentifier(choix: Pick<IChoix, 'id'>): number {
    return choix.id;
  }

  compareChoix(o1: Pick<IChoix, 'id'> | null, o2: Pick<IChoix, 'id'> | null): boolean {
    return o1 && o2 ? this.getChoixIdentifier(o1) === this.getChoixIdentifier(o2) : o1 === o2;
  }

  addChoixToCollectionIfMissing<Type extends Pick<IChoix, 'id'>>(
    choixCollection: Type[],
    ...choixesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const choixes: Type[] = choixesToCheck.filter(isPresent);
    if (choixes.length > 0) {
      const choixCollectionIdentifiers = choixCollection.map(choixItem => this.getChoixIdentifier(choixItem));
      const choixesToAdd = choixes.filter(choixItem => {
        const choixIdentifier = this.getChoixIdentifier(choixItem);
        if (choixCollectionIdentifiers.includes(choixIdentifier)) {
          return false;
        }
        choixCollectionIdentifiers.push(choixIdentifier);
        return true;
      });
      return [...choixesToAdd, ...choixCollection];
    }
    return choixCollection;
  }
}
