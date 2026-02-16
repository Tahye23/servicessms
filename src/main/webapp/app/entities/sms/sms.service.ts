import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { Sms } from '../send-sms/send-sms.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SmsService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/sms');

  constructor() {}

  createSms(sms: Sms): Observable<Sms> {
    return this.http.post<Sms>(`${this.resourceUrl}/create`, sms);
  }
}
