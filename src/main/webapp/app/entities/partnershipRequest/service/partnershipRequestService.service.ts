import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';

// Interfaces et modèles
export enum RequestStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  IN_REVIEW = 'IN_REVIEW',
}

export interface IPartnershipRequest {
  id: number;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  phone?: string | null;
  companyName?: string | null;
  industry?: string | null;
  projectDescription?: string | null;
  monthlyVolume?: string | null;
  launchDate?: string | null;
  selectedPlanId?: number | null;
  selectedPlanName?: string | null;
  status?: RequestStatus | null;
  createdDate?: dayjs.Dayjs | null;
  processedDate?: dayjs.Dayjs | null;
  adminNotes?: string | null;
}

export type NewPartnershipRequest = Omit<IPartnershipRequest, 'id'> & { id: null };

export type PartialUpdatePartnershipRequest = Partial<IPartnershipRequest> & Pick<IPartnershipRequest, 'id'>;

type RestOf<T extends IPartnershipRequest | NewPartnershipRequest> = Omit<T, 'createdDate' | 'processedDate'> & {
  createdDate?: string | null;
  processedDate?: string | null;
};

export type RestPartnershipRequest = RestOf<IPartnershipRequest>;
export type NewRestPartnershipRequest = RestOf<NewPartnershipRequest>;
export type PartialUpdateRestPartnershipRequest = RestOf<PartialUpdatePartnershipRequest>;

export type EntityResponseType = HttpResponse<IPartnershipRequest>;
export type EntityArrayResponseType = HttpResponse<IPartnershipRequest[]>;

export interface PartnershipRequestStats {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  todayRequests: number;
}

@Injectable({ providedIn: 'root' })
export class PartnershipRequestService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/partnership-requests');

  constructor(
    protected http: HttpClient,
    protected applicationConfigService: ApplicationConfigService,
  ) {}

  create(partnershipRequest: NewPartnershipRequest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(partnershipRequest);
    return this.http
      .post<RestPartnershipRequest>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(partnershipRequest: IPartnershipRequest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(partnershipRequest);
    return this.http
      .put<RestPartnershipRequest>(`${this.resourceUrl}/${this.getPartnershipRequestIdentifier(partnershipRequest)}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(partnershipRequest: PartialUpdatePartnershipRequest): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(partnershipRequest);
    return this.http
      .patch<RestPartnershipRequest>(`${this.resourceUrl}/${this.getPartnershipRequestIdentifier(partnershipRequest)}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestPartnershipRequest>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestPartnershipRequest[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  approve(id: number, adminNotes?: string): Observable<EntityResponseType> {
    const body = adminNotes ? { adminNotes } : {};
    return this.http
      .post<RestPartnershipRequest>(`${this.resourceUrl}/${id}/approve`, body, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  reject(id: number, adminNotes?: string): Observable<EntityResponseType> {
    const body = adminNotes ? { adminNotes } : {};
    return this.http
      .post<RestPartnershipRequest>(`${this.resourceUrl}/${id}/reject`, body, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  getStatistics(): Observable<HttpResponse<PartnershipRequestStats>> {
    return this.http.get<PartnershipRequestStats>(`${this.resourceUrl}/statistics`, { observe: 'response' });
  }

  getByStatus(status: string, req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestPartnershipRequest[]>(`${this.resourceUrl}/by-status/${status}`, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  getOldPendingRequests(daysOld: number = 7): Observable<EntityArrayResponseType> {
    return this.http
      .get<RestPartnershipRequest[]>(`${this.resourceUrl}/old-pending?daysOld=${daysOld}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  bulkApprove(ids: number[], adminNotes?: string): Observable<HttpResponse<any>> {
    const body = { ids, adminNotes };
    return this.http.post<any>(`${this.resourceUrl}/bulk-approve`, body, { observe: 'response' });
  }

  bulkReject(ids: number[], adminNotes?: string): Observable<HttpResponse<any>> {
    const body = { ids, adminNotes };
    return this.http.post<any>(`${this.resourceUrl}/bulk-reject`, body, { observe: 'response' });
  }

  exportData(format: 'excel' | 'csv' | 'pdf', filters?: any): Observable<Blob> {
    const options = createRequestOption(filters);
    return this.http.get(`${this.resourceUrl}/export/${format}`, {
      params: options,
      responseType: 'blob',
    });
  }

  getPartnershipRequestIdentifier(partnershipRequest: Pick<IPartnershipRequest, 'id'>): number {
    return partnershipRequest.id;
  }

  comparePartnershipRequest(o1: Pick<IPartnershipRequest, 'id'> | null, o2: Pick<IPartnershipRequest, 'id'> | null): boolean {
    return o1 && o2 ? this.getPartnershipRequestIdentifier(o1) === this.getPartnershipRequestIdentifier(o2) : o1 === o2;
  }

  addPartnershipRequestToCollectionIfMissing<Type extends Pick<IPartnershipRequest, 'id'>>(
    partnershipRequestCollection: Type[],
    ...partnershipRequestsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const partnershipRequests: Type[] = partnershipRequestsToCheck.filter(isPresent);
    if (partnershipRequests.length > 0) {
      const partnershipRequestCollectionIdentifiers = partnershipRequestCollection.map(
        partnershipRequestItem => this.getPartnershipRequestIdentifier(partnershipRequestItem)!,
      );
      const partnershipRequestsToAdd = partnershipRequests.filter(partnershipRequestItem => {
        const partnershipRequestIdentifier = this.getPartnershipRequestIdentifier(partnershipRequestItem);
        if (partnershipRequestCollectionIdentifiers.includes(partnershipRequestIdentifier)) {
          return false;
        }
        partnershipRequestCollectionIdentifiers.push(partnershipRequestIdentifier);
        return true;
      });
      return [...partnershipRequestsToAdd, ...partnershipRequestCollection];
    }
    return partnershipRequestCollection;
  }

  protected convertDateFromClient<T extends IPartnershipRequest | NewPartnershipRequest | PartialUpdatePartnershipRequest>(
    partnershipRequest: T,
  ): RestOf<T> {
    return {
      ...partnershipRequest,
      createdDate: partnershipRequest.createdDate?.toJSON() ?? null,
      processedDate: partnershipRequest.processedDate?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restPartnershipRequest: RestPartnershipRequest): IPartnershipRequest {
    return {
      ...restPartnershipRequest,
      createdDate: restPartnershipRequest.createdDate ? dayjs(restPartnershipRequest.createdDate) : undefined,
      processedDate: restPartnershipRequest.processedDate ? dayjs(restPartnershipRequest.processedDate) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestPartnershipRequest>): HttpResponse<IPartnershipRequest> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestPartnershipRequest[]>): HttpResponse<IPartnershipRequest[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
