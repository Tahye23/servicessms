// config.service.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApplicationConfigService } from '../../core/config/application-config.service';
import { ChannelType } from './channel.types';

export interface PartnerConfigurationDTO {
  id: number;
  userLogin: string;
  accessToken: string;
  businessId: string;
  phoneNumberId: string;
  appId: string;
  verified: boolean;
  isValid: boolean;
  application: string;
  updatedAt: string;
}

export interface ChannelConfigurationDTO {
  id?: number;
  channelType: 'WHATSAPP' | 'SMS' | 'EMAIL';
  userLogin?: string;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  smsOperator?: 'Mattel' | 'Mauritel' | 'Chinguitel';
  encryptedPassword?: string;
  verified?: boolean;
  extraInfo?: string;
}

export interface UnifiedConfigurationDTO {
  channel: ChannelType;
  id: number;
  verified?: boolean;
  username?: string;
  host?: string;
  port?: number;
  extraInfo?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private applicationConfigService = inject(ApplicationConfigService);
  protected apiUrl = this.applicationConfigService.getEndpointFor('api/configuration');
  private http = inject(HttpClient);

  // 🔥 État réactif pour les configurations (toutes les données en mémoire)
  private configurationsSubject = new BehaviorSubject<UnifiedConfigurationDTO[]>([]);
  public configurations$ = this.configurationsSubject.asObservable();

  constructor() {
    this.loadAllConfigurations();
  }

  /**
   * Charge toutes les configurations depuis le serveur
   */
  loadAllConfigurations(): void {
    this.getAllConfigurations().subscribe({
      next: configs => this.configurationsSubject.next(configs),
      error: err => console.error('Erreur chargement configs', err),
    });
  }

  /**
   * Retourne l'état actuel en mémoire
   */
  getConfigurationsSnapshot(): UnifiedConfigurationDTO[] {
    return this.configurationsSubject.getValue();
  }

  /**
   * Ajoute ou met à jour une config en mémoire
   */
  updateConfigInMemory(config: UnifiedConfigurationDTO): void {
    const current = this.configurationsSubject.getValue();

    const updated = current.some(c => c.id === config.id && c.channel === config.channel)
      ? current.map(c => (c.id === config.id && c.channel === config.channel ? config : c))
      : [...current, config];

    this.configurationsSubject.next(updated);
  }

  removeConfigFromMemory(id: number, channel: string): void {
    const current = this.configurationsSubject.getValue();
    const filtered = current.filter(c => !(c.id === id && c.channel === channel));
    this.configurationsSubject.next(filtered);
  }

  // ========== META WHATSAPP ==========

  getMetaConfig(): Observable<PartnerConfigurationDTO> {
    return this.http.get<PartnerConfigurationDTO>(`${this.apiUrl}/partner`);
  }

  saveMetaConfig(config: PartnerConfigurationDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/partner`, config);
  }

  testMetaConfig(config: PartnerConfigurationDTO): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/partner/test`, config);
  }

  enableCoexistence(phoneNumberId: string, accessToken: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/meta/coexistence`, {
      phoneNumberId,
      accessToken,
    });
  }

  finalizeSignup(shortLivedToken: string, webhookCallbackUrl: string): Observable<PartnerConfigurationDTO> {
    return this.http.post<PartnerConfigurationDTO>(`${this.apiUrl}/meta-signup`, {
      accessToken: shortLivedToken,
      webhookCallbackUrl,
    });
  }

  getAdminAppId(): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/admin/app-id`);
  }

  // ========== TOUTES LES CONFIGURATIONS ==========

  getAllConfigurations(): Observable<UnifiedConfigurationDTO[]> {
    return this.http.get<UnifiedConfigurationDTO[]>(`${this.apiUrl}/all`).pipe(
      tap(configs => this.configurationsSubject.next(configs)),
      catchError(err => {
        console.error('Erreur getAllConfigurations', err);
        return throwError(() => err);
      }),
    );
  }
}
