import dayjs from 'dayjs/esm';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { IContact } from 'app/entities/contact/contact.model';
import { IGroupe } from 'app/entities/groupe/groupe.model';
import { IReferentiel } from 'app/entities/referentiel/referentiel.model';

export interface ISendSms {
  id: number;
  sender?: string | null;
  receiver?: string | null;
  msgdata?: string | null;
  sendateEnvoi?: dayjs.Dayjs | null;
  dialogue?: string | null;
  isSent?: boolean | null;
  isbulk?: boolean | null;
  inprocess?: boolean | null;
  titre?: string | null;
  deliveryStatus?: string | null;
  user?: IExtendedUser | null;
  destinateur?: IContact | null;
  destinataires?: IGroupe | null;
  statut?: IReferentiel | null;
  reactionCounters?: string | null;
  template_id?: number | null;
  namereceiver?: string | null;
  bulkId?: string | null;
  characterCount?: number | null;
  totalMessage?: number | null;
  totalSuccess?: number | null;
  totalFailure?: number | null;
  successRate?: number | null;
  failureRate?: number | null;
  totalDelivered?: number | null;
  totalSent?: number | null;
  totalRead?: number | null;
  totalPending?: number | null;
  totalFailed?: number | null;
  type?: MessageType | null;
  vars?: string | null;
  last_error?: string | null;
}
export enum MessageType {
  SMS = 'SMS',
  WHATSAPP = 'WHATSAPP',
}

export interface BulkProgress {
  totalRecipients: number;
  inserted: number;
  sent: number;
  failed: number;
  pendingInsertion: number;
  pendingSend: number;
  insertionProgress: number;
  sendProgress: number;
  ratePerSecond: number;
  elapsedSeconds: number;
  remainingInsertSeconds: number;
  remainingSendSeconds: number;
  insertionComplete: boolean;
}

export interface PageSms {
  content: Sms[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
export interface Sms {
  id: number;
  sendSmsId?: number;
  receiver: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  sendDate?: string; // ISO date string
  totalMessage?: number;
  msgdata?: string;
  messageId?: string;
  deliveryStatus?: string | null;
  namereceiver?: string | null;
  vars?: string | null;
  template_id?: number | null;
  type?: MessageType | null;
  last_error?: string | null;
  direction?: 'INBOUND' | 'OUTBOUND';
  sender?: string;
  chat?: { id: number };
}

export type NewSendSms = Omit<ISendSms, 'id'> & { id: null };
