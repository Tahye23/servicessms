// external-api-messages.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ExternalApiMessage {
  id: number;
  type: 'SMS' | 'WHATSAPP';
  status: string;
  sender: string;
  receiver: string;
  msgdata: string;
  last_error: string;
  sendDate: string;
  totalMessage: number;
  messageId?: string;
}

export interface ExternalApiStats {
  totalSms: number;
  totalWhatsapp: number;
  successSms: number;
  successWhatsapp: number;
  failedSms: number;
  failedWhatsapp: number;
  totalSegments: number;
}

export interface PageResponse {
  content: ExternalApiMessage[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class ExternalApiMessagesService {
  private resourceUrl = '/api/external-api-messages';

  constructor(private http: HttpClient) {}

  getMessages(page: number, size: number, status?: string, type?: string, search?: string): Observable<PageResponse> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());

    if (status) params = params.set('status', status);
    if (type) params = params.set('type', type);
    if (search) params = params.set('search', search);

    return this.http.get<PageResponse>(this.resourceUrl, { params });
  }

  getStats(): Observable<ExternalApiStats> {
    return this.http.get<ExternalApiStats>(`${this.resourceUrl}/stats`);
  }
}
