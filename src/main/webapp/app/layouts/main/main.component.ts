import { Component, inject, OnInit, RendererFactory2, Renderer2, HostListener, computed } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { TranslateService, LangChangeEvent } from '@ngx-translate/core';
import dayjs from 'dayjs/esm';

import { AccountService } from 'app/core/auth/account.service';
import { AppPageTitleStrategy } from 'app/app-page-title-strategy';
import FooterComponent from '../footer/footer.component';
import PageRibbonComponent from '../profiles/page-ribbon.component';
import { MegaMenuModule } from 'primeng/megamenu';
import { MegaMenuItem } from 'primeng/api';
import { ToastComponent } from '../../entities/toast/toast.component';
import { filter } from 'rxjs/operators';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { LoginService } from '../../login/login.service';

// ===== NOUVEAUX IMPORTS POUR LE SYSTÈME D'ABONNEMENT =====
import { SubscriptionService } from '../../Subscription/service/subscriptionService.service';
import { DynamicMenuService } from '../../Subscription/service/dynamicMenuService.service';
import { SubscriptionNotificationService } from '../../Subscription/service/subscriptionNotificationService.service';

// Composants d'abonnement
import { SubscriptionStatusComponent } from '../../Subscription/subscriptionStatusComponent.component';
import { UpgradeSuggestionComponent } from '../../Subscription/upgradeSuggestionComponent.component';

@Component({
  standalone: true,
  selector: 'jhi-main',
  templateUrl: './main.component.html',
  styleUrls: ['./main.component.scss'],
  providers: [AppPageTitleStrategy],
  imports: [
    RouterOutlet,
    FooterComponent,
    MegaMenuModule,
    PageRibbonComponent,
    ToastComponent,
    NgClass,
    NgIf,
    NgForOf,
    // ===== NOUVEAUX COMPOSANTS =====
    SubscriptionStatusComponent,
    UpgradeSuggestionComponent,
  ],
})
export default class MainComponent implements OnInit {
  private renderer: Renderer2;
  isAccountMenuOpen = false;
  private loginService = inject(LoginService);

  // ===== SERVICES D'ABONNEMENT =====
  private subscriptionService = inject(SubscriptionService);
  private dynamicMenuService = inject(DynamicMenuService);
  private notificationService = inject(SubscriptionNotificationService);

  // ===== NOUVEAU MENU DYNAMIQUE BASÉ SUR LES ABONNEMENTS =====
  items = computed(() => this.dynamicMenuService.items());

  private router = inject(Router);
  private appPageTitleStrategy = inject(AppPageTitleStrategy);
  private accountService = inject(AccountService);
  private translateService = inject(TranslateService);
  private rootRenderer = inject(RendererFactory2);
  account = inject(AccountService).trackCurrentAccount();
  constructor() {
    this.renderer = this.rootRenderer.createRenderer(document.querySelector('html'), null);
    console.log('account', this.account);
  }

  ngOnInit(): void {
    // ===== INITIALISATION DU SYSTÈME D'ABONNEMENT =====

    // 1. Charger les abonnements quand l'utilisateur est connecté
    this.accountService.identity().subscribe(account => {
      if (account) {
        this.subscriptionService.loadUserSubscriptions().subscribe({
          next: () => {
            console.log('✅ Abonnements chargés', this.subscriptionService.subscriptionAccess());
            this.dynamicMenuService.initializeMenu();
            this.checkSubscriptionNotifications();
          },
          error: error => {
            console.error('❌ Erreur lors du chargement des abonnements:', error);
            // En cas d'erreur, utiliser les accès gratuits par défaut
            this.useDefaultFreeAccess();
          },
        });
      }
    });

    // 2. Navigation handling (existant)
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      window.scrollTo(0, 0);
    });
    this.checkScreenSize();

    // 3. Vérifications périodiques des notifications
    setInterval(() => {
      this.checkSubscriptionNotifications();
    }, 300000); // Toutes les 5 minutes
  }

  /**
   * 🔔 Vérifie et affiche les notifications d'abonnement
   */
  private checkSubscriptionNotifications(): void {
    if (this.account()) {
      this.notificationService.checkAndShowNotifications();
    }
  }

  /**
   * 🆓 Utilise les accès gratuits par défaut en cas d'erreur
   */
  private useDefaultFreeAccess(): void {
    console.log('🆓 Utilisation des accès gratuits par défaut');
    // Le service gérera automatiquement les accès gratuits
    this.dynamicMenuService.initializeMenu();
  }

  // ===== PROPRIÉTÉS ET MÉTHODES EXISTANTES (INCHANGÉES) =====

  sidebarOpen = true;
  submenuOpenIndex: number | null = null;
  hoverItemIndex: number | null = null;

  get hideSideBar(): boolean {
    return !this.isAdminOrPartner;
  }

  toggleAccountMenu() {
    this.isAccountMenuOpen = !this.isAccountMenuOpen;
  }

  get isHomePage(): boolean {
    return this.router.url === '/' || this.router.url === '';
  }

  goToRoute(route: string) {
    this.toggleAccountMenu();
    this.router.navigate(['/' + route]);
  }

  @HostListener('window:resize')
  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize() {
    if (window.innerWidth < 768) {
      this.sidebarOpen = false;
    } else {
      this.sidebarOpen = true;
    }
  }
  /**
   * Version simplifiée d'isActiveRoute
   */
  /**
   * Version simplifiée de handleMenuClick
   */
  handleMenuClick(item: any): void {
    // Si désactivé, rediriger vers upgrade
    if (item.disabled) {
      this.navigateToUpgradeWithContext(item.feature, item.tooltip);
      return;
    }

    // Si c'est un menu avec sous-éléments
    if (item.items && item.items.length > 0) {
      // Si menu fermé, l'ouvrir
      if (!this.sidebarOpen) {
        this.sidebarOpen = true;
      }

      // Toggle le submenu
      const index = this.items().findIndex(menuItem => menuItem === item);
      if (index !== -1) {
        this.toggleSubmenu(index);
      }
      return;
    }

    // Navigation normale
    if (item.routerLink) {
      const route = Array.isArray(item.routerLink) ? item.routerLink : [item.routerLink];
      this.router.navigate(route);

      // Fermer menu sur mobile
      if (window.innerWidth < 768) {
        this.sidebarOpen = false;
      }
    }
  }
  isActiveRoute(routerLink: string | string[] | null | undefined): boolean {
    if (!routerLink) return false;

    const currentUrl = this.router.url;
    const targetUrl = Array.isArray(routerLink) ? routerLink.join('/') : routerLink;

    // Cas spécial pour l'accueil
    if (targetUrl === '/dashbord' || targetUrl === '/dashboard') {
      return currentUrl.startsWith('/dashbord') || currentUrl.startsWith('/dashboard') || currentUrl === '/';
    }

    // Vérification standard
    return currentUrl === targetUrl || currentUrl.startsWith(targetUrl + '/');
  }
  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    this.submenuOpenIndex = null;
  }

  toggleSubmenu(index: number) {
    if (this.submenuOpenIndex === index) {
      this.submenuOpenIndex = null;
    } else {
      this.submenuOpenIndex = index;
    }
  }

  setHoverItem(index: number) {
    if (!this.sidebarOpen) {
      this.hoverItemIndex = index;
    }
  }

  clearHoverItem() {
    this.hoverItemIndex = null;
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  get isAdmin(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account ? account.authorities.includes('ROLE_ADMIN') : false;
  }

  get isPartner(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account ? account.authorities.includes('ROLE_PARTNER') : false;
  }

  get isAdminOrPartner(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    return account
      ? account.authorities.includes('ROLE_ADMIN') ||
          account.authorities.includes('ROLE_PARTNER') ||
          account.authorities.includes('ROLE_USER')
      : false;
  }

  logout(): void {
    this.loginService.logout();
    this.isAccountMenuOpen = false;
    this.router.navigate(['']);
  }

  // ===== NOUVELLES MÉTHODES POUR LE SYSTÈME D'ABONNEMENT =====

  /**
   * 🔄 Force le rechargement du menu après un changement d'abonnement
   */
  refreshSubscriptionData(): void {
    console.log("🔄 Rechargement des données d'abonnement...");
    this.subscriptionService.loadUserSubscriptions().subscribe(() => {
      this.dynamicMenuService.refreshMenu();
      console.log('✅ Menu mis à jour');
    });
  }

  /**
   * 📊 Récupère les informations d'accès pour les templates
   */
  get subscriptionAccess() {
    return this.subscriptionService.subscriptionAccess();
  }

  /**
   * 🎯 Navigue vers la page d'upgrade avec contexte
   */
  navigateToUpgradeWithContext(feature?: string, reason?: string): void {
    this.router.navigate(['/upgrade'], {
      queryParams: {
        feature: feature,
        reason: reason,
        returnUrl: this.router.url,
      },
    });
  }

  /**
   * Adapte les classes de badges pour le sidebar sombre
   * @param originalBadgeClass - La classe de badge originale
   * @returns string - Classes CSS adaptées pour le fond sombre
   */
  getBadgeClassForDarkSidebar(originalBadgeClass: string | undefined): string {
    // Classes de base pour tous les badges dans le sidebar sombre
    const baseClasses = 'backdrop-blur-sm border shadow-sm';

    // Mapping des couleurs pour le fond sombre
    if (originalBadgeClass?.includes('bg-green') || originalBadgeClass?.includes('badge-success')) {
      return `${baseClasses} bg-emerald-500/80 text-white border-emerald-400/50`;
    }

    if (originalBadgeClass?.includes('bg-red') || originalBadgeClass?.includes('badge-danger')) {
      return `${baseClasses} bg-red-500/80 text-white border-red-400/50`;
    }

    if (originalBadgeClass?.includes('bg-orange') || originalBadgeClass?.includes('badge-warning')) {
      return `${baseClasses} bg-amber-500/80 text-white border-amber-400/50`;
    }

    if (originalBadgeClass?.includes('bg-blue') || originalBadgeClass?.includes('badge-info')) {
      return `${baseClasses} bg-blue-500/80 text-white border-blue-400/50`;
    }

    if (originalBadgeClass?.includes('bg-purple')) {
      return `${baseClasses} bg-purple-500/80 text-white border-purple-400/50`;
    }

    if (originalBadgeClass?.includes('bg-gray') || originalBadgeClass?.includes('badge-disabled')) {
      return `${baseClasses} bg-slate-600/80 text-slate-200 border-slate-500/50`;
    }

    // Badge par défaut pour les cas non spécifiés
    return `${baseClasses} bg-indigo-500/80 text-white border-indigo-400/50`;
  }

  /**
   * Version alternative avec plus de personnalisation
   */
  getBadgeClassForDarkSidebarAdvanced(originalBadgeClass: string, isActive: boolean = false): string {
    const baseClasses = 'backdrop-blur-sm border shadow-lg font-bold';
    const activeClasses = isActive ? 'ring-2 ring-white/30' : '';

    // Badges avec effet glassmorphism pour sidebar sombre
    const badgeMap: Record<string, string> = {
      success: `${baseClasses} ${activeClasses} bg-gradient-to-r from-emerald-500/90 to-green-500/90 text-white border-emerald-400/60`,
      danger: `${baseClasses} ${activeClasses} bg-gradient-to-r from-red-500/90 to-rose-500/90 text-white border-red-400/60`,
      warning: `${baseClasses} ${activeClasses} bg-gradient-to-r from-amber-500/90 to-orange-500/90 text-white border-amber-400/60`,
      info: `${baseClasses} ${activeClasses} bg-gradient-to-r from-blue-500/90 to-cyan-500/90 text-white border-blue-400/60`,
      primary: `${baseClasses} ${activeClasses} bg-gradient-to-r from-indigo-500/90 to-purple-500/90 text-white border-indigo-400/60`,
      disabled: `${baseClasses} ${activeClasses} bg-slate-600/80 text-slate-300 border-slate-500/50`,
    };

    // Détecter le type de badge depuis la classe originale
    for (const [type, classes] of Object.entries(badgeMap)) {
      if (originalBadgeClass?.includes(type) || originalBadgeClass?.includes(`badge-${type}`)) {
        return classes;
      }
    }

    // Badge par défaut avec gradient
    return `${baseClasses} ${activeClasses} bg-gradient-to-r from-slate-500/90 to-slate-600/90 text-slate-200 border-slate-400/50`;
  }
}
