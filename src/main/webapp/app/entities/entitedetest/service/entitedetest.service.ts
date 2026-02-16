import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IEntitedetest, NewEntitedetest } from '../entitedetest.model';

export type PartialUpdateEntitedetest = Partial<IEntitedetest> & Pick<IEntitedetest, 'id'>;

type RestOf<T extends IEntitedetest | NewEntitedetest> = Omit<T, 'champdate'> & {
  champdate?: string | null;
};

export type RestEntitedetest = RestOf<IEntitedetest>;

export type NewRestEntitedetest = RestOf<NewEntitedetest>;

export type PartialUpdateRestEntitedetest = RestOf<PartialUpdateEntitedetest>;

export type EntityResponseType = HttpResponse<IEntitedetest>;
export type EntityArrayResponseType = HttpResponse<IEntitedetest[]>;

@Injectable({ providedIn: 'root' })
export class EntitedetestService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/entitedetests');

  create(entitedetest: NewEntitedetest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(entitedetest);
    return this.http
      .post<RestEntitedetest>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(entitedetest: IEntitedetest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(entitedetest);
    return this.http
      .put<RestEntitedetest>(`${this.resourceUrl}/${this.getEntitedetestIdentifier(entitedetest)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(entitedetest: PartialUpdateEntitedetest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(entitedetest);
    return this.http
      .patch<RestEntitedetest>(`${this.resourceUrl}/${this.getEntitedetestIdentifier(entitedetest)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestEntitedetest>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestEntitedetest[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getEntitedetestIdentifier(entitedetest: Pick<IEntitedetest, 'id'>): number {
    return entitedetest.id;
  }

  compareEntitedetest(o1: Pick<IEntitedetest, 'id'> | null, o2: Pick<IEntitedetest, 'id'> | null): boolean {
    return o1 && o2 ? this.getEntitedetestIdentifier(o1) === this.getEntitedetestIdentifier(o2) : o1 === o2;
  }

  addEntitedetestToCollectionIfMissing<Type extends Pick<IEntitedetest, 'id'>>(
    entitedetestCollection: Type[],
    ...entitedetestsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const entitedetests: Type[] = entitedetestsToCheck.filter(isPresent);
    if (entitedetests.length > 0) {
      const entitedetestCollectionIdentifiers = entitedetestCollection.map(entitedetestItem =>
        this.getEntitedetestIdentifier(entitedetestItem),
      );
      const entitedetestsToAdd = entitedetests.filter(entitedetestItem => {
        const entitedetestIdentifier = this.getEntitedetestIdentifier(entitedetestItem);
        if (entitedetestCollectionIdentifiers.includes(entitedetestIdentifier)) {
          return false;
        }
        entitedetestCollectionIdentifiers.push(entitedetestIdentifier);
        return true;
      });
      return [...entitedetestsToAdd, ...entitedetestCollection];
    }
    return entitedetestCollection;
  }

  protected convertDateFromClient<T extends IEntitedetest | NewEntitedetest | PartialUpdateEntitedetest>(entitedetest: T): RestOf<T> {
    return {
      ...entitedetest,
      champdate: entitedetest.champdate?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restEntitedetest: RestEntitedetest): IEntitedetest {
    return {
      ...restEntitedetest,
      champdate: restEntitedetest.champdate ? dayjs(restEntitedetest.champdate) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestEntitedetest>): HttpResponse<IEntitedetest> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestEntitedetest[]>): HttpResponse<IEntitedetest[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
