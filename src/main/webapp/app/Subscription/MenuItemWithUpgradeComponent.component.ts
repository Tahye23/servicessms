import { Component, Input, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DynamicMegaMenuItem } from './subscrptionAcces.model';

@Component({
  selector: 'app-menu-item-with-upgrade',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="menu-item-wrapper" [ngClass]="itemClass">
      <a
        [routerLink]="getRouterLink()"
        class="menu-link"
        [class.disabled]="item.disabled"
        (click)="handleClick($event)"
        [title]="item.tooltip || ''"
      >
        <i [class]="item.icon" *ngIf="item.icon"></i>
        <span class="menu-label">{{ item.label }}</span>

        <span *ngIf="item.badge" class="menu-badge" [ngClass]="item.badgeStyleClass">
          {{ item.badge }}
        </span>

        <i class="pi pi-external-link upgrade-icon" *ngIf="item.upgradeRequired" title="Mise à niveau requise"> </i>
      </a>

      <!-- Overlay d'upgrade -->
      <div class="upgrade-overlay" *ngIf="item.upgradeRequired && item.disabled">
        <button class="upgrade-btn" (click)="showUpgrade()">
          <i class="pi pi-star"></i>
          Améliorer
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .menu-item-wrapper {
        position: relative;
      }

      .menu-link {
        display: flex;
        align-items: center;
        padding: 12px 16px;
        text-decoration: none;
        color: #333;
        transition: all 0.3s ease;
        border-radius: 6px;
        margin: 2px 0;
      }

      .menu-link:hover:not(.disabled) {
        background-color: #f5f5f5;
        color: #2196f3;
      }

      .menu-link.disabled {
        opacity: 0.6;
        cursor: not-allowed;
        background-color: #fafafa;
      }

      .menu-link i {
        margin-right: 10px;
        width: 20px;
        text-align: center;
      }

      .menu-label {
        flex: 1;
      }

      .menu-badge {
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 11px;
        font-weight: bold;
        margin-left: 8px;
      }

      .badge-success {
        background: #4caf50;
        color: white;
      }
      .badge-warning {
        background: #ff9800;
        color: white;
      }
      .badge-danger {
        background: #f44336;
        color: white;
      }
      .badge-disabled {
        background: #bdbdbd;
        color: white;
      }

      .upgrade-icon {
        margin-left: 8px;
        color: #ff9800;
        font-size: 12px;
      }

      .upgrade-overlay {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        background: rgba(255, 255, 255, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 6px;
      }

      .upgrade-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border: none;
        padding: 6px 12px;
        border-radius: 20px;
        font-size: 12px;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 4px;
        transition: transform 0.2s ease;
      }

      .upgrade-btn:hover {
        transform: scale(1.05);
      }

      .warning {
        border-left: 3px solid #ff9800;
      }

      .upgrade-needed {
        border-left: 3px solid #f44336;
      }
    `,
  ],
})
export class MenuItemWithUpgradeComponent {
  @Input() item!: DynamicMegaMenuItem;

  private router = inject(Router);

  get itemClass(): string {
    return this.item.styleClass || '';
  }

  getRouterLink(): string[] | null {
    if (this.item.upgradeRequired && !this.canAccess()) {
      return ['/upgrade'];
    }
    return this.item.routerLink || null;
  }

  handleClick(event: Event): void {
    if (this.item.disabled) {
      event.preventDefault();
      event.stopPropagation();
      this.showUpgrade();
    } else if (this.item.upgradeRequired && !this.canAccess()) {
      event.preventDefault();
      this.navigateToUpgrade();
    }
  }

  private canAccess(): boolean {
    // Logique pour déterminer si l'utilisateur peut accéder à la fonctionnalité
    return !this.item.disabled;
  }

  showUpgrade(): void {
    this.navigateToUpgrade();
  }

  private navigateToUpgrade(): void {
    this.router.navigate(['/upgrade'], {
      queryParams: {
        feature: this.item.feature,
        reason: this.item.tooltip,
      },
    });
  }
}
