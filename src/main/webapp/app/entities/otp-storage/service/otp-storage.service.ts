import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IOTPStorage, NewOTPStorage } from '../otp-storage.model';

export type PartialUpdateOTPStorage = Partial<IOTPStorage> & Pick<IOTPStorage, 'id'>;

type RestOf<T extends IOTPStorage | NewOTPStorage> = Omit<T, 'otsdateexpir'> & {
  otsdateexpir?: string | null;
};

export type RestOTPStorage = RestOf<IOTPStorage>;

export type NewRestOTPStorage = RestOf<NewOTPStorage>;

export type PartialUpdateRestOTPStorage = RestOf<PartialUpdateOTPStorage>;

export type EntityResponseType = HttpResponse<IOTPStorage>;
export type EntityArrayResponseType = HttpResponse<IOTPStorage[]>;

@Injectable({ providedIn: 'root' })
export class OTPStorageService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/otp-storages');

  create(oTPStorage: NewOTPStorage): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(oTPStorage);
    return this.http
      .post<RestOTPStorage>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(oTPStorage: IOTPStorage): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(oTPStorage);
    return this.http
      .put<RestOTPStorage>(`${this.resourceUrl}/${this.getOTPStorageIdentifier(oTPStorage)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(oTPStorage: PartialUpdateOTPStorage): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(oTPStorage);
    return this.http
      .patch<RestOTPStorage>(`${this.resourceUrl}/${this.getOTPStorageIdentifier(oTPStorage)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestOTPStorage>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestOTPStorage[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getOTPStorageIdentifier(oTPStorage: Pick<IOTPStorage, 'id'>): number {
    return oTPStorage.id;
  }

  compareOTPStorage(o1: Pick<IOTPStorage, 'id'> | null, o2: Pick<IOTPStorage, 'id'> | null): boolean {
    return o1 && o2 ? this.getOTPStorageIdentifier(o1) === this.getOTPStorageIdentifier(o2) : o1 === o2;
  }

  addOTPStorageToCollectionIfMissing<Type extends Pick<IOTPStorage, 'id'>>(
    oTPStorageCollection: Type[],
    ...oTPStoragesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const oTPStorages: Type[] = oTPStoragesToCheck.filter(isPresent);
    if (oTPStorages.length > 0) {
      const oTPStorageCollectionIdentifiers = oTPStorageCollection.map(oTPStorageItem => this.getOTPStorageIdentifier(oTPStorageItem));
      const oTPStoragesToAdd = oTPStorages.filter(oTPStorageItem => {
        const oTPStorageIdentifier = this.getOTPStorageIdentifier(oTPStorageItem);
        if (oTPStorageCollectionIdentifiers.includes(oTPStorageIdentifier)) {
          return false;
        }
        oTPStorageCollectionIdentifiers.push(oTPStorageIdentifier);
        return true;
      });
      return [...oTPStoragesToAdd, ...oTPStorageCollection];
    }
    return oTPStorageCollection;
  }

  protected convertDateFromClient<T extends IOTPStorage | NewOTPStorage | PartialUpdateOTPStorage>(oTPStorage: T): RestOf<T> {
    return {
      ...oTPStorage,
      otsdateexpir: oTPStorage.otsdateexpir?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restOTPStorage: RestOTPStorage): IOTPStorage {
    return {
      ...restOTPStorage,
      otsdateexpir: restOTPStorage.otsdateexpir ? dayjs(restOTPStorage.otsdateexpir) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestOTPStorage>): HttpResponse<IOTPStorage> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestOTPStorage[]>): HttpResponse<IOTPStorage[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
  // otpenv(id: number, expirationMinutes: number): Observable<HttpResponse<any>> {
  //   return this.http.post<HttpResponse<any>>(`${this.resourceUrl}/generate/${id}/${expirationMinutes}`, {}); // Envoie l'ID dans l'URL
  // }

  otpenv(id: number): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/gener/${id}`, {}); // Envoie l'ID dans l'URL
  }

  otpsms(id: number): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/sendUI/${id}`, {}); // Envoie l'ID dans l'URL
  }

  // sendSmsM(id: number): Observable<any> {
  //   return this.http.post<any>(`${this.resourceUrl}/send/${id}`, {}); // Envoie l'ID dans l'URL
  // }
}
