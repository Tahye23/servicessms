import { inject, Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

import { LoginService } from 'app/login/login.service';
import { StateStorageService } from 'app/core/auth/state-storage.service';

@Injectable()
export class AuthExpiredInterceptor implements HttpInterceptor {
  private loginService = inject(LoginService);
  private stateStorageService = inject(StateStorageService);
  private router = inject(Router);

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap({
        error: (err: HttpErrorResponse) => {
          const publicUrls = ['/', '/login', '/account/register', '/request/']; // adapte selon ton app
          const currentUrl = this.router.routerState.snapshot.url;

          if (err.status === 401 && err.url && !err.url.includes('api/account')) {
            if (!publicUrls.includes(currentUrl)) {
              this.stateStorageService.storeUrl(currentUrl);
              this.loginService.logout();
              this.router.navigate(['/login']);
            }
            // Sinon on ne fait rien, on laisse l'utilisateur rester sur la page publique
          }
        },
      }),
    );
  }
}
