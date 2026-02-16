import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { AdvancedFiltersPayload, ICampaignSummary, IGroupe, NewGroupe } from '../groupe.model';
import { IContact, AdvancedContactFilters, MessageStatistics, BulkLinkFiltersPayload } from '../../contact/contact.model';

export type PartialUpdateGroupe = Partial<IGroupe> & Pick<IGroupe, 'id'>;
export type EntityResponseType = HttpResponse<IGroupe>;
export type EntityArrayResponseType = HttpResponse<IGroupe[]>;

@Injectable({ providedIn: 'root' })
export class GroupeService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/groupes');
  protected apiUrl = this.applicationConfigService.getEndpointFor('api/contacts');

  getGroupesByContact(contactId: number): Observable<IGroupe[]> {
    return this.http.get<IGroupe[]>(`${this.resourceUrl}/contacts/${contactId}/groupes`);
  }
  /**
   * Retire des contacts d'un groupe
   */
  removeContactsFromGroup(groupeId: number, contactIds: number[]): Observable<HttpResponse<{ deletedCount: number; message: string }>> {
    const url = `${this.resourceUrl}/${groupeId}/contacts`;
    return this.http.delete<{ deletedCount: number; message: string }>(url, {
      body: contactIds,
      observe: 'response',
    });
  }
  // groupe.service.ts
  getCampaignsByGroup(groupeId: number, params: { page?: string; size?: string; search?: string }) {
    let httpParams = new HttpParams();
    if (params.page) httpParams = httpParams.set('page', params.page);
    if (params.size) httpParams = httpParams.set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    const url = `${this.resourceUrl}/${groupeId}/campaigns/sms`;

    return this.http.get<ICampaignSummary[]>(url, { params: httpParams, observe: 'response' });
  }

  create(groupe: NewGroupe): Observable<EntityResponseType> {
    return this.http.post<IGroupe>(this.resourceUrl, groupe, { observe: 'response' });
  }

  addContactsToGroup(groupeId: number, contactIds: number[]): Observable<HttpResponse<void>> {
    const url = `${this.resourceUrl}/${groupeId}/contacts`;
    return this.http.post<void>(url, contactIds, { observe: 'response' });
  }
  bulkLinkContactsByFilter(sourceGroupeId: number, targetGroupeId: number, filters: BulkLinkFiltersPayload) {
    return this.http.post<{ linked: number }>(`${this.resourceUrl}/${sourceGroupeId}/contacts/bulk-link`, filters, {
      params: new HttpParams().set('targetGroupeId', String(targetGroupeId)),
    });
  }

  update(groupe: IGroupe): Observable<EntityResponseType> {
    return this.http.put<IGroupe>(`${this.resourceUrl}/${this.getGroupeIdentifier(groupe)}`, groupe, { observe: 'response' });
  }

  partialUpdate(groupe: PartialUpdateGroupe): Observable<EntityResponseType> {
    return this.http.patch<IGroupe>(`${this.resourceUrl}/${this.getGroupeIdentifier(groupe)}`, groupe, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupe>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<any> {
    const options = createRequestOption(req);
    return this.http.get<IGroupe[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getGroupeIdentifier(groupe: Pick<IGroupe, 'id'>): number {
    return groupe.id;
  }

  compareGroupe(o1: Pick<IGroupe, 'id'> | null, o2: Pick<IGroupe, 'id'> | null): boolean {
    return o1 && o2 ? this.getGroupeIdentifier(o1) === this.getGroupeIdentifier(o2) : o1 === o2;
  }

  findGroupsByContactId(contactId: number, params: HttpParams = new HttpParams()): Observable<HttpResponse<IGroupe[]>> {
    const queryParams = params.set('contactId', contactId.toString());
    return this.http.get<IGroupe[]>(`${this.resourceUrl}/by-contact`, { params: queryParams, observe: 'response' });
  }

  addGroupeToCollectionIfMissing<Type extends Pick<IGroupe, 'id'>>(
    groupeCollection: Type[],
    ...groupesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const groupes: Type[] = groupesToCheck.filter(isPresent);
    if (groupes.length > 0) {
      const groupeCollectionIdentifiers = groupeCollection.map(groupeItem => this.getGroupeIdentifier(groupeItem));
      const groupesToAdd = groupes.filter(groupeItem => {
        const groupeIdentifier = this.getGroupeIdentifier(groupeItem);
        if (groupeCollectionIdentifiers.includes(groupeIdentifier)) {
          return false;
        }
        groupeCollectionIdentifiers.push(groupeIdentifier);
        return true;
      });
      return [...groupesToAdd, ...groupeCollection];
    }
    return groupeCollection;
  }

  // groupe.service.ts
  postContactSearch(
    groupeId: number,
    payload: AdvancedFiltersPayload, // ton interface côté front
    params?: HttpParams,
  ): Observable<HttpResponse<IContact[]>> {
    return this.http.post<IContact[]>(`${this.resourceUrl}/contact/${groupeId}/search`, payload, { params, observe: 'response' });
  }
  getContactHistory(groupeId: number, params?: HttpParams): Observable<HttpResponse<IContact[]>> {
    // this.resourceUrl doit être "/api/groupe"
    return this.http.get<IContact[]>(`${this.resourceUrl}/contact/${groupeId}/search`, {
      params,
      observe: 'response',
    });
  }
}
