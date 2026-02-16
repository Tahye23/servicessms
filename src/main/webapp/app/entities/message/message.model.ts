export interface Message {
  id: number;
  batch?: {
    id: number;
  } | null;
  messageId?: string | null;
  template_id?: number | null;
  chat?: {
    id: number;
  } | null;
  deliveryStatus?: string | null;
  sender: string;
  contentType?: string | null;
  type: 'WHATSAPP' | 'SMS';
  receiver: string;
  msgdata: string;
  totalMessage?: number | null;
  sendDate?: string | null;
  status?: string | null;
  bulkId?: string | null;
  bulkCreatedAt?: string | null;
  direction?: string | null;
  vars?: string | null;
  sent?: boolean | null;
}

export interface MessageResponse {
  content: Message[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}
