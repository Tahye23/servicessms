import { Injectable, computed, inject, signal } from '@angular/core';
import { AccountService } from 'app/core/auth/account.service';
import { DynamicMegaMenuItem } from '../subscrptionAcces.model';
import { SubscriptionService } from './subscriptionService.service';

@Injectable({ providedIn: 'root' })
export class DynamicMenuService {
  private accountService = inject(AccountService);
  private subscriptionService = inject(SubscriptionService);

  private currentAccount = this.accountService.trackCurrentAccount();

  private isAdmin = computed(() => {
    const account = this.currentAccount();
    return account?.authorities?.includes('ROLE_ADMIN') ?? false;
  });

  private isPartner = computed(() => {
    const account = this.currentAccount();
    return account?.authorities?.includes('ROLE_PARTNER') ?? false;
  });

  private isUser = computed(() => {
    const account = this.currentAccount();
    return account?.authorities?.includes('ROLE_USER') ?? false;
  });

  get adminRole() {
    return this.isAdmin();
  }
  get partnerRole() {
    return this.isPartner();
  }
  get userRole() {
    return this.isUser();
  }

  public userPermissions = computed(() => {
    const account = this.currentAccount();
    if (!account?.permissions) return [];

    try {
      return JSON.parse(account.permissions);
    } catch {
      return [];
    }
  });

  public hasPermission(permission: string): boolean {
    if (this.adminRole) return true; // Admin a tout
    return this.userPermissions().includes(permission);
  }

  private adminAccess = computed(() => ({
    canSendSMS: false,
    canSendWhatsApp: false,
    canManageUsers: false,
    canManageTemplates: false,
    canViewConversations: false,
    canViewDashboard: false,
    canManageAPI: false,
    smsRemaining: 0,
    whatsappRemaining: 0,
    isSubscriptionExpiring: false,
    daysUntilExpiration: 0,
    subscriptionType: 'FREE',
    needsUpgrade: false,
    sidebarVisible: true,
  }));

  private readonly icons = {
    home: 'pi pi-home',
    sms: 'pi pi-comments',
    whatsapp: 'pi pi-whatsapp',
    conversations: 'pi pi-comments',
    templates: 'pi pi-file',
    contacts: 'pi pi-users',
    users: 'pi pi-users',
    apps: 'pi pi-key',
    subscription: 'pi pi-credit-card',
    config: 'pi pi-cog',
    admin: 'pi pi-crown',
    shield: 'pi pi-shield',
    database: 'pi pi-database',
    exclamation: 'pi pi-exclamation-triangle',
  };

  items = computed((): DynamicMegaMenuItem[] => {
    const admin = this.adminRole;
    const partner = this.partnerRole;
    const user = this.userRole;

    const access = this.subscriptionService.subscriptionAccess();

    const menuItems: DynamicMegaMenuItem[] = [
      {
        label: 'Accueil',
        icon: 'fas fa-home',
        routerLink: admin || (this.hasPermission('dashboard') && access.canViewDashboard) ? ['/dashbord'] : ['/upgrade'],
        styleClass: 'menuContclass',
        tooltip: !this.hasPermission('dashboard') || !access.canViewDashboard ? 'Permission ou abonnement requis' : '',
        disabled: !this.hasPermission('dashboard') || (!access.canViewDashboard && !admin),
        visible: admin || this.hasPermission('dashboard'),
      },
      {
        label: 'SMS',
        icon: 'fas fa-sms',
        routerLink: this.hasPermission('sms') && access.canSendSMS ? ['/send-sms'] : ['/upgrade'],
        styleClass: this.getMenuItemClass(this.hasPermission('sms') && access.canSendSMS, access.smsRemaining <= 5 && !admin),
        visible: admin || this.hasPermission('sms'),
        badge: admin ? '∞' : this.hasPermission('sms') && access.canSendSMS ? access.smsRemaining.toString() : '0',
        badgeStyleClass: this.getBadgeClass(access.smsRemaining, this.hasPermission('sms') && access.canSendSMS, admin),
        tooltip: this.getSmsTooltip(access, admin, this.hasPermission('sms')),
        upgradeRequired: !admin && (!this.hasPermission('sms') || !access.canSendSMS || access.smsRemaining <= 5),
        disabled: !this.hasPermission('sms') || (!access.canSendSMS && !admin) || (access.smsRemaining <= 0 && !admin),
        feature: 'send-sms',
      },
      {
        label: 'WhatsApp',
        icon: 'fab fa-whatsapp',
        routerLink: this.hasPermission('whatsapp') && access.canSendWhatsApp ? ['/send-whatsapp'] : ['/upgrade'],
        styleClass: this.getMenuItemClass(
          this.hasPermission('whatsapp') && access.canSendWhatsApp,
          access.whatsappRemaining <= 5 && !admin,
        ),
        visible: admin || this.hasPermission('whatsapp'),
        badge: admin ? '∞' : this.hasPermission('whatsapp') && access.canSendWhatsApp ? access.whatsappRemaining.toString() : '0',
        badgeStyleClass: this.getBadgeClass(access.whatsappRemaining, this.hasPermission('whatsapp') && access.canSendWhatsApp, admin),
        tooltip: this.getWhatsAppTooltip(access, admin, this.hasPermission('whatsapp')),
        upgradeRequired: !admin && (!this.hasPermission('whatsapp') || !access.canSendWhatsApp || access.whatsappRemaining <= 5),
        disabled: !this.hasPermission('whatsapp') || (!access.canSendWhatsApp && !admin) || (access.whatsappRemaining <= 0 && !admin),
        feature: 'send-whatsapp',
      },
      {
        label: 'Conversations',
        visible: admin || this.hasPermission('conversations'),
        styleClass:
          admin || (this.hasPermission('conversations') && access.canViewConversations) ? 'menuitemclass' : 'menuitemclass disabled',
        icon: 'fas fa-comments',
        routerLink: admin || (this.hasPermission('conversations') && access.canViewConversations) ? ['/conversations'] : ['/upgrade'],
        disabled: !admin && (!this.hasPermission('conversations') || !access.canViewConversations),
        tooltip: !admin && (!this.hasPermission('conversations') || !access.canViewConversations) ? 'Permission ou abonnement requis' : '',
      },
      {
        label: 'Modèles',
        visible: admin || this.hasPermission('templates'),
        styleClass: admin || (this.hasPermission('templates') && access.canManageTemplates) ? 'menuitemclass' : 'menuitemclass disabled',
        icon: 'fas fa-file-alt',
        routerLink: admin || (this.hasPermission('templates') && access.canManageTemplates) ? ['/template'] : ['/upgrade'],
        tooltip:
          !admin && (!this.hasPermission('templates') || !access.canManageTemplates)
            ? 'Permission ou abonnement requis pour gérer les modèles'
            : '',
        upgradeRequired: !admin && (!this.hasPermission('templates') || !access.canManageTemplates),
        disabled: !admin && (!this.hasPermission('templates') || !access.canManageTemplates),
        feature: 'templates',
      },
      this.getContactsMenuItem(admin, partner, user, access),
      {
        label: 'Gestion des utilisateurs',
        icon: 'fas fa-address-book',
        visible: admin || this.hasPermission('users'),
        routerLink: admin || (this.hasPermission('users') && access.canManageUsers) ? ['/admin/user-management'] : ['/upgrade'],
        styleClass: admin || (this.hasPermission('users') && access.canManageUsers) ? 'menuitemclass' : 'menuitemclass disabled',
        tooltip: !admin && (!this.hasPermission('users') || !access.canManageUsers) ? 'Permission ou abonnement requis' : '',
        upgradeRequired: !admin && (!this.hasPermission('users') || !access.canManageUsers),
        disabled: !admin && (!this.hasPermission('users') || !access.canManageUsers),
        feature: 'manage-users',
      },
      {
        label: 'Tokens ',
        visible: admin || this.hasPermission('applications'),
        styleClass: admin || (this.hasPermission('applications') && access.canManageAPI) ? 'menuitemclass' : 'menuitemclass disabled',
        disabled: !admin && (!this.hasPermission('applications') || !access.canManageAPI),
        tooltip: !admin && (!this.hasPermission('applications') || !access.canManageAPI) ? 'Permission ou abonnement requis' : '',
        icon: 'fas fa-key',
        routerLink: admin || (this.hasPermission('applications') && access.canManageAPI) ? ['/tokens-app'] : ['/upgrade'],
      },
      {
        label: 'Messages API',
        visible: admin || this.hasPermission('applications'),
        styleClass: admin || (this.hasPermission('applications') && access.canManageAPI) ? 'menuitemclass' : 'menuitemclass disabled',
        disabled: !admin && (!this.hasPermission('applications') || !access.canManageAPI),
        tooltip: !admin && (!this.hasPermission('applications') || !access.canManageAPI) ? 'Permission ou abonnement requis' : '',
        icon: 'fas fa-paper-plane',
        routerLink: admin || (this.hasPermission('applications') && access.canManageAPI) ? ['/external-api-messages'] : ['/upgrade'],
      },
      {
        label: 'Applications',
        visible: admin || this.hasPermission('applications'),
        styleClass: admin || (this.hasPermission('applications') && access.canManageAPI) ? 'menuitemclass' : 'menuitemclass disabled',
        disabled: !admin && (!this.hasPermission('applications') || !access.canManageAPI),
        tooltip: !admin && (!this.hasPermission('applications') || !access.canManageAPI) ? 'Permission ou abonnement requis' : '',
        icon: 'fas fa-mobile-alt',
        routerLink: admin || (this.hasPermission('applications') && access.canManageAPI) ? ['/application'] : ['/upgrade'],
      },
      {
        label: admin ? 'Gestion abonnements' : 'Mon abonnement',
        visible: admin || partner || this.hasPermission('subscriptions'),
        styleClass: admin || !access.needsUpgrade ? 'menuitemclass' : 'menuitemclass upgrade-needed',
        icon: 'fas fa-crown',
        routerLink: admin ? ['/admin/abonnements'] : ['/billing'],
        badge: admin ? 'ADMIN' : access.needsUpgrade ? '!' : '',
        badgeStyleClass: admin ? 'badge-admin' : 'badge-warning',
        tooltip: admin
          ? 'Gérer tous les abonnements'
          : access.needsUpgrade
            ? 'Action requise sur votre abonnement'
            : 'Gérer mon abonnement',
      },
      {
        label: 'Configuration',
        visible: admin || partner || this.hasPermission('config'),
        styleClass: 'menuitemclass',
        icon: 'fas fa-cog',
        routerLink: ['/config'],
      },
    ];

    if (admin) {
      menuItems.push(
        {
          label: 'Services',
          visible: true,
          icon: 'fas fa-tools',
          styleClass: 'menuitemclass',
          items: [
            [
              {
                items: [
                  { label: 'Envoi SMS', styleClass: 'menuitemclass', routerLink: ['/services/sms'] },
                  { label: 'Nettoyage', styleClass: 'menuitemclass', routerLink: ['/services/cleanup'] },
                  { label: 'OTP', styleClass: 'menuitemclass', routerLink: ['/services/otp'] },
                ],
              },
            ],
          ],
        },
        {
          label: 'Administration',
          visible: true,
          icon: 'fas fa-shield-alt',
          styleClass: 'menuitemclass',
          items: [
            [
              {
                items: [
                  { label: 'Autorités', routerLink: ['/admin/authority'], icon: 'pi pi-key', styleClass: 'menuitemclass' },
                  {
                    label: 'Gestion utilisateurs',
                    icon: 'pi pi-users',
                    routerLink: ['/admin/user-management'],
                    styleClass: 'menuitemclass',
                  },
                  { label: 'Métriques', routerLink: ['/admin/metrics'], styleClass: 'menuitemclass' },
                  { label: 'Diagnostics', routerLink: ['/admin/health'], styleClass: 'menuitemclass' },
                  { label: 'Configuration', routerLink: ['/admin/configuration'], styleClass: 'menuitemclass' },
                  { label: 'Logs', routerLink: ['/admin/logs'], styleClass: 'menuitemclass' },
                  { label: 'API', routerLink: ['/admin/docs'], styleClass: 'menuitemclass' },
                ],
              },
            ],
          ],
        },
        {
          label: 'Gestion',
          visible: true,
          icon: 'fas fa-ellipsis-h',
          styleClass: 'menuitemclass',
          items: [
            [
              {
                items: [
                  { label: 'Demandes partenariat', routerLink: ['/requests'], styleClass: 'menuitemclass' },
                  { label: 'Utilisateurs', routerLink: ['/extended-user'], styleClass: 'menuitemclass' },
                  { label: 'Entreprises', routerLink: ['/company'], styleClass: 'menuitemclass' },
                  { label: 'Abonnements', routerLink: ['/abonnement'], styleClass: 'menuitemclass' },
                  { label: 'Plans abonnement', routerLink: ['/planabonnement'], styleClass: 'menuitemclass' },
                  { label: 'Applications', routerLink: ['/application'], styleClass: 'menuitemclass' },
                  { label: 'Services', routerLink: ['/service'], styleClass: 'menuitemclass' },
                  { label: 'APIs', routerLink: ['/sapi'], styleClass: 'menuitemclass' },
                  { label: 'Tokens', routerLink: ['/tokens-app'], styleClass: 'menuitemclass' },
                ],
              },
            ],
          ],
        },
      );
    }

    return menuItems.filter(item => item.visible !== false);
  });

  private getContactsMenuItem(admin: boolean, partner: boolean, user: boolean, access: any): DynamicMegaMenuItem {
    if (admin || partner || this.hasPermission('canManageContacts')) {
      return {
        label: 'Destinataires',
        visible: admin || this.hasPermission('contacts'),
        icon: 'fas fa-users',
        items: [
          [
            {
              items: [
                {
                  label: 'Contacts',
                  icon: 'fas fa-user',
                  routerLink: ['/contact'],
                  visible: admin || this.hasPermission('contacts'),
                  styleClass: admin || this.hasPermission('contacts') ? 'menuitemclass' : 'menuitemclass disabled',
                  disabled: !admin && !this.hasPermission('contacts'),
                },
                {
                  label: 'Groupes de contacts',
                  icon: 'fas fa-layer-group',
                  visible: admin || this.hasPermission('groups'),
                  routerLink: admin || (this.hasPermission('groups') && access.canManageTemplates) ? ['/groupe'] : ['/upgrade'],
                  styleClass:
                    admin || (this.hasPermission('groups') && access.canManageTemplates) ? 'menuitemclass' : 'menuitemclass disabled',
                  tooltip: !admin && !access.canManageTemplates ? 'Permission ou abonnement requis' : '',
                  disabled: !admin && !access.canManageTemplates,

                  upgradeRequired: !admin && (!this.hasPermission('groups') || !access.canManageTemplates),
                },
              ],
            },
          ],
        ],
      };
    }
    return {
      label: 'Mes contacts',
      visible: admin || this.hasPermission('contacts'),
      styleClass: admin || this.hasPermission('contacts') ? 'menuitemclass' : 'menuitemclass disabled',
      icon: 'pi pi-user',
      routerLink: admin || this.hasPermission('contacts') ? ['/contact'] : ['/upgrade'],
      disabled: !admin && !this.hasPermission('contacts'),
      tooltip: !admin && !this.hasPermission('contacts') ? 'Permission requise pour gérer les contacts' : '',
    };
  }

  private getMenuItemClass(hasAccess: boolean, isLowCredit: boolean): string {
    const classes = ['menuitemclass'];

    if (!hasAccess) {
      classes.push('opacity-60', 'cursor-not-allowed');
    } else if (isLowCredit) {
      classes.push('border-l-4', 'border-orange-400', 'bg-orange-50');
    }

    return classes.join(' ');
  }

  private getBadgeClass(remaining: number, hasAccess: boolean, isAdmin = false): string {
    if (isAdmin) return 'bg-purple-500 text-white px-2 py-1 rounded-full text-xs font-bold';
    if (!hasAccess) return 'bg-gray-400 text-white px-2 py-1 rounded-full text-xs';
    if (remaining <= 5) return 'bg-red-500 text-white px-2 py-1 rounded-full text-xs animate-pulse';
    if (remaining <= 20) return 'bg-orange-500 text-white px-2 py-1 rounded-full text-xs';
    return 'bg-green-500 text-white px-2 py-1 rounded-full text-xs';
  }

  private getSmsTooltip(access: any, isAdmin = false, hasPermission = true): string {
    if (isAdmin) return 'Accès administrateur - SMS illimités';
    if (!hasPermission) return 'Permission requise pour envoyer des SMS';
    if (!access.canSendSMS) return 'Abonnement SMS requis - Cliquez pour améliorer';
    if (access.smsRemaining <= 0) return 'Plus de SMS disponibles - Rechargez maintenant';
    if (access.smsRemaining <= 5) return `Attention : Plus que ${access.smsRemaining} SMS disponibles`;
    return `${access.smsRemaining} SMS disponibles`;
  }

  private getWhatsAppTooltip(access: any, isAdmin = false, hasPermission = true): string {
    if (isAdmin) return 'Accès administrateur - Messages WhatsApp illimités';
    if (!hasPermission) return 'Permission requise pour envoyer des messages WhatsApp';
    if (!access.canSendWhatsApp) return 'Abonnement WhatsApp requis - Cliquez pour améliorer';
    if (access.whatsappRemaining <= 0) return 'Plus de messages WhatsApp disponibles - Rechargez maintenant';
    if (access.whatsappRemaining <= 5) return `Attention : Plus que ${access.whatsappRemaining} messages WhatsApp disponibles`;
    return `${access.whatsappRemaining} messages WhatsApp disponibles`;
  }

  initializeMenu(): void {
    if (this.isAdmin()) {
      return;
    }

    this.subscriptionService.loadUserSubscriptions().subscribe({
      next: () => {},
      error: error => console.error("Erreur lors de l'initialisation du menu:", error),
    });
  }

  refreshMenu(): void {
    if (this.isAdmin()) {
      return;
    }

    this.subscriptionService.loadUserSubscriptions().subscribe({
      next: () => {},
      error: error => console.error('Erreur lors du rafraîchissement du menu:', error),
    });
  }

  hasFeatureAccess(feature: string): boolean {
    if (this.adminRole) return true;

    const access = this.subscriptionService.subscriptionAccess();

    switch (feature) {
      case 'send-sms':
        return this.hasPermission('sms') && access.canSendSMS && access.smsRemaining > 0;
      case 'send-whatsapp':
        return this.hasPermission('whatsapp') && access.canSendWhatsApp && access.whatsappRemaining > 0;
      case 'templates':
        return this.hasPermission('templates') && access.canManageTemplates;
      case 'manage-users':
        return this.hasPermission('users') && access.canManageUsers;
      case 'conversations':
        return this.hasPermission('conversations') && access.canViewConversations;
      case 'contacts':
        return this.hasPermission('contacts');
      case 'groups':
        return this.hasPermission('groups');
      case 'api':
        return this.hasPermission('applications') && access.canManageAPI;
      default:
        return false;
    }
  }
}
