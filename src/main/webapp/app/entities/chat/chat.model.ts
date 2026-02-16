import { IContact } from '../contact/contact.model';
import { Contact } from '../conversations/conversation.service';

export interface Chat {
  id: number;
  contact: Contact;
  channel: string;
  lastUpdated: string;
  messages: any | null;
}

export interface GroupedChats {
  [contactId: number]: {
    contact: IContact;
    chats: Chat[];
  };
}

// Vérifiez votre interface Chat, elle devrait ressembler à :
export interface RespnsegetChatsByContactIdGroupedByChannel {
  chatId: number;
  lastUpdated: string;
}
