import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { DATE_FORMAT } from 'app/config/input.constants';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { ICompany, NewCompany } from '../company.model';

export type PartialUpdateCompany = Partial<ICompany> & Pick<ICompany, 'id'>;

type RestOf<T extends ICompany | NewCompany> = Omit<T, 'camdatecreation' | 'camdatefin'> & {
  camdatecreation?: string | null;
  camdatefin?: string | null;
};

export type RestCompany = RestOf<ICompany>;

export type NewRestCompany = RestOf<NewCompany>;

export type PartialUpdateRestCompany = RestOf<PartialUpdateCompany>;

export type EntityResponseType = HttpResponse<ICompany>;
export type EntityArrayResponseType = HttpResponse<ICompany[]>;

@Injectable({ providedIn: 'root' })
export class CompanyService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/companies');

  create(company: NewCompany): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(company);
    return this.http
      .post<RestCompany>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(company: ICompany): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(company);
    return this.http
      .put<RestCompany>(`${this.resourceUrl}/${this.getCompanyIdentifier(company)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(company: PartialUpdateCompany): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(company);
    return this.http
      .patch<RestCompany>(`${this.resourceUrl}/${this.getCompanyIdentifier(company)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestCompany>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestCompany[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getCompanyIdentifier(company: Pick<ICompany, 'id'>): number {
    return company.id;
  }

  compareCompany(o1: Pick<ICompany, 'id'> | null, o2: Pick<ICompany, 'id'> | null): boolean {
    return o1 && o2 ? this.getCompanyIdentifier(o1) === this.getCompanyIdentifier(o2) : o1 === o2;
  }

  addCompanyToCollectionIfMissing<Type extends Pick<ICompany, 'id'>>(
    companyCollection: Type[],
    ...companiesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const companies: Type[] = companiesToCheck.filter(isPresent);
    if (companies.length > 0) {
      const companyCollectionIdentifiers = companyCollection.map(companyItem => this.getCompanyIdentifier(companyItem));
      const companiesToAdd = companies.filter(companyItem => {
        const companyIdentifier = this.getCompanyIdentifier(companyItem);
        if (companyCollectionIdentifiers.includes(companyIdentifier)) {
          return false;
        }
        companyCollectionIdentifiers.push(companyIdentifier);
        return true;
      });
      return [...companiesToAdd, ...companyCollection];
    }
    return companyCollection;
  }

  protected convertDateFromClient<T extends ICompany | NewCompany | PartialUpdateCompany>(company: T): RestOf<T> {
    return {
      ...company,
      camdatecreation: company.camdatecreation?.format(DATE_FORMAT) ?? null,
      camdatefin: company.camdatefin?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restCompany: RestCompany): ICompany {
    return {
      ...restCompany,
      camdatecreation: restCompany.camdatecreation ? dayjs(restCompany.camdatecreation) : undefined,
      camdatefin: restCompany.camdatefin ? dayjs(restCompany.camdatefin) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestCompany>): HttpResponse<ICompany> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestCompany[]>): HttpResponse<ICompany[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
