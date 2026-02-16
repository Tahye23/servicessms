import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

export interface StatsResponseDTO {
  typeStats: TypeStatsDTO[];
  abonnementSolde: number;
  totalConsomme: number;
  soldeRestant: number;
  total: number;
  totalFailed: number;
  totalPending: number;
}

export interface TypeStatsDTO {
  type: string;
  total: number;
  success: number;
  failed: number;
  pending: number;
  unitPrice: number;
}

export interface SubscriptionInfoDTO {
  subscriptionType: string;
  planName: string;
  canSendSMS: boolean;
  canSendWhatsApp: boolean;
  smsLimit: number;
  whatsappLimit: number;
  smsRemaining: number;
  whatsappRemaining: number;
  canManageTemplates: boolean;
  canViewAnalytics: boolean;
  canManageUsers: boolean;
  isExpiringSoon: boolean;
  daysUntilExpiration?: number;
  endDate?: string;
}

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);

  private resourceUrl = this.applicationConfigService.getEndpointFor('/api/subscription');

  /**
   * Récupère les statistiques par période
   */
  getStatsByDateRange(startDate?: string, endDate?: string): Observable<StatsResponseDTO> {
    let params = new HttpParams();

    if (startDate) {
      params = params.set('startDate', startDate);
    }

    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<StatsResponseDTO>(`${this.resourceUrl}/stats-by-range`, { params });
  }

  /**
   * Récupère les informations d'abonnement
   */
  getSubscriptionInfo(): Observable<SubscriptionInfoDTO> {
    return this.http.get<SubscriptionInfoDTO>(`${this.resourceUrl}/user/subscription-info`);
  }

  /**
   * Rafraîchit les données du dashboard
   */
  refreshDashboard(): Observable<any> {
    return this.http.post(`${this.resourceUrl}/user/refresh-subscription`, {});
  }
}
