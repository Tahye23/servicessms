import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { BehaviorSubject, catchError, distinctUntilChanged, interval, map, Observable, of } from 'rxjs';

import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { BulkProgress, ISendSms, NewSendSms, PageSms, Sms } from '../send-sms.model';
import { filter, shareReplay, startWith, switchMap } from 'rxjs/operators';

export type PartialUpdateSendSms = Partial<ISendSms> & Pick<ISendSms, 'id'>;

type RestOf<T extends ISendSms | NewSendSms> = Omit<T, 'sendateEnvoi'> & {
  sendateEnvoi?: string | null;
};
export interface SmsPageResponse {
  content: any[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  globalStats: {
    total: number;
    delivered: number;
    failed: number;
    pending: number;
    read: number;
    sent: number;
  };
}
export interface SendSmsResponse {
  bulkId: string;
  totalRecipients: number;
  templateExiste: boolean;
  iSent: boolean;
}
export interface MonitoringStats {
  totalProcessed: number;
  totalSuccess: number;
  totalFailed: number;
  activeThreads: number;
  successRate: number;
  targetRatePerSecond: number;
  circuitBreakerOpen: boolean;
  currentQueueSize: number;
  activeThreadsPool: number;
}
export interface SmsFilterParams {
  page: number;
  size: number;
  search?: string;
  deliveryStatus?: string;
  dateFrom?: string;
  dateTo?: string;
}
export interface BulkStats {
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
  deliveryRate: number;
  readRate: number;
}

export interface BulkProgressResponse {
  bulkId: string;
  sendSmsId: number;
  totalRecipients: number;
  stats: BulkStats;
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

export interface RealtimeMetrics {
  currentRate: number;
  averageRate: number;
  peakRate: number;
  efficiency: number;
  estimatedCompletion: Date | null;
  errorRate: number;
  throughputHistory: number[];
  timeLabels: string[];
}
export type RestSendSms = RestOf<ISendSms>;

export type NewRestSendSms = RestOf<NewSendSms>;

export type PartialUpdateRestSendSms = RestOf<PartialUpdateSendSms>;

export type EntityResponseType = HttpResponse<ISendSms>;
export type EntityArrayResponseType = HttpResponse<ISendSms[]>;

@Injectable({ providedIn: 'root' })
export class SendSmsService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/send-sms');
  protected resourceUrlSms = this.applicationConfigService.getEndpointFor('api/sms');
  sendToGroupTest(sendSmsId: number, groupId: number): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/${sendSmsId}/send-test`, null, {
      params: { testGroupId: groupId.toString() },
    });
  }
  // Dans send-sms.service.ts, ajoutez cette méthode :

  /**
   * Envoie un SMS/WhatsApp unitaire par son ID
   */
  sendSingleSms(smsId: number, test: boolean = false): Observable<any> {
    const params = new HttpParams().set('test', test.toString());

    return this.http.post<any>(`${this.resourceUrl}/sms/${smsId}/send`, null, { params }).pipe(
      catchError(error => {
        console.error(' Erreur envoi SMS:', error);
        throw error;
      }),
    );
  }
  downloadSmsContacts(bulkId: number, params: any): Observable<Blob> {
    const cleanParams: { [key: string]: string } = {};

    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        cleanParams[key] = String(value);
      }
    });

    return this.http.get(`${this.resourceUrl}/sms/by-bulk/${bulkId}/export/csv`, {
      params: cleanParams,
      responseType: 'blob',
    });
  }

  getBulkProgressByBulkId(bulkId: string): Observable<BulkProgressResponse> {
    return this.http.get<BulkProgressResponse>(`${this.resourceUrl}/sms/bulk-progress/${bulkId}`).pipe(
      catchError(error => {
        console.error('Erreur récupération progression bulk:', error);
        return of({
          error: 'Erreur de récupération des données',
          bulkId,
          totalRecipients: 0,
          stats: this.getEmptyStats(),
          insertionProgress: 0,
          sendProgress: 0,
          insertionComplete: false,
          currentRate: 0,
          elapsedSeconds: 0,
          etaInsertSeconds: -1,
          etaSendSeconds: -1,
          inProcess: false,
          lastUpdate: new Date().toISOString(),
        } as BulkProgressResponse);
      }),
    );
  }

  getBulkProgressBySendSmsId(sendSmsId: number): Observable<BulkProgressResponse> {
    return this.http.get<BulkProgressResponse>(`${this.resourceUrl}/sms/bulk-progress/${sendSmsId}`).pipe(
      map(response => {
        console.log(' [SERVICE] Progression reçue:', response);
        return response;
      }),
      catchError(error => {
        console.error(' [SERVICE] Erreur récupération progression:', error);
        // Retourner une structure d'erreur plus détaillée
        return of({
          error: `Erreur ${error.status}: ${error.message}`,
          sendSmsId,
          totalRecipients: 0,
          stats: this.getEmptyStats(),
          insertionProgress: 0,
          sendProgress: 0,
          insertionComplete: false,
          currentRate: 0,
          elapsedSeconds: 0,
          etaInsertSeconds: -1,
          etaSendSeconds: -1,
          inProcess: false,
          lastUpdate: new Date().toISOString(),
        } as BulkProgressResponse);
      }),
    );
  }

  stopBulkSending(sendSmsId: number): Observable<any> {
    return this.http.post(`${this.resourceUrl}/${sendSmsId}/stop`, {}).pipe(
      catchError(error => {
        console.error('Erreur arrêt envoi:', error);
        throw error;
      }),
    );
  }

  getBulkProgressStream(identifier: string | number, type: 'bulkId' | 'sendSmsId' = 'sendSmsId'): Observable<BulkProgressResponse> {
    console.log(` [SERVICE] Début monitoring ${type}:`, identifier);

    const source$ =
      type === 'bulkId' ? this.getBulkProgressByBulkId(identifier as string) : this.getBulkProgressBySendSmsId(identifier as number);

    return interval(1000).pipe(
      // Ralenti à 1 seconde pour éviter spam
      startWith(0),
      switchMap(() => {
        console.log(` [SERVICE] Polling ${type}: ${identifier}`);
        return source$;
      }),
      distinctUntilChanged((prev, curr) => {
        const unchanged =
          prev.stats?.sent === curr.stats?.sent &&
          prev.stats?.failed === curr.stats?.failed &&
          prev.stats?.inserted === curr.stats?.inserted &&
          prev.inProcess === curr.inProcess;

        if (!unchanged) {
          console.log(` [SERVICE] Changement détecté:`, {
            sent: `${prev.stats?.sent} -> ${curr.stats?.sent}`,
            failed: `${prev.stats?.failed} -> ${curr.stats?.failed}`,
            inserted: `${prev.stats?.inserted} -> ${curr.stats?.inserted}`,
            inProcess: `${prev.inProcess} -> ${curr.inProcess}`,
          });
        }

        return unchanged;
      }),
      shareReplay(1),
    );
  }
  isProcessComplete(progress: BulkProgressResponse): boolean {
    if (progress.error) {
      console.log(' [SERVICE] Processus interrompu par erreur:', progress.error);
      return true;
    }

    const isComplete =
      !progress.inProcess &&
      progress.stats.inserted >= progress.totalRecipients &&
      progress.stats.sent + progress.stats.failed >= progress.stats.inserted;

    if (isComplete) {
      console.log(' [SERVICE] Processus terminé:', {
        totalRecipients: progress.totalRecipients,
        inserted: progress.stats.inserted,
        sent: progress.stats.sent,
        failed: progress.stats.failed,
        inProcess: progress.inProcess,
      });
    }

    return isComplete;
  }

  /**
   * Calcule les métriques temps réel
   */
  calculateRealtimeMetrics(currentProgress: BulkProgressResponse, history: BulkProgressResponse[]): RealtimeMetrics {
    const rates = history.map(h => h.currentRate).filter(r => r > 0);

    return {
      currentRate: currentProgress.currentRate,
      averageRate: rates.length > 0 ? rates.reduce((a, b) => a + b) / rates.length : 0,
      peakRate: rates.length > 0 ? Math.max(...rates) : 0,
      efficiency: currentProgress.currentRate > 0 ? (currentProgress.currentRate / 10) * 100 : 0, // 10 msg/sec = 100%
      estimatedCompletion: this.calculateEstimatedCompletion(currentProgress),
      errorRate: currentProgress.stats.processed > 0 ? (currentProgress.stats.failed / currentProgress.stats.processed) * 100 : 0,
      throughputHistory: rates.slice(-30), // Dernières 30 mesures
      timeLabels: history.slice(-30).map(h =>
        new Date(h.lastUpdate).toLocaleTimeString('fr-FR', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        }),
      ),
    };
  }

  private calculateEstimatedCompletion(progress: BulkProgressResponse): Date | null {
    if (progress.currentRate <= 0) return null;

    const remaining = progress.totalRecipients - progress.stats.sent;
    const secondsRemaining = remaining / progress.currentRate;

    return new Date(Date.now() + secondsRemaining * 1000);
  }

  private getEmptyStats(): BulkStats {
    return {
      total: 0,
      inserted: 0,
      sent: 0,
      delivered: 0,
      read: 0,
      failed: 0,
      deliveryFailed: 0,
      pending: 0,
      processed: 0,
      successful: 0,
      successRate: 0,
      deliveryRate: 0,
      readRate: 0,
    };
  }
  refresh(sendSmsId: number): Observable<ISendSms> {
    return this.http.post<ISendSms>(`${this.resourceUrl}/${sendSmsId}/refresh`, null);
  }

  create(sendSms: NewSendSms): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sendSms);
    return this.http
      .post<RestSendSms>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(sendSms: ISendSms): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sendSms);
    return this.http
      .put<RestSendSms>(`${this.resourceUrl}/${this.getSendSmsIdentifier(sendSms)}`, sendSms, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(sendSms: PartialUpdateSendSms): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(sendSms);
    return this.http
      .patch<RestSendSms>(`${this.resourceUrl}/${this.getSendSmsIdentifier(sendSms)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestSendSms>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }
  getSendSmsById(id: number): Observable<ISendSms> {
    return this.http.get<ISendSms>(`${this.resourceUrl}/${id}`);
  }
  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestSendSms[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  getSendSmsIdentifier(sendSms: Pick<ISendSms, 'id'>): number {
    return sendSms.id;
  }

  compareSendSms(o1: Pick<ISendSms, 'id'> | null, o2: Pick<ISendSms, 'id'> | null): boolean {
    return o1 && o2 ? this.getSendSmsIdentifier(o1) === this.getSendSmsIdentifier(o2) : o1 === o2;
  }

  addSendSmsToCollectionIfMissing<Type extends Pick<ISendSms, 'id'>>(
    sendSmsCollection: Type[],
    ...sendSmsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const sendSms: Type[] = sendSmsToCheck.filter(isPresent);
    if (sendSms.length > 0) {
      const sendSmsCollectionIdentifiers = sendSmsCollection.map(sendSmsItem => this.getSendSmsIdentifier(sendSmsItem));
      const sendSmsToAdd = sendSms.filter(sendSmsItem => {
        const sendSmsIdentifier = this.getSendSmsIdentifier(sendSmsItem);
        if (sendSmsCollectionIdentifiers.includes(sendSmsIdentifier)) {
          return false;
        }
        sendSmsCollectionIdentifiers.push(sendSmsIdentifier);
        return true;
      });
      return [...sendSmsToAdd, ...sendSmsCollection];
    }
    return sendSmsCollection;
  }

  protected convertDateFromClient<T extends ISendSms | NewSendSms | PartialUpdateSendSms>(sendSms: T): RestOf<T> {
    return {
      ...sendSms,
      sendateEnvoi: sendSms.sendateEnvoi?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restSendSms: RestSendSms): ISendSms {
    return {
      ...restSendSms,
      sendateEnvoi: restSendSms.sendateEnvoi ? dayjs(restSendSms.sendateEnvoi) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestSendSms>): HttpResponse<ISendSms> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestSendSms[]>): HttpResponse<ISendSms[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }

  sendSmsM(id: number, options?: { params: HttpParams }): Observable<SendSmsResponse> {
    return this.http.post<SendSmsResponse>(`${this.resourceUrl}/send/${id}`, {}, options);
  }

  getSmsByBulkId(bulkId: number, params: SmsFilterParams): Observable<SmsPageResponse> {
    let httpParams = new HttpParams().set('page', params.page.toString()).set('size', params.size.toString());

    if (params.search?.trim()) httpParams = httpParams.set('search', params.search.trim());
    if (params.deliveryStatus?.trim()) httpParams = httpParams.set('deliveryStatus', params.deliveryStatus.trim());
    if (params.dateFrom?.trim()) httpParams = httpParams.set('dateFrom', params.dateFrom.trim());
    if (params.dateTo?.trim()) httpParams = httpParams.set('dateTo', params.dateTo.trim());

    return this.http.get<SmsPageResponse>(`${this.resourceUrl}/sms/by-bulk/${bulkId}`, { params: httpParams });
  }
  getSmsById(smsId: number): Observable<Sms> {
    return this.http.get<Sms>(`${this.resourceUrlSms}/${smsId}`);
  }
  getTotalSendMSCount(): Observable<number> {
    return this.http.get<number>(`${this.resourceUrl}/count`);
  }

  debugMonitoring(sendSmsId: number): Observable<any> {
    console.log(' [SERVICE] Debug monitoring pour sendSmsId:', sendSmsId);

    return this.http.get(`${this.resourceUrl}/${sendSmsId}`, { observe: 'response' }).pipe(
      map(response => {
        console.log(' [SERVICE] Réponse complète:', {
          status: response.status,
          headers: response.headers.keys(),
          body: response.body,
        });
        return response.body;
      }),
      catchError(error => {
        console.error(' [SERVICE] Erreur debug:', {
          status: error.status,
          statusText: error.statusText,
          url: error.url,
          message: error.message,
        });
        throw error;
      }),
    );
  }
  testConnectivity(): Observable<boolean> {
    return this.http.get(`${this.resourceUrl}/count`).pipe(
      map(() => {
        console.log(' [SERVICE] Connectivité OK');
        return true;
      }),
      catchError(error => {
        console.error(' [SERVICE] Problème de connectivité:', error);
        return of(false);
      }),
    );
  }
}
