import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IDialogue, NewDialogue } from '../dialogue.model';

export type PartialUpdateDialogue = Partial<IDialogue> & Pick<IDialogue, 'id'>;

export type EntityResponseType = HttpResponse<IDialogue>;
export type EntityArrayResponseType = HttpResponse<IDialogue[]>;

@Injectable({ providedIn: 'root' })
export class DialogueService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/dialogues');

  create(dialogue: NewDialogue): Observable<EntityResponseType> {
    return this.http.post<IDialogue>(this.resourceUrl, dialogue, { observe: 'response' });
  }

  update(dialogue: IDialogue): Observable<EntityResponseType> {
    return this.http.put<IDialogue>(`${this.resourceUrl}/${this.getDialogueIdentifier(dialogue)}`, dialogue, { observe: 'response' });
  }

  partialUpdate(dialogue: PartialUpdateDialogue): Observable<EntityResponseType> {
    return this.http.patch<IDialogue>(`${this.resourceUrl}/${this.getDialogueIdentifier(dialogue)}`, dialogue, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IDialogue>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IDialogue[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getDialogueIdentifier(dialogue: Pick<IDialogue, 'id'>): number {
    return dialogue.id;
  }

  compareDialogue(o1: Pick<IDialogue, 'id'> | null, o2: Pick<IDialogue, 'id'> | null): boolean {
    return o1 && o2 ? this.getDialogueIdentifier(o1) === this.getDialogueIdentifier(o2) : o1 === o2;
  }

  addDialogueToCollectionIfMissing<Type extends Pick<IDialogue, 'id'>>(
    dialogueCollection: Type[],
    ...dialoguesToCheck: (Type | null | undefined)[]
  ): Type[] {
    const dialogues: Type[] = dialoguesToCheck.filter(isPresent);
    if (dialogues.length > 0) {
      const dialogueCollectionIdentifiers = dialogueCollection.map(dialogueItem => this.getDialogueIdentifier(dialogueItem));
      const dialoguesToAdd = dialogues.filter(dialogueItem => {
        const dialogueIdentifier = this.getDialogueIdentifier(dialogueItem);
        if (dialogueCollectionIdentifiers.includes(dialogueIdentifier)) {
          return false;
        }
        dialogueCollectionIdentifiers.push(dialogueIdentifier);
        return true;
      });
      return [...dialoguesToAdd, ...dialogueCollection];
    }
    return dialogueCollection;
  }
}
