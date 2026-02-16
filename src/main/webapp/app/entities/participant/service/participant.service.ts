import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IParticipant, NewParticipant } from '../participant.model';

export type PartialUpdateParticipant = Partial<IParticipant> & Pick<IParticipant, 'id'>;

export type EntityResponseType = HttpResponse<IParticipant>;
export type EntityArrayResponseType = HttpResponse<IParticipant[]>;

@Injectable({ providedIn: 'root' })
export class ParticipantService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/participants');

  create(participant: NewParticipant): Observable<EntityResponseType> {
    return this.http.post<IParticipant>(this.resourceUrl, participant, { observe: 'response' });
  }

  update(participant: IParticipant): Observable<EntityResponseType> {
    return this.http.put<IParticipant>(`${this.resourceUrl}/${this.getParticipantIdentifier(participant)}`, participant, {
      observe: 'response',
    });
  }

  partialUpdate(participant: PartialUpdateParticipant): Observable<EntityResponseType> {
    return this.http.patch<IParticipant>(`${this.resourceUrl}/${this.getParticipantIdentifier(participant)}`, participant, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IParticipant>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IParticipant[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getParticipantIdentifier(participant: Pick<IParticipant, 'id'>): number {
    return participant.id;
  }

  compareParticipant(o1: Pick<IParticipant, 'id'> | null, o2: Pick<IParticipant, 'id'> | null): boolean {
    return o1 && o2 ? this.getParticipantIdentifier(o1) === this.getParticipantIdentifier(o2) : o1 === o2;
  }

  addParticipantToCollectionIfMissing<Type extends Pick<IParticipant, 'id'>>(
    participantCollection: Type[],
    ...participantsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const participants: Type[] = participantsToCheck.filter(isPresent);
    if (participants.length > 0) {
      const participantCollectionIdentifiers = participantCollection.map(participantItem => this.getParticipantIdentifier(participantItem));
      const participantsToAdd = participants.filter(participantItem => {
        const participantIdentifier = this.getParticipantIdentifier(participantItem);
        if (participantCollectionIdentifiers.includes(participantIdentifier)) {
          return false;
        }
        participantCollectionIdentifiers.push(participantIdentifier);
        return true;
      });
      return [...participantsToAdd, ...participantCollection];
    }
    return participantCollection;
  }
}
