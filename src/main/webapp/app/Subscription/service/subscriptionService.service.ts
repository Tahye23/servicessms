import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { AccessRestriction, SubscriptionAccess, SubscriptionType, UserSubscription } from '../subscrptionAcces.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private http = inject(HttpClient);
  private userSubscriptions = signal<UserSubscription[]>([]);
  public currentAccess = signal<SubscriptionAccess>({
    canSendSMS: false,
    canSendWhatsApp: false,
    canManageUsers: false,
    canManageTemplates: false,
    canViewConversations: false,
    canViewDashboard: false,
    canManageAPI: false,
    smsRemaining: 0,
    sidebarVisible: true,
    whatsappRemaining: 0,
    isSubscriptionExpiring: false, // ✅ Toujours false
    daysUntilExpiration: 0, // ✅ Toujours 0
    subscriptionType: 'FREE',
    needsUpgrade: false,
  });

  subscriptionAccess = computed(() => this.currentAccess());
  hasActiveSubscription = computed(() => this.userSubscriptions().length > 0);
  canSendSMS = computed(() => this.currentAccess().canSendSMS);
  canSendWhatsApp = computed(() => this.currentAccess().canSendWhatsApp);
  needsUpgrade = computed(() => this.currentAccess().needsUpgrade);

  loadUserSubscriptions(): Observable<UserSubscription[]> {
    return this.http.get<UserSubscription[]>('/api/subscription/user/partner-subscription').pipe(
      tap(subscriptions => {
        this.userSubscriptions.set(subscriptions);
        this.calculateAccess(subscriptions);
        console.log('subscriptions', subscriptions);
      }),
    );
  }

  private calculateAccess(subscriptions: UserSubscription[]): void {
    if (subscriptions.length === 0) {
      this.setFreeAccess();
      return;
    }

    const access: SubscriptionAccess = {
      canSendSMS: false,
      canSendWhatsApp: false,
      canManageUsers: false,
      canManageTemplates: false,
      canViewConversations: false,
      canViewDashboard: false,
      canManageAPI: false,
      smsRemaining: 0,
      whatsappRemaining: 0,
      isSubscriptionExpiring: false, // ✅ Toujours false (pas d'expiration)
      daysUntilExpiration: 0, // ✅ Toujours 0 (pas d'expiration)
      subscriptionType: 'FREE',
      needsUpgrade: false,
      sidebarVisible: true,
    };

    subscriptions.forEach(sub => {
      // ✅ SUPPRIMÉ : Calcul des jours jusqu'à expiration
      // const daysUntilExp = sub.endDate ? this.calculateDaysUntilExpiration(new Date(sub.endDate)) : null;

      const smsBonus = sub.bonusSmsEnabled && sub.bonusSmsAmount ? sub.bonusSmsAmount : 0;
      const whatsappBonus = sub.bonusWhatsappEnabled && sub.bonusWhatsappAmount ? sub.bonusWhatsappAmount : 0;

      // Calcule sms restants = limite - utilisés + bonus
      const smsUsed = sub.smsUsed ?? 0;
      const smsLimit = sub.smsLimit ?? 0;
      const smsRemaining = smsLimit - smsUsed;

      const whatsappUsed = sub.whatsappUsed ?? 0;
      const whatsappLimit = sub.whatsappLimit ?? 0;
      const whatsappRemaining = whatsappLimit - whatsappUsed;

      access.canSendSMS = access.canSendSMS || smsRemaining > 0;
      access.canSendWhatsApp = access.canSendWhatsApp || whatsappRemaining > 0;
      access.sidebarVisible = sub.sidebarVisible;
      access.canManageUsers = access.canManageUsers || !!sub.customCanManageUsers;
      access.canManageTemplates = access.canManageTemplates || !!sub.customCanManageTemplates;
      access.canViewConversations = access.canViewConversations || !!sub.customCanViewConversations;
      access.canViewDashboard = access.canViewDashboard || !!sub.canViewDashboard;
      access.canManageAPI = access.canManageAPI || !!sub.canManageAPI;

      access.smsRemaining += smsRemaining;
      access.whatsappRemaining += whatsappRemaining;

      if (sub.planType && sub.planType !== 'FREE') {
        if (this.isValidSubscriptionType(sub.planType)) {
          access.subscriptionType = sub.planType;
        }
      }

      // ✅ SUPPRIMÉ : Vérification de l'expiration
      /*
      if (daysUntilExp !== null && daysUntilExp <= 7) {
        access.isSubscriptionExpiring = true;
        access.daysUntilExpiration = daysUntilExp;
      }
      */

      // Vérifier si upgrade nécessaire (basé uniquement sur les quotas restants)
      if (smsRemaining <= 0 && whatsappRemaining <= 0) {
        access.needsUpgrade = true;
      }
    });

    // ✅ Toujours false - les abonnements n'expirent jamais
    access.isSubscriptionExpiring = false;
    access.daysUntilExpiration = 0;

    this.currentAccess.set(access);
  }

  private setFreeAccess(): void {
    this.currentAccess.set({
      canSendSMS: true,
      canSendWhatsApp: true,
      canManageUsers: false,
      canManageTemplates: false,
      canViewConversations: false,
      canViewDashboard: false,
      canManageAPI: false,
      smsRemaining: 10,
      whatsappRemaining: 10,
      isSubscriptionExpiring: false, // ✅ Toujours false
      daysUntilExpiration: 0, // ✅ Toujours 0
      subscriptionType: 'FREE',
      needsUpgrade: true,
      sidebarVisible: true,
    });
  }

  checkFeatureAccess(feature: string): AccessRestriction {
    const access = this.currentAccess();

    const restrictions: Record<string, AccessRestriction> = {
      'send-sms': {
        feature: 'send-sms',
        allowed: access.canSendSMS && access.smsRemaining > 0,
        reason: !access.canSendSMS ? 'Abonnement SMS requis' : 'Plus de SMS disponibles',
        upgradeRequired: !access.canSendSMS || access.smsRemaining <= 5,
      },
      'send-whatsapp': {
        feature: 'send-whatsapp',
        allowed: access.canSendWhatsApp && access.whatsappRemaining > 0,
        reason: !access.canSendWhatsApp ? 'Abonnement WhatsApp requis' : 'Plus de messages WhatsApp disponibles',
        upgradeRequired: !access.canSendWhatsApp || access.whatsappRemaining <= 5,
      },
      'manage-users': {
        feature: 'manage-users',
        allowed: access.canManageUsers,
        reason: 'Fonctionnalité disponible uniquement pour les partenaires',
        upgradeRequired: true,
      },
      templates: {
        feature: 'templates',
        allowed: access.canManageTemplates || access.subscriptionType !== 'FREE',
        reason: 'Mise à niveau requise pour gérer les modèles',
        upgradeRequired: access.subscriptionType === 'FREE',
      },
      'view-dashboard': {
        feature: 'view-dashboard',
        allowed: access.canViewDashboard,
        reason: 'Accès refusé au tableau de bord',
        upgradeRequired: true,
      },
      'manage-api': {
        feature: 'manage-api',
        allowed: access.canManageAPI,
        reason: 'Accès API non autorisé',
        upgradeRequired: true,
      },
      conversations: {
        feature: 'conversations',
        allowed: access.canViewConversations,
        reason: 'Accès aux conversations refusé',
        upgradeRequired: true,
      },
    };

    return restrictions[feature] || { feature, allowed: true };
  }

  updateCounter(type: 'SMS' | 'WHATSAPP', count: number): Observable<any> {
    return this.http.post('/api/user/update-counter', { type, count }).pipe(tap(() => this.loadUserSubscriptions().subscribe()));
  }

  private isValidSubscriptionType(value: string): value is SubscriptionType {
    return ['FREE', 'SMS', 'WHATSAPP', 'PREMIUM', 'NONE'].includes(value);
  }

  /**
   * ✅ MÉTHODE SUPPRIMÉE : Plus besoin de calculer l'expiration
   */
  /*
  private calculateDaysUntilExpiration(expirationDate: Date): number | null {
    if (!expirationDate) return null;
    const now = new Date();
    const diff = expirationDate.getTime() - now.getTime();
    return diff > 0 ? Math.ceil(diff / (1000 * 60 * 60 * 24)) : 0;
  }
  */

  getUpgradeSuggestions(): string[] {
    const access = this.currentAccess();
    const suggestions: string[] = [];

    if (access.subscriptionType === 'FREE') {
      suggestions.push('Passez à un plan payant pour plus de fonctionnalités');
    }
    if (access.smsRemaining <= 10 && access.canSendSMS) {
      suggestions.push('Rechargez votre forfait SMS');
    }
    if (access.whatsappRemaining <= 10 && access.canSendWhatsApp) {
      suggestions.push('Rechargez votre forfait WhatsApp');
    }

    // ✅ SUPPRIMÉ : Message d'expiration
    /*
    if (access.isSubscriptionExpiring) {
      suggestions.push(`Votre abonnement expire dans ${access.daysUntilExpiration} jours`);
    }
    */

    return suggestions;
  }

  updateSidebarStatus(isVisible: boolean): Observable<any> {
    const params = new HttpParams().set('visible', isVisible.toString());
    return this.http.put('/api/subscription/sidebar-visibility', null, { params });
  }
}
