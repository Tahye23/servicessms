import { Component, inject, Renderer2, RendererFactory2 } from '@angular/core';
import { NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault, registerLocaleData } from '@angular/common';
import dayjs from 'dayjs/esm';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import locale from '@angular/common/locales/fr';
// jhipster-needle-angular-add-module-import JHipster will add new module here

import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { fontAwesomeIcons } from './config/font-awesome-icons';
import MainComponent from './layouts/main/main.component';
import HomeComponent from './home/home.component';
import { Router, RouterOutlet } from '@angular/router';
import LoginComponent from './login/login.component';

@Component({
  standalone: true,
  selector: 'jhi-app',
  template: `
    <ng-container *ngIf="useRouterOutlet(); else customLayout">
      <router-outlet></router-outlet>
    </ng-container>
    <ng-template #customLayout>
      <jhi-main></jhi-main>
    </ng-template>
  `,
  imports: [MainComponent, NgIf, HomeComponent, LoginComponent, NgSwitch, NgSwitchCase, NgSwitchDefault, RouterOutlet],
})
export default class AppComponent {
  private applicationConfigService = inject(ApplicationConfigService);
  private iconLibrary = inject(FaIconLibrary);
  private dpConfig = inject(NgbDatepickerConfig);
  private router = inject(Router);
  private renderer: Renderer2;
  private rootRenderer = inject(RendererFactory2);
  useRouterOutlet(): boolean {
    const url = this.router.url;
    return url === '/' || url.startsWith('/login') || url.startsWith('/account') || url.startsWith('/boot') || url.startsWith('/flow');
  }
  constructor() {
    this.applicationConfigService.setEndpointPrefix(SERVER_API_URL);
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
    registerLocaleData(locale);
    this.iconLibrary.addIcons(...fontAwesomeIcons);
    this.dpConfig.minDate = { year: dayjs().subtract(100, 'year').year(), month: 1, day: 1 };
  }
}
