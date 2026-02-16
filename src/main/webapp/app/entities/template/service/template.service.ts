import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ITemplate, Template } from '../template.model';
import { ApplicationConfigService } from '../../../core/config/application-config.service';
export interface PageTemplate {
  content: ITemplate[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}
export interface MediaUploadResponse {
  success: boolean;
  mediaId: string;
  filename: string;
  mediaType: string;
  fileSize: number;
}
export interface ContactFieldInfo {
  fieldName: string;
  displayName: string;
  type: 'standard' | 'custom';
  required: boolean;
}
// Interface
export interface VariableInfo {
  name: string;
  defaultValue: string;
  type: string;
  index: number;
}
@Injectable({
  providedIn: 'root',
})
export class TemplateService {
  protected applicationConfigService = inject(ApplicationConfigService);
  protected baseUrl = this.applicationConfigService.getEndpointFor('/api/templates');
  protected whatsappUrl = this.applicationConfigService.getEndpointFor('/api/whats-apps');

  constructor(private http: HttpClient) {}

  // template.service.ts

  uploadMediaForTemplate(templateId: number, file: File, mediaType: 'IMAGE' | 'VIDEO' | 'DOCUMENT'): Observable<MediaUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mediaType', mediaType);

    return this.http.post<MediaUploadResponse>(`${this.baseUrl}/${templateId}/upload-media`, formData);
  }

  getAvailableFields(): Observable<ContactFieldInfo[]> {
    return this.http.get<ContactFieldInfo[]>(`${this.baseUrl}/available-fields`);
  }
  getTemplates(approved: string, page: number, size: number, search?: string): Observable<PageTemplate> {
    let params = new HttpParams().set('approved', approved.toString()).set('page', page.toString()).set('size', size.toString());
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get<PageTemplate>(this.baseUrl, { params });
  }
  // template.service.ts
  getMediaUrl(handle: string): Observable<string> {
    return this.http.get<{ url: string }>(`/api/media/${encodeURIComponent(handle)}`).pipe(map(resp => resp.url));
  }

  getAllTemplates(page: number, size: number, search?: string, isWhatsapp?: boolean, approved?: boolean): Observable<PageTemplate> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (search) {
      params = params.set('search', search);
    }
    if (isWhatsapp !== undefined) {
      params = params.set('isWhatsapp', isWhatsapp.toString());
    }
    if (approved !== undefined && approved !== null) {
      params = params.set('approved', approved.toString());
    }

    return this.http.get<PageTemplate>(this.baseUrl, { params });
  }
  importTemplateFromMeta(templateName: string): Observable<ITemplate> {
    return this.http.post<ITemplate>(`${this.baseUrl}/import-from-meta`, {
      templateName: templateName,
    });
  }
  refreshStatuses(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/refresh`, null);
  }
  getTemplate(id: number): Observable<ITemplate> {
    return this.http.get<ITemplate>(`${this.baseUrl}/${id}`);
  }

  createTemplate(template: ITemplate): Observable<ITemplate> {
    return this.http.post<ITemplate>(this.baseUrl, template);
  }

  updateTemplate(template: ITemplate): Observable<ITemplate> {
    return this.http.put<ITemplate>(`${this.baseUrl}/${template.id}`, template);
  }
  approveTemplate(id: number): Observable<ITemplate> {
    // L'endpoint sur le backend sera de la forme /api/templates/{id}/approve
    return this.http.put<ITemplate>(`${this.baseUrl}/${id}/approve`, {});
  }
  rejectedTemplate(id: number): Observable<ITemplate> {
    // L'endpoint sur le backend sera de la forme /api/templates/{id}/approve
    return this.http.put<ITemplate>(`${this.baseUrl}/${id}/rejected`, {});
  }
  deleteTemplate(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }
  find(id: number): Observable<HttpResponse<ITemplate>> {
    return this.http.get<ITemplate>(`${this.baseUrl}/${id}`, { observe: 'response' });
  }

  getWhatsappTemplates(approved: boolean): Observable<any[]> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    const params = new HttpParams().set('approved', approved.toString());
    return this.http.get<any>(`${this.whatsappUrl}/templates`, { headers, params }).pipe(map(response => response.data));
  }

  getTemplateByMetaId(templateId: string): Observable<Template> {
    return this.http.get<Template>(`${this.baseUrl}/by-meta-id/${templateId}`);
  }
  // template.service.ts

  extractTemplateVariables(templateId: number): Observable<VariableInfo[]> {
    return this.http.get<VariableInfo[]>(`${this.baseUrl}/${templateId}/variables`);
  }

  updateVariableMapping(templateId: number, mappings: Map<string, string>): Observable<ITemplate> {
    const mappingsObject = Object.fromEntries(mappings);
    return this.http.post<ITemplate>(`${this.baseUrl}/${templateId}/variable-mapping`, {
      mappings: mappingsObject,
    });
  }
}
