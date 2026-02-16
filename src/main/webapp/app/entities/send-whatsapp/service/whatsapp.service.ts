import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { map, Observable } from 'rxjs';
import { MessageType, NewSendSms } from '../../send-sms/send-sms.model';
import dayjs from 'dayjs/esm';
import { TemplatePayload } from '../whatsapp.model';

@Injectable({
  providedIn: 'root',
})
export class WhatsappService {
  constructor(private http: HttpClient) {}

  protected applicationConfigService = inject(ApplicationConfigService);
  private token = localStorage.getItem('authToken');
  protected baseUrl = this.applicationConfigService.getEndpointFor('/api/whats-apps');

  submitTemplate(template: any): Observable<any> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    return this.http.post<any>(`${this.baseUrl}/create-template`, template, { headers });
  }
  createTemplate(template: any): Observable<any> {
    // Récupérer le token JWT depuis le localStorage ou sessionStorage
    const token = localStorage.getItem('authToken') || sessionStorage.getItem('authToken') || '';

    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    });

    // Transformer le payload frontend vers le format backend
    const backendPayload = this.transformToBackendFormat(template);

    return this.http.post<any>(`${this.baseUrl}/create-template`, backendPayload, { headers });
  }

  /**
   * Transforme le payload frontend vers le format attendu par le backend
   */
  private transformToBackendFormat(frontendTemplate: any): any {
    return {
      name: frontendTemplate.name,
      language: frontendTemplate.language,
      category: frontendTemplate.category,
      components: frontendTemplate.components
        .map((comp: any) => ({
          type: comp.type,
          format: comp.format,
          text: comp.text,
          mediaUrl: comp.mediaUrl,
          fileName: comp.fileName,
          fileSize: comp.fileSize,
          mimeType: comp.mimeType,
          documentName: comp.documentName,
          buttons: comp.buttons?.map((btn: any) => ({
            type: btn.type,
            text: btn.text,
            url: btn.url,
            phoneNumber: btn.phoneNumber,
          })),
        }))
        .filter((comp: any) => this.isValidComponent(comp)),
    };
  }

  private isValidComponent(comp: any): boolean {
    switch (comp.type) {
      case 'HEADER':
        return comp.format === 'TEXT' ? !!comp.text?.trim() : !!comp.mediaUrl;
      case 'BODY':
        return !!comp.text?.trim();
      case 'FOOTER':
        return !!comp.text?.trim();
      case 'BUTTONS':
        return Array.isArray(comp.buttons) && comp.buttons.length > 0;
      default:
        return false;
    }
  }
  convertToISendSms(payload: any): NewSendSms {
    return {
      id: null,
      isSent: payload.isSent ?? null,
      sendateEnvoi: dayjs(payload.sendDate),
      template_id: Number(payload.templateId) ?? null,
      destinateur: payload.recipientContact ?? null,
      destinataires: payload.recipientGroup ?? null,
      user: null,
      statut: null,
      receiver: payload.numero ?? null,
      sender: null,
      msgdata: null,
      dialogue: null,
      isbulk: null,
      titre: null,
      characterCount: null,
      type: MessageType.WHATSAPP,
    };
  }
  getWhatsappTemplates(approved: boolean): Observable<any[]> {
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    const params = new HttpParams().set('approved', approved.toString());
    return this.http.get<any>(`${this.baseUrl}/templates`, { headers, params }).pipe(map(response => response.data));
  }

  saveSendWhatsapp(payload: any): Observable<any> {
    return this.http.post<any>(this.baseUrl, payload);
  }

  sendWhatsapp(payload: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/send`, payload);
  }

  getAllSendWhatsapp(page: number, size: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}?page=${page}&size=${size}`);
  }

  getSendWhatsappById(id: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${id}`);
  }

  update(id: number, sendWhatsappItem: any): Observable<any> {
    return this.http.put(`/api/whats-apps/${id}`, sendWhatsappItem);
  }
}
