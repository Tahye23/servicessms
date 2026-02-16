import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IGroupedecontact, NewGroupedecontact } from '../groupedecontact.model';

export type PartialUpdateGroupedecontact = Partial<IGroupedecontact> & Pick<IGroupedecontact, 'id'>;

export type EntityResponseType = HttpResponse<IGroupedecontact>;
export type EntityArrayResponseType = HttpResponse<IGroupedecontact[]>;

@Injectable({ providedIn: 'root' })
export class GroupedecontactService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/groupedecontacts');

  create(groupedecontact: NewGroupedecontact): Observable<EntityResponseType> {
    return this.http.post<IGroupedecontact>(this.resourceUrl, groupedecontact, { observe: 'response' });
  }

  update(groupedecontact: IGroupedecontact): Observable<EntityResponseType> {
    return this.http.put<IGroupedecontact>(`${this.resourceUrl}/${this.getGroupedecontactIdentifier(groupedecontact)}`, groupedecontact, {
      observe: 'response',
    });
  }

  partialUpdate(groupedecontact: PartialUpdateGroupedecontact): Observable<EntityResponseType> {
    return this.http.patch<IGroupedecontact>(`${this.resourceUrl}/${this.getGroupedecontactIdentifier(groupedecontact)}`, groupedecontact, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IGroupedecontact>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IGroupedecontact[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getGroupedecontactIdentifier(groupedecontact: Pick<IGroupedecontact, 'id'>): number {
    return groupedecontact.id;
  }

  compareGroupedecontact(o1: Pick<IGroupedecontact, 'id'> | null, o2: Pick<IGroupedecontact, 'id'> | null): boolean {
    return o1 && o2 ? this.getGroupedecontactIdentifier(o1) === this.getGroupedecontactIdentifier(o2) : o1 === o2;
  }

  addGroupedecontactToCollectionIfMissing<Type extends Pick<IGroupedecontact, 'id'>>(
    groupedecontactCollection: Type[],
    ...groupedecontactsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const groupedecontacts: Type[] = groupedecontactsToCheck.filter(isPresent);
    if (groupedecontacts.length > 0) {
      const groupedecontactCollectionIdentifiers = groupedecontactCollection.map(groupedecontactItem =>
        this.getGroupedecontactIdentifier(groupedecontactItem),
      );
      const groupedecontactsToAdd = groupedecontacts.filter(groupedecontactItem => {
        const groupedecontactIdentifier = this.getGroupedecontactIdentifier(groupedecontactItem);
        if (groupedecontactCollectionIdentifiers.includes(groupedecontactIdentifier)) {
          return false;
        }
        groupedecontactCollectionIdentifiers.push(groupedecontactIdentifier);
        return true;
      });
      return [...groupedecontactsToAdd, ...groupedecontactCollection];
    }
    return groupedecontactCollection;
  }
}
