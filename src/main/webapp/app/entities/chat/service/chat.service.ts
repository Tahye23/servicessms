import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Observable } from 'rxjs';
import { Sms } from 'app/entities/send-sms/send-sms.model';

import { HttpResponse } from '@angular/common/http';
import { Chat, RespnsegetChatsByContactIdGroupedByChannel } from 'app/entities/chat/chat.model';

export type EntityArrayResponseType = HttpResponse<Chat[]>;
export type SmsArrayResponseType = HttpResponse<Sms[]>;

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/chats');

  constructor() {}

  /**
   * Récupère les chats avec pagination.
   * @param page Numéro de page
   * @param size Nombre d'éléments par page
   */
  getAllChats(page = 0, size = 10): Observable<EntityArrayResponseType> {
    const options = {
      observe: 'response' as const,
      params: {
        page: page.toString(),
        size: size.toString(),
      },
    };
    return this.http.get<Chat[]>(this.resourceUrl, options);
  }

  /**
   * Récupère les messages d’un chat avec pagination.
   * @param chatId ID du chat
   * @param page Numéro de page
   * @param size Nombre d'éléments par page
   */
  getMessagesByChatId(chatId: number): Observable<SmsArrayResponseType> {
    const url = `${this.resourceUrl}/${chatId}/messages`;
    const options = {
      observe: 'response' as const,
    };
    return this.http.get<Sms[]>(url, options);
  }

  /**
   * Récupère les chats d’un contact groupés par canal.
   * @param contactId ID du contact
   */
  getChatsByContactIdGroupedByChannel(contactId: number): Observable<{ [channel: string]: RespnsegetChatsByContactIdGroupedByChannel[] }> {
    const url = `${this.resourceUrl}/by-contact/${contactId}/group-by-channel`;
    return this.http.get<{ [channel: string]: RespnsegetChatsByContactIdGroupedByChannel[] }>(url);
  }

  createChat(contactId: any, channel: 'SMS' | 'WHATSAPP'): Observable<HttpResponse<Chat>> {
    const url = this.resourceUrl;
    const body = {
      contactId,
      channel,
    };

    return this.http.post<Chat>(url, body, { observe: 'response' });
  }
}
