import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { faComment } from '@fortawesome/free-solid-svg-icons';

import { StateStorageService } from 'app/core/auth/state-storage.service';
import SharedModule from 'app/shared/shared.module';
import HasAnyAuthorityDirective from 'app/shared/auth/has-any-authority.directive';
import { VERSION } from 'app/app.constants';
import { LANGUAGES } from 'app/config/language.constants';
import { AccountService } from 'app/core/auth/account.service';
import { LoginService } from 'app/login/login.service';
import { ProfileService } from 'app/layouts/profiles/profile.service';
import ActiveMenuDirective from './active-menu.directive';
import { MegaMenuModule } from 'primeng/megamenu';
import { MegaMenuItem, MenuItem } from 'primeng/api';
import { StepsModule } from 'primeng/steps';
import { Account } from 'app/core/auth/account.model';
import { IconProp } from '@fortawesome/fontawesome-svg-core';
// import { AccountService } from 'app/core/auth/account.service';

@Component({
  standalone: true,
  selector: 'jhi-navbar',
  templateUrl: './navbar.component.html',

  imports: [RouterModule, SharedModule, HasAnyAuthorityDirective, MegaMenuModule, ActiveMenuDirective, StepsModule],
})
export default class NavbarComponent implements OnInit {
  inProduction?: boolean;
  faComment = faComment;
  items = computed((): MegaMenuItem[] => {
    const admin = this.isAdmin;
    return [
      {
        label: 'Accueil',
        icon: 'pi pi-home',
        routerLink: ['/dashbord'],
        styleClass: 'menuContclass',
      },
      {
        label: 'SMS',
        styleClass: 'menuitemclass',
        icon: 'pi pi-comments',
        routerLink: ['/send-sms'],
      },
      {
        label: 'Whatsapp',
        styleClass: 'menuitemclass',
        icon: 'pi pi-comments',

        routerLink: ['/send-whatsapp'],
      },
      {
        label: 'Conversations',
        visible: !admin,
        styleClass: 'menuitemclass',
        icon: ' pi pi-comments',
        routerLink: ['/conversations'],
      },
      {
        label: 'Modèles',
        styleClass: 'menuitemclass',
        icon: 'pi pi-file',
        routerLink: ['/template'],
      },

      {
        label: 'Destinataires',
        icon: '',
        items: [
          [
            {
              items: [
                { label: 'Destinataire', routerLink: ['/contact'] },
                { label: ' groupe contact', routerLink: ['/groupe'] },
              ],
            },
          ],
        ],
      },

      {
        label: 'À propos',
        styleClass: 'menuitemclass',
        icon: ' pi pi-key',
        routerLink: ['/'],
      },
      {
        label: 'Services',
        // L'item Services n'est affiché que si l'utilisateur est admin.
        visible: admin,
        icon: '',
        items: [
          [
            {
              items: [
                { label: 'Sending Sms', styleClass: 'menuitemclass', routerLink: ['/send-sms'] },
                { label: 'Nettoyage', styleClass: 'menuitemclass', routerLink: ['/contact'] },
                { label: 'OTP', routerLink: ['/otp-storage'] },
                { label: 'Conversations', routerLink: ['/conversations'] },
              ],
            },
          ],
        ],
      },
      {
        label: 'Configuration',
        styleClass: 'menuitemclass',
        icon: ' pi pi-key',
        routerLink: ['/config'],
      },
      {
        label: 'Tokens',
        styleClass: 'menuitemclass',
        icon: ' pi pi-key',
        routerLink: ['/tokens-app'],
      },
      {
        label: 'Autres',
        visible: admin,
        items: [
          [
            {
              items: [
                { label: 'Configuration', routerLink: ['/config'] },

                { label: 'Company', routerLink: ['/company'] },
                { label: 'Abonnement', styleClass: 'menuitemclass', routerLink: ['/abonnement'] },
                { label: 'Plan abonnement', routerLink: ['/planabonnement'] },
                { label: 'TokensApp', routerLink: ['/tokens-app'] },
                { label: 'Application', routerLink: ['/application'] },
                { label: 'Utilisateurs', routerLink: ['/extended-user'] },
                { label: 'Service', routerLink: ['/service'] },
                { label: 'Api', routerLink: ['/sapi'] },
                { label: 'User Service', routerLink: ['/user-service'] },
                { label: 'User token api', routerLink: ['/user-token-api'] },
                { label: 'participant', routerLink: ['/participant'] },
                { label: 'choix', routerLink: ['/choix'] },
                { label: 'question', routerLink: ['/question'] },
                { label: 'reponse', routerLink: ['/reponse'] },
                { label: 'conversation', routerLink: ['/conversation'] },
              ],
            },
          ],
        ],
      },
      {
        label: 'Administration',
        visible: admin,
        icon: '',
        items: [
          [
            {
              items: [
                {
                  label: 'authority',
                  routerLink: ['/authority'],
                  icon: 'pi pi-key',
                  styleClass: 'menuitemclass',
                },
                {
                  label: 'Gestion des utilisateurs',
                  icon: 'pi pi-users',
                  routerLink: ['/admin/user-management'],
                },
                { label: 'Métriques', routerLink: ['/admin/metrics'] },
                { label: 'Diagnostics', routerLink: ['/admin/health'] },
                { label: 'Configuration', routerLink: ['/admin/configuration'] },
                { label: 'Logs', routerLink: ['/admin/logs'] },
                { label: 'API', routerLink: ['/admin/docs'] },
              ],
            },
          ],
        ],
      },
    ];
  });
  isNavbarCollapsed = signal(true);
  languages = LANGUAGES;
  openDropdownIndex: number | null = null;
  openAPIEnabled?: boolean;
  version = '';
  account = inject(AccountService).trackCurrentAccount();
  //entitiesNavbarItems: NavbarItem[] = [];
  private loginService = inject(LoginService);
  private translateService = inject(TranslateService);
  private stateStorageService = inject(StateStorageService);
  private profileService = inject(ProfileService);
  private router = inject(Router);
  public isAccountMenuOpen = false;
  items2!: MegaMenuItem[] | undefined;
  items3: MenuItem[] = [];
  itemss: MenuItem[] = [];
  account1: Account | null = null;
  mobileMenuOpen = false;
  constructor(private accountService: AccountService) {
    if (VERSION) {
      this.version = VERSION.toLowerCase().startsWith('v') ? VERSION : `v${VERSION}`;
    }
  }
  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }
  get isAdmin(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account ? account.authorities.includes('ROLE_ADMIN') : false;
  }

  ngOnInit(): void {
    this.accountService.getAuthenticationState().subscribe(account => {
      this.account1 = account;
      console.log('account', account!.id);
    });
    console.log('isAdmin', this.isAdmin);
    //this.entitiesNavbarItems = EntityNavbarItems;
    // this.getItems();
  }

  changeLanguage(languageKey: string): void {
    this.stateStorageService.storeLocale(languageKey);
    this.translateService.use(languageKey);
  }

  collapseNavbar(): void {
    this.isNavbarCollapsed.set(true);
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.collapseNavbar();
    this.loginService.logout();
    this.router.navigate(['']);
  }

  toggleNavbar(): void {
    this.isNavbarCollapsed.update(isNavbarCollapsed => !isNavbarCollapsed);
  }
  getIcon(item: any): IconProp {
    return ['fas', item.icon] as unknown as IconProp;
  }
  toggleAccountMenu(): void {
    this.isAccountMenuOpen = !this.isAccountMenuOpen;
  }
  toggleDropdown(index: number): void {
    this.openDropdownIndex = this.openDropdownIndex === index ? null : index;
  }
  closeDropdown(): void {
    this.openDropdownIndex = null;
  }
}
