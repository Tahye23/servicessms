import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApplicationConfigService } from '../../core/config/application-config.service';
import { Observable } from 'rxjs';

export interface MigrationResult {
  success: boolean;
  message: string;
  userLogin?: string;
  totalSendSms?: number;
  migrated?: number;
}

export interface GlobalMigrationResult {
  success: boolean;
  message: string;
  totalUsersProcessed?: number;
  totalSendSmsProcessed?: number;
  totalSmsUpdated?: number;
}

export interface QuotaInfo {
  success: boolean;
  userLogin: string;
  abonnements: Array<{
    id: number;
    planName: string;
    smsLimit: number;
    smsUsed: number;
    smsRemaining: number;
    whatsappLimit: number;
    whatsappUsed: number;
    whatsappRemaining: number;
    status: string;
    startDate: string;
    endDate: string;
  }>;
}

export interface UpdateQuotaResponse {
  success: boolean;
  userLogin: string;
  message: string;
  abonnements: Array<{
    id: number;
    planName: string;
    type: string;
    oldSmsLimit?: number;
    newSmsLimit?: number;
    oldWhatsappLimit?: number;
    newWhatsappLimit?: number;
    smsIncrease?: number;
    whatsappIncrease?: number;
  }>;
}

@Injectable({ providedIn: 'root' })
export class DataService {
  protected applicationConfigService = inject(ApplicationConfigService);
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/contacts');
  protected resourceUrlT = this.applicationConfigService.getEndpointFor('api/templates');
  protected resourceUrl2 = this.applicationConfigService.getEndpointFor('api/send-sms');
  protected baseUrl = this.applicationConfigService.getEndpointFor('api/whats-apps');
  protected resourceUrlGroupe = this.applicationConfigService.getEndpointFor('api/groupes');
  protected adminSendSmsUrl = this.applicationConfigService.getEndpointFor('api/admin/send-sms');
  protected adminAbonnementUrl = this.applicationConfigService.getEndpointFor('api/admin/abonnements');

  constructor(private http: HttpClient) {}

  // ==================== GESTION DES QUOTAS ====================

  /**
   * Consulter les quotas d'un utilisateur
   */
  viewUserQuota(userLogin: string): Observable<QuotaInfo> {
    return this.http.get<QuotaInfo>(`${this.adminAbonnementUrl}/view-quota/${userLogin}`);
  }

  /**
   * Mettre à jour (remplacer) les quotas d'un utilisateur
   */
  updateUserQuota(userLogin: string, newSmsLimit?: number, newWhatsappLimit?: number): Observable<UpdateQuotaResponse> {
    return this.http.post<UpdateQuotaResponse>(`${this.adminAbonnementUrl}/update-quota`, {
      userLogin,
      newSmsLimit,
      newWhatsappLimit,
    });
  }

  /**
   * Augmenter les quotas d'un utilisateur
   */
  increaseUserQuota(userLogin: string, smsIncrease?: number, whatsappIncrease?: number): Observable<UpdateQuotaResponse> {
    return this.http.post<UpdateQuotaResponse>(`${this.adminAbonnementUrl}/increase-quota`, {
      userLogin,
      smsIncrease,
      whatsappIncrease,
    });
  }

  // ==================== MIGRATION ====================

  /**
   * Migrer les user_login pour un utilisateur
   */
  migrateUserLogin(userLogin: string): Observable<MigrationResult> {
    return this.http.post<MigrationResult>(`${this.adminAbonnementUrl}/migrate-user`, {
      userLogin,
    });
  }

  /**
   * Migrer les user_login pour TOUS les utilisateurs
   */
  migrateAllUsers(): Observable<GlobalMigrationResult> {
    return this.http.post<GlobalMigrationResult>(`${this.adminAbonnementUrl}/migrate-all`, {});
  }

  /**
   * Recalcule l'abonnement d'un utilisateur
   */
  recalculateAbonnement(userLogin: string): Observable<any> {
    return this.http.post(`${this.adminAbonnementUrl}/recalculate`, { userLogin });
  }

  // ==================== MÉTHODES EXISTANTES ====================

  deleteGroupeWithContactsAndMessages(groupeId: number): Observable<any> {
    return this.http.delete(`${this.resourceUrlGroupe}/${groupeId}/with-contacts-and-messages`);
  }

  deleteSendSmsWithMessages(sendSmsId: number): Observable<any> {
    return this.http.delete(`${this.adminSendSmsUrl}/${sendSmsId}`);
  }

  deleteGroupeWithContacts(groupeId: number): Observable<any> {
    return this.http.delete(`${this.resourceUrlGroupe}/${groupeId}/with-contacts`);
  }

  deleteAllContacts() {
    return this.http.delete(`${this.resourceUrl}/delete-all`);
  }

  deleteAllTemplates() {
    return this.http.delete(`${this.resourceUrlT}/delete-all`);
  }

  deleteAllSms() {
    return this.http.delete(`${this.resourceUrl2}/delete-all`);
  }

  syncDeliveryStatus(sendSmsId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${sendSmsId}/sync-delivery-status`, {});
  }

  updateSendSmsStatus(sendSmsId: number): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${sendSmsId}/update-status`, {});
  }

  deleteSendSms(sendSmsId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/delete/${sendSmsId}`);
  }
}
