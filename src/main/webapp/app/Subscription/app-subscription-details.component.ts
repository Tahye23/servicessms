import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { UserSubscription } from '../Subscription/subscrptionAcces.model';
import { SubscriptionService } from './service/subscriptionService.service';

@Component({
  selector: 'app-subscription-details',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen p-4 md:p-8">
      <div class="max-w-6xl mx-auto">
        <!-- Header avec retour -->
        <div class="flex items-center justify-between mb-8">
          <div class="flex items-center space-x-4">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">Mon Abonnement</h1>
              <p class="text-gray-600 mt-1">Gérez votre plan et vos crédits</p>
            </div>
          </div>

          <div class="flex space-x-3">
            <button
              (click)="downloadInvoice()"
              class="px-4 py-2 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <i class="fas fa-download mr-2"></i>
              Facture
            </button>
            <button
              (click)="navigateToUpgrade()"
              class="px-6 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-md hover:shadow-lg"
            >
              <i class="fas fa-star mr-2"></i>
              Améliorer
            </button>
          </div>
        </div>

        <!-- Nouvelle section : Paramètres d'affichage -->
        <div class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl shadow-lg p-6 mb-8 border border-blue-200">
          <div class="flex items-center justify-between">
            <div class="flex items-center space-x-4">
              <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg">
                <i class="fas fa-cog text-white text-xl"></i>
              </div>
              <div>
                <h3 class="text-lg font-bold text-gray-900">Paramètres d'affichage</h3>
                <p class="text-sm text-gray-600">Personnalisez l'affichage de votre statut d'abonnement</p>
              </div>
            </div>

            <!-- Toggle Switch avec Tooltip -->
            <div class="relative group">
              <!-- Tooltip -->
              <div
                class="absolute bottom-full right-0 mb-3 w-80 bg-gray-900 text-white text-sm rounded-xl p-4 shadow-2xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 z-50"
              >
                <div class="flex items-start space-x-3">
                  <div class="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center flex-shrink-0">
                    <i class="fas fa-info text-white text-sm"></i>
                  </div>
                  <div>
                    <h4 class="font-semibold text-white mb-2">Affichage dans la sidebar</h4>
                    <p class="text-gray-300 text-xs leading-relaxed mb-3">
                      Choisissez si vous souhaitez afficher le widget de statut d'abonnement dans votre barre latérale.
                    </p>
                    <div class="flex items-center space-x-2 text-xs">
                      <div class="flex items-center space-x-1">
                        <div class="w-2 h-2 bg-green-400 rounded-full"></div>
                        <span class="text-green-300">Activé : Widget visible</span>
                      </div>
                      <div class="flex items-center space-x-1">
                        <div class="w-2 h-2 bg-red-400 rounded-full"></div>
                        <span class="text-red-300">Désactivé : Widget masqué</span>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- Flèche du tooltip -->
                <div
                  class="absolute top-full right-6 w-0 h-0 border-l-4 border-r-4 border-t-4 border-l-transparent border-r-transparent border-t-gray-900"
                ></div>
              </div>

              <!-- Toggle Switch -->
              <div class="flex items-center space-x-3">
                <span class="text-sm font-medium text-gray-700"> Statut dans la sidebar </span>
                <button
                  (click)="toggleSidebarStatus()"
                  class="relative inline-flex h-7 w-12 items-center rounded-full transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                  [ngClass]="{
                    'bg-gradient-to-r from-blue-500 to-indigo-600 shadow-lg': showSidebarStatus(),
                    'bg-gray-300': !showSidebarStatus()
                  }"
                  [attr.aria-checked]="showSidebarStatus()"
                  role="switch"
                >
                  <span class="sr-only">Afficher le statut dans la sidebar</span>
                  <span
                    class="inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition-transform duration-300 ease-in-out"
                    [ngClass]="{
                      'translate-x-6': showSidebarStatus(),
                      'translate-x-1': !showSidebarStatus()
                    }"
                  >
                    <!-- Icône à l'intérieur du toggle -->
                    <i
                      class="absolute inset-0 flex items-center justify-center text-xs transition-colors duration-300"
                      [ngClass]="{
                        'fas fa-eye text-blue-600': showSidebarStatus(),
                        'fas fa-eye-slash text-gray-400': !showSidebarStatus()
                      }"
                    ></i>
                  </span>
                </button>

                <!-- Indicateur de statut -->
                <div class="flex items-center space-x-2">
                  <div
                    class="w-2 h-2 rounded-full animate-pulse"
                    [ngClass]="{
                      'bg-green-500': showSidebarStatus(),
                      'bg-red-500': !showSidebarStatus()
                    }"
                  ></div>
                  <span
                    class="text-xs font-medium"
                    [ngClass]="{
                      'text-green-600': showSidebarStatus(),
                      'text-red-600': !showSidebarStatus()
                    }"
                  >
                    {{ showSidebarStatus() ? 'Visible' : 'Masqué' }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Message de confirmation après changement -->
          <div *ngIf="showConfirmationMessage()" class="mt-4 p-3 bg-white rounded-lg border border-blue-200 shadow-sm animate-fade-in">
            <div class="flex items-center space-x-2">
              <i class="fas fa-check-circle text-green-500"></i>
              <span class="text-sm font-medium text-gray-700">
                Paramètres mis à jour ! Le statut est maintenant
                <span
                  class="font-bold"
                  [ngClass]="{
                    'text-green-600': showSidebarStatus(),
                    'text-red-600': !showSidebarStatus()
                  }"
                >
                  {{ showSidebarStatus() ? 'visible' : 'masqué' }}
                </span>
                dans la sidebar.
              </span>
            </div>
          </div>
        </div>

        <!-- Grille des statistiques -->
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6 mb-8">
          <!-- SMS -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <i class="fas fa-sms text-green-600 text-xl"></i>
              </div>
              <span class="text-sm font-medium text-gray-500">SMS</span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-gray-600">Utilisés</span>
                <span class="font-medium">{{ currentSubscription()?.smsUsed || 0 }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-600">Restants</span>
                <span class="font-bold text-green-600">{{ getSmsRemaining() }}</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-3">
                <div
                  class="bg-gradient-to-r from-green-500 to-green-600 h-3 rounded-full transition-all duration-500 shadow-sm"
                  [style.width.%]="getSmsUsagePercentage()"
                ></div>
              </div>
              <div class="text-xs text-gray-500 text-center mt-2">{{ getSmsUsagePercentage() | number: '1.0-0' }}% utilisé</div>
            </div>
          </div>

          <!-- WhatsApp -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-emerald-100 rounded-lg flex items-center justify-center">
                <i class="fab fa-whatsapp text-emerald-600 text-xl"></i>
              </div>
              <span class="text-sm font-medium text-gray-500">WhatsApp</span>
            </div>
            <div class="space-y-2">
              <div class="flex justify-between text-sm">
                <span class="text-gray-600">Utilisés</span>
                <span class="font-medium">{{ currentSubscription()?.whatsappUsed || 0 }}</span>
              </div>
              <div class="flex justify-between text-sm">
                <span class="text-gray-600">Restants</span>
                <span class="font-bold text-emerald-600">{{ getWhatsappRemaining() }}</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-3">
                <div
                  class="bg-gradient-to-r from-emerald-500 to-emerald-600 h-3 rounded-full transition-all duration-500 shadow-sm"
                  [style.width.%]="getWhatsappUsagePercentage()"
                ></div>
              </div>
              <div class="text-xs text-gray-500 text-center mt-2">{{ getWhatsappUsagePercentage() | number: '1.0-0' }}% utilisé</div>
            </div>
          </div>

          <!-- Status Card avec badge -->
          <div class="bg-white rounded-2xl shadow-lg p-6 border border-gray-200 hover:shadow-xl transition-shadow duration-300">
            <!-- Header badge -->
            <div class="flex items-center gap-3 mb-4">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center transition-colors duration-300"
                [ngClass]="badgeIconClass()"
                aria-label="Type d'abonnement"
              >
                <i [class]="badgeIcon()" class="text-base"></i>
              </div>
              <span
                class="px-4 py-1.5 rounded-full text-sm font-semibold uppercase tracking-wide select-none shadow-sm"
                [ngClass]="badgeClass()"
              >
                {{ subscriptionTypeLabel() }}
              </span>
            </div>

            <!-- Status -->
            <div class="flex items-center gap-3">
              <span
                class="w-3 h-3 rounded-full animate-pulse transition-colors duration-300"
                [ngClass]="{
                  'bg-emerald-500': !access().needsUpgrade && !access().isSubscriptionExpiring,
                  'bg-amber-500': access().smsRemaining <= 100 && access().whatsappRemaining <= 100,
                  'bg-red-500': access().needsUpgrade || access().isSubscriptionExpiring
                }"
                aria-hidden="true"
              ></span>
              <span
                class="font-semibold text-base transition-colors duration-300"
                [ngClass]="{
                  'text-emerald-700': !access().needsUpgrade && !access().isSubscriptionExpiring,
                  'text-amber-700': access().smsRemaining <= 100 && access().whatsappRemaining <= 100,
                  'text-red-700': access().needsUpgrade || access().isSubscriptionExpiring
                }"
              >
                {{ getStatus() }}
              </span>
            </div>
          </div>

          <!-- Économies -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow duration-300">
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
                <i class="fas fa-piggy-bank text-purple-600 text-xl"></i>
              </div>
              <span class="text-sm font-medium text-gray-500">Tarif</span>
            </div>
            <div class="space-y-2">
              <div class="text-2xl font-bold text-purple-600">{{ getSavings() }} MRU</div>
              <div class="text-sm text-gray-600">Prix mensuel</div>
            </div>
          </div>
        </div>

        <!-- Fonctionnalités incluses -->
        <div class="bg-white rounded-2xl shadow-xl p-6 md:p-8 mb-8 border border-gray-100">
          <h3 class="text-xl font-bold text-gray-900 mb-6">Fonctionnalités incluses</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div *ngFor="let feature of currentSubscription()?.features" class="flex items-center space-x-3">
              <div class="w-6 h-6 bg-green-100 rounded-full flex items-center justify-center">
                <i class="fas fa-check text-green-600 text-sm"></i>
              </div>
              <span class="text-gray-700">{{ feature }}</span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <!-- Gérer l'abonnement -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow">
            <div class="text-center">
              <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <i class="fas fa-cog text-blue-600 text-2xl"></i>
              </div>
              <h4 class="text-lg font-semibold text-gray-900 mb-2">Gérer l'abonnement</h4>
              <p class="text-gray-600 text-sm mb-4">Modifiez vos paramètres d'abonnement et de facturation</p>
              <button
                (click)="navigateToBilling()"
                class="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                Gérer
              </button>
            </div>
          </div>

          <!-- Historique des paiements -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow">
            <div class="text-center">
              <div class="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <i class="fas fa-history text-green-600 text-2xl"></i>
              </div>
              <h4 class="text-lg font-semibold text-gray-900 mb-2">Historique</h4>
              <p class="text-gray-600 text-sm mb-4">Consultez vos factures et l'historique des paiements</p>
              <button
                (click)="navigateToPaymentHistory()"
                class="w-full px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
              >
                Voir l'historique
              </button>
            </div>
          </div>

          <!-- Support -->
          <div class="bg-white rounded-xl shadow-lg p-6 border border-gray-100 hover:shadow-xl transition-shadow">
            <div class="text-center">
              <div class="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <i class="fas fa-headset text-purple-600 text-2xl"></i>
              </div>
              <h4 class="text-lg font-semibold text-gray-900 mb-2">Besoin d'aide ?</h4>
              <p class="text-gray-600 text-sm mb-4">Contactez notre équipe support pour toute question</p>
              <button
                (click)="contactSupport()"
                class="w-full px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
              >
                Contacter
              </button>
            </div>
          </div>
        </div>

        <!-- Section d'annulation (si applicable) -->
        <div *ngIf="canCancelSubscription()" class="mt-8 p-6 bg-red-50 rounded-xl border border-red-200">
          <div class="flex items-center justify-between">
            <div>
              <h4 class="text-lg font-semibold text-red-900">Annuler l'abonnement</h4>
              <p class="text-red-700 text-sm mt-1">Vous pouvez annuler votre abonnement à tout moment</p>
            </div>
            <button (click)="cancelSubscription()" class="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors">
              Annuler l'abonnement
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      @keyframes fade-in {
        from {
          opacity: 0;
          transform: translateY(-10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .animate-fade-in {
        animation: fade-in 0.5s ease-out;
      }
    `,
  ],
})
export class SubscriptionDetailsComponent implements OnInit {
  // Signaux existants
  currentSubscription = signal<UserSubscription | null>(null);
  isLoading = signal<boolean>(true);
  access = computed(() => this.subscriptionService.subscriptionAccess());

  // Nouveaux signaux pour la gestion de l'affichage
  showSidebarStatus = signal<boolean>(true);
  showConfirmationMessage = signal<boolean>(false);

  constructor(
    private router: Router,
    private subscriptionService: SubscriptionService,
  ) {
    // Charger la préférence depuis localStorage
    this.loadSidebarPreference();
  }

  ngOnInit(): void {
    this.loadSubscriptionDetails();
  }

  // Nouvelle méthode pour toggle le statut sidebar
  toggleSidebarStatus(): void {
    const newValue = !this.showSidebarStatus();
    this.showSidebarStatus.set(newValue);

    // Sauvegarder la préférence localement
    this.saveSidebarPreference(newValue);

    // Appeler le backend pour mettre à jour
    this.subscriptionService.updateSidebarStatus(newValue).subscribe({
      next: () => {
        // Message de confirmation si besoin

        const current = this.subscriptionService.currentAccess();
        this.subscriptionService.currentAccess.set({
          ...current,
          sidebarVisible: newValue,
        });
        this.showConfirmationMessage.set(true);
        setTimeout(() => {
          this.showConfirmationMessage.set(false);
        }, 3000);
      },
      error: err => {
        console.error('Erreur lors de la mise à jour du backend', err);
        // Optionnel : revert l'état en cas d'erreur
        this.showSidebarStatus.set(!newValue);
      },
    });
  }

  // Sauvegarder la préférence dans localStorage
  private saveSidebarPreference(show: boolean): void {
    try {
      localStorage.setItem('subscription-sidebar-visible', JSON.stringify(show));
    } catch (error) {
      console.warn('Impossible de sauvegarder la préférence dans localStorage:', error);
    }
  }

  // Charger la préférence depuis localStorage
  private loadSidebarPreference(): void {
    try {
      const saved = localStorage.getItem('subscription-sidebar-visible');
      if (saved !== null) {
        this.showSidebarStatus.set(JSON.parse(saved));
      }
    } catch (error) {
      console.warn('Impossible de charger la préférence depuis localStorage:', error);
      // Valeur par défaut: true
      this.showSidebarStatus.set(true);
    }
  }

  // ===== MÉTHODES EXISTANTES (inchangées) =====

  private loadSubscriptionDetails(): void {
    this.isLoading.set(true);

    this.subscriptionService.loadUserSubscriptions().subscribe(
      (subscriptions: UserSubscription[]) => {
        const activeSubscription = subscriptions.find(sub => sub.isActive === true) || subscriptions[0];
        this.currentSubscription.set(activeSubscription);
        const newValue = activeSubscription.sidebarVisible;
        this.showSidebarStatus.set(newValue);
        this.isLoading.set(false);
      },
      (error: any) => {
        console.error("Erreur lors du chargement de l'abonnement:", error);
        this.isLoading.set(false);
      },
    );
  }

  getSmsRemaining(): number {
    const sub = this.currentSubscription();
    if (!sub) return 0;
    return sub.smsRemaining || 0;
  }

  getWhatsappRemaining(): number {
    const sub = this.currentSubscription();
    if (!sub) return 0;
    return sub.whatsappRemaining || 0;
  }

  getSmsUsagePercentage(): number {
    const sub = this.currentSubscription();
    if (!sub || !sub.smsLimit || sub.smsLimit === 0) return 0;

    const used = sub.smsUsed || 0;
    return Math.min(100, (used / sub.smsLimit) * 100);
  }

  getWhatsappUsagePercentage(): number {
    const sub = this.currentSubscription();
    if (!sub || !sub.whatsappLimit || sub.whatsappLimit === 0) return 0;

    const used = sub.whatsappUsed || 0;
    return Math.min(100, (used / sub.whatsappLimit) * 100);
  }

  getStatus = computed(() => {
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

  subscriptionTypeLabel = computed(() => {
    const types: Record<string, string> = {
      FREE: 'Gratuit',
      SMS: 'SMS Plus',
      WHATSAPP: 'WhatsApp Pro',
      PREMIUM: 'Premium',
      NONE: 'Inactif',
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

  getSavings(): number {
    return this.currentSubscription()?.price ?? 0;
  }

  canCancelSubscription(): boolean {
    const sub = this.currentSubscription();
    if (!sub) return false;
    return sub.isActive === true && sub.planType !== 'free';
  }

  downloadInvoice(): void {
    const sub = this.currentSubscription();
    if (!sub) {
      alert("Aucune donnée d'abonnement disponible");
      return;
    }

    const content = `
    Facture d'abonnement

    Plan : ${sub.planName || 'Non défini'}
    SMS restants : ${sub.smsRemaining}
    WhatsApp restants : ${sub.whatsappRemaining}
    Date de fin : ${this.formatDate(sub.endDate)}
  `;

    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'facture.txt';
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }

  formatDate(date?: string | null): string {
    if (!date) return 'Non défini';

    try {
      const dateObj = new Date(date);
      return new Intl.DateTimeFormat('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      }).format(dateObj);
    } catch {
      return 'Date invalide';
    }
  }

  // Actions
  navigateToUpgrade(): void {
    this.router.navigate(['/upgrade']);
  }

  navigateToBilling(): void {
    // Navigation vers la facturation
  }

  navigateToPaymentHistory(): void {
    // Navigation vers l'historique
  }

  contactSupport(): void {
    // Contacter le support
  }

  cancelSubscription(): void {
    if (confirm('Êtes-vous sûr de vouloir annuler votre abonnement ? Cette action est irréversible.')) {
      console.log("Annulation de l'abonnement...");
    }
  }
}
