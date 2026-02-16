import { ApplicationConfig, LOCALE_ID, importProvidersFrom, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {
  Router,
  RouterFeatures,
  TitleStrategy,
  provideRouter,
  withComponentInputBinding,
  withDebugTracing,
  withNavigationErrorHandler,
  NavigationError,
} from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';

import { DEBUG_INFO_ENABLED } from 'app/app.constants';
import './config/dayjs';
import { TranslationModule } from 'app/shared/language/translation.module';
import { httpInterceptorProviders } from './core/interceptor';
import routes from './app.routes';

// ===== NOUVEAUX IMPORTS POUR LE SYSTÈME D'ABONNEMENT =====
import { SubscriptionService } from './Subscription/service/subscriptionService.service';
import { DynamicMenuService } from './Subscription/service/dynamicMenuService.service';
import { SubscriptionNotificationService } from './Subscription/service/subscriptionNotificationService.service';
import { MessageCounterInterceptor } from './Subscription/service/MessageCounterInterceptor.interceptor';

import { NgbDateDayjsAdapter } from './config/datepicker-adapter';
import { AppPageTitleStrategy } from './app-page-title-strategy';

const routerFeatures: Array<RouterFeatures> = [
  withComponentInputBinding(),
  withNavigationErrorHandler((e: NavigationError) => {
    const router = inject(Router);
    if (e.error.status === 403) {
      router.navigate(['/accessdenied']);
    } else if (e.error.status === 404) {
      router.navigate(['/404']);
    } else if (e.error.status === 401) {
      // Ne pas rediriger si on est déjà sur la page publique (ex: '/')
      if (router.url === '/' || router.url.startsWith('/login') || router.url.startsWith('/account')) {
        // Ne rien faire, rester sur la page
        return;
      }
      router.navigate(['/login']);
    } else {
      router.navigate(['/error']);
    }
  }),
];

if (DEBUG_INFO_ENABLED) {
  routerFeatures.push(withDebugTracing());
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, ...routerFeatures),
    importProvidersFrom(BrowserAnimationsModule),
    importProvidersFrom(ServiceWorkerModule.register('ngsw-worker.js', { enabled: false })),
    importProvidersFrom(TranslationModule),
    importProvidersFrom(HttpClientModule),
    Title,
    { provide: LOCALE_ID, useValue: 'fr' },
    { provide: NgbDateAdapter, useClass: NgbDateDayjsAdapter },

    // ===== INTERCEPTEURS EXISTANTS + NOUVEAU =====
    ...httpInterceptorProviders,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MessageCounterInterceptor,
      multi: true,
    },

    // ===== SERVICES D'ABONNEMENT =====
    SubscriptionService,
    DynamicMenuService,
    SubscriptionNotificationService,

    { provide: TitleStrategy, useClass: AppPageTitleStrategy },
  ],
};
