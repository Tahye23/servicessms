import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApplicationConfigService } from '../../core/config/application-config.service';

export interface ChannelConfigurationDTO {
  id?: number;
  channelType: 'WHATSAPP' | 'SMS' | 'EMAIL';
  userLogin?: string;
  host?: string;
  port?: number;
  username?: string;
  password?: string; // pour envoyer temporairement au backend pour chiffrement
  smsOperator?: 'Mattel' | 'Mauritel' | 'Chinguitel';
  encryptedPassword?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ChannelConfigService {
  private http = inject(HttpClient);
  private appConfigService = inject(ApplicationConfigService);
  private apiUrl = this.appConfigService.getEndpointFor('api/channels');

  saveConfig(cfg: ChannelConfigurationDTO, password: string): Observable<ChannelConfigurationDTO> {
    return this.http.post<ChannelConfigurationDTO>(`${this.apiUrl}?password=${password}`, cfg);
  }

  testConfig(cfg: ChannelConfigurationDTO, password: string): Observable<string> {
    let endpoint = '';
    switch (cfg.channelType) {
      case 'SMS':
        endpoint = 'test-sms';
        break;
      case 'EMAIL':
        endpoint = 'test-email';
        break;
      default:
        throw new Error('Test non supporté pour ce canal');
    }
    return this.http.post(`${this.apiUrl}/${endpoint}?password=${password}`, cfg, { responseType: 'text' });
  }

  // Récupérer la configuration existante pour un utilisateur et un canal
  getConfig(channelType: 'WHATSAPP' | 'SMS' | 'EMAIL'): Observable<ChannelConfigurationDTO> {
    return this.http.get<ChannelConfigurationDTO>(`${this.apiUrl}/${channelType.toLowerCase()}`);
  }
}
