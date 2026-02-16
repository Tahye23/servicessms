import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { SubscriptionService } from './subscriptionService.service';

@Injectable()
export class MessageCounterInterceptor implements HttpInterceptor {
  private subscriptionService = inject(SubscriptionService);

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          // Décrémenter le compteur après envoi réussi
          if (req.url.includes('/api/send-sms') && event.status === 200) {
            const messageCount = this.extractMessageCount(req.body);
            this.subscriptionService.updateCounter('SMS', messageCount).subscribe();
          }

          if (req.url.includes('/api/send-whatsapp') && event.status === 200) {
            const messageCount = this.extractMessageCount(req.body);
            this.subscriptionService.updateCounter('WHATSAPP', messageCount).subscribe();
          }
        }
      }),
    );
  }

  private extractMessageCount(body: any): number {
    // Extraire le nombre de messages du body de la requête
    if (body?.recipients?.length) {
      return body.recipients.length;
    }
    return 1; // Par défaut, 1 message
  }
}
