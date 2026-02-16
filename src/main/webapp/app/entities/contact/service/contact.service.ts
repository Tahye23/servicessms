import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { AdvancedContactFilters, CustomFieldPayload, IContact, ImportHistory, NewContact, Page, ProgressStatus } from '../contact.model';
import { DuplicateContactsResponse } from '../list/contact.component';
import { map } from 'rxjs/operators';

export type PartialUpdateContact = Partial<IContact> & Pick<IContact, 'id'>;

export type EntityResponseType = HttpResponse<IContact>;
export type EntityArrayResponseType = HttpResponse<IContact[]>;
export interface CustomField {
  label: string;
  maxLength: number;
}

@Injectable({ providedIn: 'root' })
export class ContactService {
  protected http = inject(HttpClient);

  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/contacts');
  protected resourceUrl2 = this.applicationConfigService.getEndpointFor('api/groupedecontacts');
  getImportHistoryByBulkId(bulkId: string): Observable<ImportHistory> {
    return this.http.get<ImportHistory>(`${this.resourceUrl}/import-history/${bulkId}`);
  }
  getValidContacts(): Observable<IContact[]> {
    return this.http.get<IContact[]>(`${this.resourceUrl}/valides`);
  }

  getDoublantContacts(): Observable<IContact[]> {
    return this.http.get<IContact[]>(`${this.resourceUrl}/dublants`);
  }
  exportContactsByBulkId(bulkId: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/bulk/${bulkId}/export`, {
      responseType: 'blob',
    });
  }

  create(contact: NewContact): Observable<EntityResponseType> {
    return this.http.post<IContact>(this.resourceUrl, contact, { observe: 'response' });
  }
  getProgressStatus(progressId: string) {
    return this.http.get<ProgressStatus>(`${this.resourceUrl}/clean/progress/${progressId}`);
  }
  getProgress(progressId: string): Observable<ProgressStatus> {
    return this.http.get<ProgressStatus>(`${this.resourceUrl}/progress/${progressId}`);
  }
  update(contact: IContact): Observable<EntityResponseType> {
    return this.http.put<IContact>(`${this.resourceUrl}/${this.getContactIdentifier(contact)}`, contact, { observe: 'response' });
  }

  partialUpdate(contact: PartialUpdateContact): Observable<EntityResponseType> {
    return this.http.patch<IContact>(`${this.resourceUrl}/${this.getContactIdentifier(contact)}`, contact, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IContact>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IContact[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
  advancedSearch(payload: any, req?: any): Observable<EntityArrayResponseType> {
    const params = createRequestOption(req);
    return this.http.post<IContact[]>(`${this.resourceUrl}/filter`, payload, {
      params,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getContactIdentifier(contact: Pick<IContact, 'id'>): number {
    return contact.id;
  }
  createCustomFields(fields: CustomFieldPayload[]): Observable<void> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<void>(`${this.resourceUrl}/custom-fields`, fields, { headers });
  }

  getCustomFields(): Observable<Record<string, CustomField>> {
    return this.http.get<Record<string, CustomField>>(`${this.resourceUrl}/custom-fields`);
  }
  downloadErrorFile(fileName: string): Observable<Blob> {
    const url = `${this.resourceUrl}/download/error/${fileName}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  downloadDuplicateFile(fileName: string): Observable<Blob> {
    const url = `${this.resourceUrl}/download/duplicate/${fileName}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  cleanContacts(file: File, groupId: number, insert: boolean = true): Observable<DuplicateContactsResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('groupId', groupId.toString());
    formData.append('insert', insert.toString());

    return this.http.post<DuplicateContactsResponse>(`${this.resourceUrl}/clean`, formData).pipe(
      map(response => {
        console.log(' Response brute du backend:', response);

        const normalizedResponse: DuplicateContactsResponse = {
          uniqueContacts: response.uniqueContacts || [],
          duplicateContacts: response.duplicateContacts || [],
          databaseDuplicates: response.databaseDuplicates || [],
          fileDuplicates: response.fileDuplicates || [],
          errorContacts: response.errorContacts || [],

          // Statistiques de base
          totalFileLines: response.totalFileLines || 0,
          totalInserted: response.totalInserted || 0,
          totalDuplicates: response.totalDuplicates || 0,
          totalDatabaseDuplicates: response.totalDatabaseDuplicates || 0,
          totalFileDuplicates: response.totalFileDuplicates || 0,
          totalErrors: response.totalErrors || 0,

          errorFileLocation: response.errorFileLocation,
          duplicateFileLocation: response.duplicateFileLocation,
          databaseDuplicateFileLocation: response.databaseDuplicateFileLocation,
          fileDuplicateFileLocation: response.fileDuplicateFileLocation,

          progressId: response.progressId,

          successRate: response.successRate,
          duplicateRate: response.duplicateRate,
          errorRate: response.errorRate,
          importSummary: response.importSummary,

          valid: response.valid,
          hasDuplicates: response.hasDuplicates,
          hasErrors: response.hasErrors,
          hasDatabaseDuplicates: response.hasDatabaseDuplicates,
          hasFileDuplicates: response.hasFileDuplicates,

          allContactsToInsert: response.allContactsToInsert || response.uniqueContacts || [],
        };

        console.log(' Response normalisée:', {
          totalLines: normalizedResponse.totalFileLines,
          inserted: normalizedResponse.totalInserted,
          dbDuplicates: normalizedResponse.totalDatabaseDuplicates,
          fileDuplicates: normalizedResponse.totalFileDuplicates,
          errors: normalizedResponse.totalErrors,
          progressId: normalizedResponse.progressId,
        });

        return normalizedResponse;
      }),
    );
  }

  downloadDatabaseDuplicateFile(fileName: string): Observable<Blob> {
    const url = `${this.resourceUrl}/download/database-duplicate/${fileName}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  downloadFileDuplicateFile(fileName: string): Observable<Blob> {
    const url = `${this.resourceUrl}/download/file-duplicate/${fileName}`;
    return this.http.get(url, { responseType: 'blob' });
  }

  getContactsByBulkId(bulkId: string, page: number, size: number): Observable<Page<IContact>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<Page<IContact>>(`${this.resourceUrl}/bulk/${bulkId}`, { params });
  }
  getImportHistory(page: number, size: number, search?: string): Observable<Page<ImportHistory>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<Page<ImportHistory>>(`${this.resourceUrl}/import-history`, { params });
  }
  compareContact(o1: Pick<IContact, 'id'> | null, o2: Pick<IContact, 'id'> | null): boolean {
    return o1 && o2 ? this.getContactIdentifier(o1) === this.getContactIdentifier(o2) : o1 === o2;
  }

  addContactToCollectionIfMissing<Type extends Pick<IContact, 'id'>>(
    contactCollection: Type[],
    ...contactsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const contacts: Type[] = contactsToCheck.filter(isPresent);
    if (contacts.length > 0) {
      const contactCollectionIdentifiers = contactCollection.map(contactItem => this.getContactIdentifier(contactItem));
      const contactsToAdd = contacts.filter(contactItem => {
        const contactIdentifier = this.getContactIdentifier(contactItem);
        if (contactCollectionIdentifiers.includes(contactIdentifier)) {
          return false;
        }
        contactCollectionIdentifiers.push(contactIdentifier);
        return true;
      });
      return [...contactsToAdd, ...contactCollection];
    }
    return contactCollection;
  }
}
