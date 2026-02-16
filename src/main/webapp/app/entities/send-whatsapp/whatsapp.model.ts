import { SafeHtml } from '@angular/platform-browser';

export interface Button {
  type: string;
  text: string;
}

export interface Component {
  type: string;
  text?: string;
  format?: string;
  buttons?: Button[];
  example?: {
    body_text?: string[][];
  };
}

export interface Template {
  name: string;
  language: string;
  category: string;
  components: Component[];
}

export interface ButtonPayload {
  type: string;
  text: string;
  url?: string;
  phoneNumber?: string;
}

export interface ComponentPayload {
  type: string;
  format?: string;
  text?: string;
  safeText?: SafeHtml | string;
  mediaUrl?: string;
  fileName?: string;
  fileSize?: number;
  mimeType?: string;
  documentName?: string;
  buttons?: ButtonPayload[];
}

export interface TemplatePayload {
  name: string;
  language: string;
  category: string;
  components: ComponentPayload[];
}

// Interface pour la réponse du backend
export interface TemplateResponse {
  id: string;
  status: string;
  message?: string;
}

// Interface pour les erreurs
export interface ApiError {
  error: {
    error_user_msg: string;
    message?: string;
  };
}
