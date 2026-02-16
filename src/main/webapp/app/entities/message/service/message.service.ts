import { Injectable } from '@angular/core';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Message, MessageResponse } from '../message.model';
@Injectable({
  providedIn: 'root',
})
export class MessageService {
  private apiUrl = 'your-api-base-url'; // Remplacez par votre URL d'API

  constructor(private http: HttpClient) {}

  getMessages(contactPhone: string, channel: 'WHATSAPP' | 'SMS', page: number = 0, size: number = 20): Observable<MessageResponse> {
    let params = new HttpParams()
      .set('receiver', contactPhone)
      .set('type', channel)
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'sendDate,desc'); // Tri par date décroissante

    return this.http.get<MessageResponse>(`${this.apiUrl}/messages`, { params });
  }

  sendMessage(receiver: string, message: string, type: 'WHATSAPP' | 'SMS', templateId?: number): Observable<Message> {
    const payload = {
      receiver,
      msgdata: message,
      type,
      template_id: templateId,
      contentType: templateId ? 'TEMPLATE' : 'TEXT',
      direction: 'OUTBOUND',
    };

    return this.http.post<Message>(`${this.apiUrl}/messages`, payload);
  }
}
