import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IReferentiel, NewReferentiel } from '../referentiel.model';

export type PartialUpdateReferentiel = Partial<IReferentiel> & Pick<IReferentiel, 'id'>;

export type EntityResponseType = HttpResponse<IReferentiel>;
export type EntityArrayResponseType = HttpResponse<IReferentiel[]>;

@Injectable({ providedIn: 'root' })
export class ReferentielService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/referentiels');

  create(referentiel: NewReferentiel): Observable<EntityResponseType> {
    return this.http.post<IReferentiel>(this.resourceUrl, referentiel, { observe: 'response' });
  }

  update(referentiel: IReferentiel): Observable<EntityResponseType> {
    return this.http.put<IReferentiel>(`${this.resourceUrl}/${this.getReferentielIdentifier(referentiel)}`, referentiel, {
      observe: 'response',
    });
  }

  partialUpdate(referentiel: PartialUpdateReferentiel): Observable<EntityResponseType> {
    return this.http.patch<IReferentiel>(`${this.resourceUrl}/${this.getReferentielIdentifier(referentiel)}`, referentiel, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IReferentiel>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IReferentiel[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getReferentielIdentifier(referentiel: Pick<IReferentiel, 'id'>): number {
    return referentiel.id;
  }

  compareReferentiel(o1: Pick<IReferentiel, 'id'> | null, o2: Pick<IReferentiel, 'id'> | null): boolean {
    return o1 && o2 ? this.getReferentielIdentifier(o1) === this.getReferentielIdentifier(o2) : o1 === o2;
  }

  addReferentielToCollectionIfMissing<Type extends Pick<IReferentiel, 'id'>>(
    referentielCollection: Type[],
    ...referentielsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const referentiels: Type[] = referentielsToCheck.filter(isPresent);
    if (referentiels.length > 0) {
      const referentielCollectionIdentifiers = referentielCollection.map(referentielItem => this.getReferentielIdentifier(referentielItem));
      const referentielsToAdd = referentiels.filter(referentielItem => {
        const referentielIdentifier = this.getReferentielIdentifier(referentielItem);
        if (referentielCollectionIdentifiers.includes(referentielIdentifier)) {
          return false;
        }
        referentielCollectionIdentifiers.push(referentielIdentifier);
        return true;
      });
      return [...referentielsToAdd, ...referentielCollection];
    }
    return referentielCollection;
  }
}
