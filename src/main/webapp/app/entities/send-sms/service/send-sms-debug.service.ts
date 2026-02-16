// send-sms.service.ts - VERSION CORRIGÉE
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { BehaviorSubject, catchError, interval, map, Observable, of, tap } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { switchMap } from 'rxjs/operators';

export interface BulkProgressResponse {
  bulkId: string;
  sendSmsId: number;
  totalRecipients: number;
  stats: {
    total: number;
    inserted: number;
    sent: number;
    delivered: number;
    read: number;
    failed: number;
    deliveryFailed: number;
    pending: number;
    processed: number;
    successful: number;
    successRate: number;
  };
  insertionProgress: number;
  sendProgress: number;
  insertionComplete: boolean;
  currentRate: number;
  elapsedSeconds: number;
  etaInsertSeconds: number;
  etaSendSeconds: number;
  inProcess: boolean;
  lastUpdate: string;
  error?: string;
}

@Injectable({ providedIn: 'root' })
export class SendSmsDebugService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/send-sms');

  refresh(sendSmsId: number): Observable<any> {
    return this.http.post<any>(`${this.resourceUrl}/${sendSmsId}/refresh`, null);
  }
}
