import { Component, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MenuModule } from 'primeng/menu';
import { MegaMenuModule } from 'primeng/megamenu';
import { Subscription } from 'rxjs';
import { MenuItemWithUpgradeComponent } from './MenuItemWithUpgradeComponent.component';
import { UpgradeSuggestionComponent } from './upgradeSuggestionComponent.component';
import { SubscriptionStatusComponent } from './subscriptionStatusComponent.component';
import { DynamicMenuService } from './service/dynamicMenuService.service';
import { SubscriptionNotificationService } from './service/subscriptionNotificationService.service';

@Component({
  selector: 'app-main-menu',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MenuModule,
    MegaMenuModule,
    SubscriptionStatusComponent,
    UpgradeSuggestionComponent,
    MenuItemWithUpgradeComponent,
  ],
  template: `
    <!-- Notifications d'abonnement -->
    <app-upgrade-suggestion></app-upgrade-suggestion>

    <!-- Menu principal -->
    <div class="main-menu">
      <p-megaMenu [model]="menuItems()" orientation="horizontal" styleClass="custom-megamenu"> </p-megaMenu>
    </div>

    <!-- Statut d'abonnement dans la sidebar -->
    <div class="subscription-sidebar" *ngIf="showSubscriptionStatus()">
      <app-subscription-status></app-subscription-status>
    </div>
  `,
  styles: [
    `
      .main-menu {
        background: white;
        border-bottom: 1px solid #e0e0e0;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .custom-megamenu {
        border: none;
        background: transparent;
      }

      .subscription-sidebar {
        position: fixed;
        top: 80px;
        right: 20px;
        width: 300px;
        z-index: 1000;
      }

      @media (max-width: 768px) {
        .subscription-sidebar {
          position: relative;
          top: 0;
          right: 0;
          width: 100%;
          margin: 20px 0;
        }
      }

      ::ng-deep .custom-megamenu .p-menuitem-link {
        transition: all 0.3s ease;
      }

      ::ng-deep .custom-megamenu .p-menuitem-link.disabled {
        opacity: 0.6;
        pointer-events: none;
      }

      ::ng-deep .custom-megamenu .p-menuitem-link:hover:not(.disabled) {
        background-color: #f0f8ff;
        color: #2196f3;
      }

      ::ng-deep .custom-megamenu .p-badge {
        margin-left: 8px;
      }

      ::ng-deep .custom-megamenu .warning {
        border-bottom: 2px solid #ff9800;
      }

      ::ng-deep .custom-megamenu .upgrade-needed {
        border-bottom: 2px solid #f44336;
      }
    `,
  ],
})
export class MainMenuComponent implements OnInit, OnDestroy {
  private dynamicMenuService = inject(DynamicMenuService);
  private notificationService = inject(SubscriptionNotificationService);
  private subscriptions: Subscription[] = [];

  // Menu items réactif
  menuItems = computed(() => this.dynamicMenuService.items());

  ngOnInit(): void {
    // Initialiser le menu
    this.dynamicMenuService.initializeMenu();

    // Vérifier les notifications périodiquement
    this.checkNotifications();

    // Configurer les vérifications périodiques
    const notificationCheck = setInterval(() => {
      this.checkNotifications();
    }, 300000); // Toutes les 5 minutes

    // Nettoyer à la destruction
    this.subscriptions.push({
      unsubscribe: () => clearInterval(notificationCheck),
    } as Subscription);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  showSubscriptionStatus(): boolean {
    // Afficher le statut d'abonnement pour les partenaires et utilisateurs
    return window.innerWidth > 768; // Seulement sur desktop
  }

  private checkNotifications(): void {
    this.notificationService.checkAndShowNotifications();
  }
}
