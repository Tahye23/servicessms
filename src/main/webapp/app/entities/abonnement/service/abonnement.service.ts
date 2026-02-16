import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IAbonnement, NewAbonnement } from '../abonnement.model';

export type PartialUpdateAbonnement = Partial<IAbonnement> & Pick<IAbonnement, 'id'>;

export type EntityResponseType = HttpResponse<IAbonnement>;
export type EntityArrayResponseType = HttpResponse<IAbonnement[]>;

@Injectable({ providedIn: 'root' })
export class AbonnementService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/abonnements');

  create(abonnement: NewAbonnement): Observable<EntityResponseType> {
    return this.http.post<IAbonnement>(this.resourceUrl, abonnement, { observe: 'response' });
  }

  update(abonnement: IAbonnement): Observable<EntityResponseType> {
    return this.http.put<IAbonnement>(`${this.resourceUrl}/${this.getAbonnementIdentifier(abonnement)}`, abonnement, {
      observe: 'response',
    });
  }

  partialUpdate(abonnement: PartialUpdateAbonnement): Observable<EntityResponseType> {
    return this.http.patch<IAbonnement>(`${this.resourceUrl}/${this.getAbonnementIdentifier(abonnement)}`, abonnement, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IAbonnement>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IAbonnement[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getAbonnementIdentifier(abonnement: Pick<IAbonnement, 'id'>): number {
    return abonnement.id;
  }

  compareAbonnement(o1: Pick<IAbonnement, 'id'> | null, o2: Pick<IAbonnement, 'id'> | null): boolean {
    return o1 && o2 ? this.getAbonnementIdentifier(o1) === this.getAbonnementIdentifier(o2) : o1 === o2;
  }

  addAbonnementToCollectionIfMissing<Type extends Pick<IAbonnement, 'id'>>(
    abonnementCollection: Type[],
    ...abonnementsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const abonnements: Type[] = abonnementsToCheck.filter(isPresent);
    if (abonnements.length > 0) {
      const abonnementCollectionIdentifiers = abonnementCollection.map(abonnementItem => this.getAbonnementIdentifier(abonnementItem));
      const abonnementsToAdd = abonnements.filter(abonnementItem => {
        const abonnementIdentifier = this.getAbonnementIdentifier(abonnementItem);
        if (abonnementCollectionIdentifiers.includes(abonnementIdentifier)) {
          return false;
        }
        abonnementCollectionIdentifiers.push(abonnementIdentifier);
        return true;
      });
      return [...abonnementsToAdd, ...abonnementCollection];
    }
    return abonnementCollection;
  }

  getAbonnementForCurrentUser(): Observable<EntityResponseType> {
    return this.http.get<IAbonnement>(`${this.resourceUrl}/user`, { observe: 'response' });
  }
}
