import { inject, Injectable } from '@angular/core';
import { SubscriptionService } from './subscriptionService.service';

@Injectable({ providedIn: 'root' })
export class SubscriptionNotificationService {
  private subscriptionService = inject(SubscriptionService);

  /**
   * Vérifie et affiche les notifications d'abonnement
   */
  checkAndShowNotifications(): void {
    const access = this.subscriptionService.subscriptionAccess();

    if (access.isSubscriptionExpiring) {
      this.showExpirationWarning(access.daysUntilExpiration);
    }

    if (access.needsUpgrade) {
      this.showUpgradeSuggestion();
    }

    if (access.smsRemaining <= 5 && access.canSendSMS) {
      this.showLowCreditWarning('SMS', access.smsRemaining);
    }

    if (access.whatsappRemaining <= 5 && access.canSendWhatsApp) {
      this.showLowCreditWarning('WhatsApp', access.whatsappRemaining);
    }
  }

  private showExpirationWarning(days: number): void {
    // Intégration avec votre système de notifications
    console.warn(`Votre abonnement expire dans ${days} jours`);
  }

  private showUpgradeSuggestion(): void {
    console.info('Améliorez votre plan pour plus de fonctionnalités');
  }

  private showLowCreditWarning(type: string, remaining: number): void {
    console.warn(`Il vous reste ${remaining} crédits ${type}`);
  }
}
