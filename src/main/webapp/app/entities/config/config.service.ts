// config.service.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Config } from '@fortawesome/fontawesome-svg-core';
import { ApplicationConfigService } from '../../core/config/application-config.service';
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
@Injectable({
  providedIn: 'root',
})
export class ConfigService {
  private applicationConfigService = inject(ApplicationConfigService);
  protected apiUrl = this.applicationConfigService.getEndpointFor('api/configuration');
  constructor(private http: HttpClient) {}

  /** Récupère la config du user courant */
  getMetaConfig(): Observable<PartnerConfigurationDTO> {
    return this.http.get<PartnerConfigurationDTO>(`${this.apiUrl}/partner`);
  }

  createForPartner(userLogin: string, accessToken: string): Observable<PartnerConfigurationDTO> {
    return this.http.post<PartnerConfigurationDTO>(`${this.apiUrl}/partner`, { userLogin, accessToken });
  }
  saveMetaConfig(config: PartnerConfigurationDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/partner`, config);
  }
  enableCoexistence(phoneNumberId: string, accessToken: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/meta/coexistence`, { phoneNumberId, accessToken });
  }

  /** Teste la configuration auprès de Meta */
  testMetaConfig(config: PartnerConfigurationDTO): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/partner/test`, config);
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
  getAllConfigurations(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/all`);
  }
}
