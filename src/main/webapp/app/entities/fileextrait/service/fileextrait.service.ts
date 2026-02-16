import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { HttpEventType } from '@angular/common/http';

import { tap } from 'rxjs/operators';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IFileextrait, NewFileextrait } from '../fileextrait.model';
import { HttpEvent } from '@angular/common/http';
export type PartialUpdateFileextrait = Partial<IFileextrait> & Pick<IFileextrait, 'id'>;
import { IContact } from '../../contact/contact.model';

export type EntityResponseType = HttpResponse<IFileextrait>;
export type EntityArrayResponseType = HttpResponse<IFileextrait[]>;
export interface DuplicateContactsResponse {
  uniqueContacts: IContact[]; // Liste des contacts uniques
  duplicateContacts: IContact[];
  errorContacts: IContact[];
  // Liste des contacts doublons
}
@Injectable({ providedIn: 'root' })
export class FileextraitService {
  //uniqueContacts: IContact[];
  //duplicateContacts: IContact[];
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/fileextraits');
  //protected resourceUrl = this.applicationConfigService.getEndpointFor('api/contacts');

  create(fileextrait: NewFileextrait): Observable<EntityResponseType> {
    return this.http.post<IFileextrait>(this.resourceUrl, fileextrait, { observe: 'response' });
  }

  //hello

  pushFiletostorage(id: string, file: File): Observable<HttpEvent<{}>> {
    console.log('hello');
    const data: FormData = new FormData();
    data.append('file', file);
    console.log('file ajouter');
    return this.http.post(this.resourceUrl + '/pushfile/' + id, data, { observe: 'response' });
  }

  cleanContacts(file: File, selectedGroup: any): Observable<DuplicateContactsResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('groupId', selectedGroup);
    return this.http.post<DuplicateContactsResponse>(`${this.resourceUrl}/clean`, formData);
  }

  update(fileextrait: IFileextrait): Observable<EntityResponseType> {
    return this.http.put<IFileextrait>(`${this.resourceUrl}/${this.getFileextraitIdentifier(fileextrait)}`, fileextrait, {
      observe: 'response',
    });
  }

  partialUpdate(fileextrait: PartialUpdateFileextrait): Observable<EntityResponseType> {
    return this.http.patch<IFileextrait>(`${this.resourceUrl}/${this.getFileextraitIdentifier(fileextrait)}`, fileextrait, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IFileextrait>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IFileextrait[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getFileextraitIdentifier(fileextrait: Pick<IFileextrait, 'id'>): number {
    return fileextrait.id;
  }

  compareFileextrait(o1: Pick<IFileextrait, 'id'> | null, o2: Pick<IFileextrait, 'id'> | null): boolean {
    return o1 && o2 ? this.getFileextraitIdentifier(o1) === this.getFileextraitIdentifier(o2) : o1 === o2;
  }

  addFileextraitToCollectionIfMissing<Type extends Pick<IFileextrait, 'id'>>(
    fileextraitCollection: Type[],
    ...fileextraitsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const fileextraits: Type[] = fileextraitsToCheck.filter(isPresent);
    if (fileextraits.length > 0) {
      const fileextraitCollectionIdentifiers = fileextraitCollection.map(fileextraitItem => this.getFileextraitIdentifier(fileextraitItem));
      const fileextraitsToAdd = fileextraits.filter(fileextraitItem => {
        const fileextraitIdentifier = this.getFileextraitIdentifier(fileextraitItem);
        if (fileextraitCollectionIdentifiers.includes(fileextraitIdentifier)) {
          return false;
        }
        fileextraitCollectionIdentifiers.push(fileextraitIdentifier);
        return true;
      });
      return [...fileextraitsToAdd, ...fileextraitCollection];
    }
    return fileextraitCollection;
  }
}
