import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SubscriptionService } from './service/subscriptionService.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-subscription-status',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="bg-gradient-to-br from-white to-gray-50 rounded-xl p-5 max-w-sm mx-auto border border-gray-100 hover:shadow-xl transition-all duration-300"
      [ngClass]="{
        'ring-2 ring-amber-300/50 shadow-amber-100': access().needsUpgrade || access().isSubscriptionExpiring,
        'ring-2 ring-emerald-300/50 shadow-emerald-100': !access().needsUpgrade && !access().isSubscriptionExpiring
      }"
    >
      <!-- Header avec toggle -->
      <div class="flex justify-between items-center cursor-pointer group" (click)="toggleDetails()">
        <div class="flex items-center gap-3">
          <!-- Badge avec icône -->
          <div class="flex items-center gap-2">
            <div class="w-6 h-6 rounded-full flex items-center justify-center" [ngClass]="badgeIconClass()">
              <i [class]="badgeIcon()" class="text-sm"></i>
            </div>
            <span
              class="px-2 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider select-none shadow-sm"
              [ngClass]="badgeClass()"
            >
              {{ subscriptionTypeLabel() }}
            </span>
          </div>
        </div>

        <button
          type="button"
          aria-label="Afficher les détails de l'abonnement"
          class="w-6 h-6 rounded-full bg-gray-100 flex items-center justify-center transform transition-all duration-300 hover:bg-gray-200 group-hover:scale-110"
          [class.rotate-180]="detailsVisible()"
          (click)="$event.stopPropagation(); toggleDetails()"
        >
          <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      <!-- Status rapide (toujours visible) -->
      <div class="mt-4">
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-600 font-medium">État</span>
          <div class="flex items-center gap-2">
            <div
              class="w-2 h-2 rounded-full animate-pulse"
              [ngClass]="{
                'bg-emerald-500': !access().needsUpgrade && !access().isSubscriptionExpiring,
                'bg-amber-500': access().smsRemaining <= 100 && access().whatsappRemaining <= 100,
                'bg-red-500': access().needsUpgrade || access().isSubscriptionExpiring
              }"
            ></div>
            <span
              class="font-semibold"
              [ngClass]="{
                'text-emerald-600': !access().needsUpgrade && !access().isSubscriptionExpiring,
                'text-amber-600': access().smsRemaining <= 100 && access().whatsappRemaining <= 100,
                'text-red-600': access().needsUpgrade || access().isSubscriptionExpiring
              }"
            >
              {{ getStatusText() }}
            </span>
          </div>
        </div>
      </div>

      <!-- Détails (déroulants) -->
      <div
        class="overflow-hidden transition-all duration-500 ease-out"
        [style.maxHeight]="detailsVisible() ? '800px' : '0px'"
        [style.opacity]="detailsVisible() ? '1' : '0'"
      >
        <div class="mt-6 space-y-5" [style.transform]="detailsVisible() ? 'translateY(0)' : 'translateY(-10px)'">
          <!-- Crédits pour plans payants -->
          <div *ngIf="access().subscriptionType !== 'FREE'" class="space-y-4">
            <!-- SMS -->
            <div
              *ngIf="access().canSendSMS"
              class="bg-gradient-to-r text-sm from-emerald-50 to-green-50 rounded-lg p-4 border border-emerald-200"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-emerald-500 rounded-lg flex items-center justify-center">
                    <i class="fas fa-sms text-white text-sm"></i>
                  </div>
                  <span class="font-semibold text-emerald-800">SMS</span>
                </div>
                <span class="text-lg font-bold text-emerald-700">{{ access().smsRemaining }}</span>
              </div>
            </div>

            <!-- WhatsApp -->
            <div
              *ngIf="access().canSendWhatsApp"
              class=" text-sm bg-gradient-to-r from-green-50 to-emerald-50 rounded-lg p-4 border border-green-200"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center gap-2">
                  <div class="w-5 h-5 bg-green-500 rounded-lg flex items-center justify-center">
                    <i class="fab fa-whatsapp text-white text-sm"></i>
                  </div>
                  <span class="font-semibold text-green-800 p-2">WTSP</span>
                </div>
                <span class="text-lg font-bold text-green-700">{{ access().whatsappRemaining }}</span>
              </div>
            </div>
          </div>

          <!-- Plan gratuit -->
          <div *ngIf="access().subscriptionType === 'FREE'" class="space-y-4">
            <div class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-4 border border-blue-200">
              <div class="text-center mb-4">
                <div
                  class="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center mx-auto mb-2"
                >
                  <i class="fas fa-gift text-white"></i>
                </div>
                <p class="font-semibold text-blue-800 text-sm">Plan Gratuit</p>
                <p class="text-xs text-blue-600">10 SMS et 10 WhatsApp par mois</p>
              </div>

              <!-- SMS Gratuit -->
              <div class="mb-4">
                <div class="flex justify-between items-center mb-2">
                  <span class="text-sm font-medium text-blue-700">SMS</span>
                  <span class="text-sm font-bold text-blue-800">{{ access().smsRemaining }}/10</span>
                </div>
                <div class="w-full h-2.5 bg-blue-200 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded-full transition-all duration-700"
                    [style.width.%]="(access().smsRemaining / 10) * 100"
                  ></div>
                </div>
              </div>

              <!-- WhatsApp Gratuit -->
              <div>
                <div class="flex justify-between items-center mb-2">
                  <span class="text-sm font-medium text-blue-700">WhatsApp</span>
                  <span class="text-sm font-bold text-blue-800">{{ access().whatsappRemaining }}/10</span>
                </div>
                <div class="w-full h-2.5 bg-blue-200 rounded-full overflow-hidden">
                  <div
                    class="h-full bg-gradient-to-r from-indigo-400 to-indigo-600 rounded-full transition-all duration-700"
                    [style.width.%]="(access().whatsappRemaining / 10) * 100"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Alertes et avertissements -->
          <div *ngIf="hasWarnings()" class="space-y-3">
            <div
              *ngIf="access().isSubscriptionExpiring"
              class="bg-gradient-to-r from-amber-50 to-orange-50 border-l-4 border-amber-400 rounded-r-lg p-4 animate-pulse"
            >
              <div class="flex items-center gap-3">
                <div class="w-6 h-6 bg-amber-500 rounded-full flex items-center justify-center">
                  <i class="fas fa-clock text-white text-sm"></i>
                </div>
                <div>
                  <p class="font-semibold text-amber-800 text-sm">Expiration proche</p>
                  <p class="text-xs text-amber-700">
                    Expire dans {{ access()!.daysUntilExpiration }} jour{{ access()!.daysUntilExpiration > 1 ? 's' : '' }}
                  </p>
                </div>
              </div>
            </div>
            s
            <div
              *ngIf="access().needsUpgrade"
              class="bg-gradient-to-r from-purple-50 to-indigo-50 border-l-4 border-purple-400 rounded-r-lg p-4"
            >
              <div class="flex items-center gap-3">
                <div class="w-6 h-6 bg-purple-500 rounded-full flex items-center justify-center">
                  <i class="fas fa-star text-white text-sm"></i>
                </div>
                <div>
                  <p class="font-semibold text-purple-800 text-sm">Upgrade recommandé</p>
                  <p class="text-xs text-purple-700">Débloquez plus de fonctionnalités</p>
                </div>
              </div>
            </div>
          </div>

          <!-- Actions avec icônes -->
          <div class="space-y-3 pt-2">
            <button
              *ngIf="access().needsUpgrade"
              (click)="navigateToUpgrade()"
              class="w-full bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-lg py-3 px-4 font-semibold transition-all duration-300 transform hover:scale-105 hover:shadow-lg flex items-center justify-center gap-2"
            >
              <i class="fas fa-rocket text-sm"></i>
              <span>Améliorer mon plan</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class SubscriptionStatusComponent {
  private subscriptionService = inject(SubscriptionService);
  private router = inject(Router);
  access = computed(() => this.subscriptionService.subscriptionAccess());
  detailsVisible = signal(false);

  toggleDetails() {
    this.detailsVisible.set(!this.detailsVisible());
  }

  subscriptionTypeLabel = computed(() => {
    const types: Record<string, string> = {
      FREE: 'Gratuit',
      SMS: 'SMS Plus',
      WHATSAPP: 'WhatsApp Pro',
      PREMIUM: 'Premium',
      NONE: 'Inactif', // <-- ici
    };
    return types[this.access().subscriptionType] || 'Inconnu';
  });

  badgeClass = computed(() => {
    const type = this.access().subscriptionType;
    const baseClasses = 'px-3 py-1.5 rounded-full text-xs font-bold uppercase tracking-wider select-none shadow-sm border';

    switch (type) {
      case 'FREE':
        return `${baseClasses} bg-gradient-to-r from-blue-100 to-blue-200 text-blue-800 border-blue-300`;
      case 'PREMIUM':
        return `${baseClasses} bg-gradient-to-r from-purple-100 to-purple-200 text-purple-800 border-purple-300`;
      case 'SMS':
        return `${baseClasses} bg-gradient-to-r from-emerald-100 to-emerald-200 text-emerald-800 border-emerald-300`;
      case 'WHATSAPP':
        return `${baseClasses} bg-gradient-to-r from-green-100 to-green-200 text-green-800 border-green-300`;
      default:
        return `${baseClasses} bg-gradient-to-r from-gray-100 to-gray-200 text-gray-800 border-gray-300`;
    }
  });

  badgeIcon = computed(() => {
    const type = this.access().subscriptionType;
    switch (type) {
      case 'FREE':
        return 'fas fa-heart';
      case 'PREMIUM':
        return 'fas fa-crown';
      case 'SMS':
        return 'fas fa-sms';
      case 'WHATSAPP':
        return 'fab fa-whatsapp';
      default:
        return 'fas fa-question';
    }
  });

  badgeIconClass = computed(() => {
    const type = this.access().subscriptionType;
    switch (type) {
      case 'FREE':
        return 'bg-gradient-to-br from-blue-500 to-blue-600 text-white';
      case 'PREMIUM':
        return 'bg-gradient-to-br from-purple-500 to-purple-600 text-white';
      case 'SMS':
        return 'bg-gradient-to-br from-emerald-500 to-emerald-600 text-white';
      case 'WHATSAPP':
        return 'bg-gradient-to-br from-green-500 to-green-600 text-white';
      default:
        return 'bg-gradient-to-br from-gray-500 to-gray-600 text-white';
    }
  });

  getStatusText = computed(() => {
    const access = this.access();

    const sms = access.smsRemaining;
    const wa = access.whatsappRemaining;

    if (sms > 0 && sms <= 100 && wa > 0 && wa <= 100) {
      return 'Expire bientôt';
    }
    if (access.needsUpgrade && (sms === 0 || wa === 0)) {
      return 'Upgrade requis';
    }
    return 'Actif';
  });

  smsPercentage = computed(() => {
    const access = this.access();
    if (!access.canSendSMS) return 0;

    // Estimation basée sur un plan de 100 SMS (à ajuster selon vos plans)
    const estimatedTotal = access.subscriptionType === 'FREE' ? 10 : 100;
    return Math.max(5, Math.min(100, (access.smsRemaining / estimatedTotal) * 100));
  });

  whatsappPercentage = computed(() => {
    const access = this.access();
    if (!access.canSendWhatsApp) return 0;

    // Estimation basée sur un plan de 100 WhatsApp (à ajuster selon vos plans)
    const estimatedTotal = access.subscriptionType === 'FREE' ? 10 : 100;
    return Math.max(5, Math.min(100, (access.whatsappRemaining / estimatedTotal) * 100));
  });

  hasWarnings = computed(() => this.access().isSubscriptionExpiring || this.access().needsUpgrade);

  needsRecharge = computed(() => {
    const access = this.access();
    return access.smsRemaining <= 10 || access.whatsappRemaining <= 10;
  });

  navigateToUpgrade() {
    console.log("Navigation vers la page d'upgrade");
    this.router.navigate(['/upgrade']);
    // TODO: Implémenter navigation réelle vers /subscription/upgrade
  }

  navigateToRecharge() {
    console.log('Navigation vers la page de recharge');
    // TODO: Implémenter navigation réelle vers /subscription/recharge
  }
}
