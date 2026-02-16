import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Contact {
  id: number;
  name: string;
  phone: string;
}

export interface SmsC {
  id: number;
  sender: string;
  receiver: string;
  msgdata: string;
  timestamp: string;
  isSent: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private base = '/api'; // ajustez selon votre API

  constructor(private http: HttpClient) {}

  getContacts(page: number, size: number): Observable<{ contacts: Contact[]; total: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ contacts: Contact[]; total: number }>(`${this.base}/contacts`, { params });
  }

  getMessages(contactId: number): Observable<SmsC[]> {
    return this.http.get<SmsC[]>(`${this.base}/contacts/${contactId}/messages`);
  }

  sendMessage(contactId: number, text: string): Observable<SmsC> {
    return this.http.post<SmsC>(`${this.base}/contacts/${contactId}/messages`, { text });
  }
}
