// Subscription/subscrptionAcces.model.ts

import { Type } from '@angular/core';

export interface SubscriptionAccess {
  canSendSMS: boolean;
  canSendWhatsApp: boolean;
  canManageUsers: boolean;
  canManageTemplates: boolean;
  canViewConversations: boolean;
  canViewDashboard: boolean;
  canManageAPI: boolean;
  smsRemaining: number;
  whatsappRemaining: number;
  isSubscriptionExpiring: boolean;
  daysUntilExpiration: number;
  subscriptionType: SubscriptionType;
  needsUpgrade: boolean;
  sidebarVisible: boolean;
}
export type SubscriptionType = 'FREE' | 'SMS' | 'WHATSAPP' | 'PREMIUM' | 'NONE';

export interface UserSubscription {
  id: number;
  planName: string | null;
  planType: string;
  status: string | null;
  sidebarVisible: boolean;
  startDate: string | null; // ISO date string
  endDate: string | null;
  createdDate: string | null;

  smsLimit: number;
  whatsappLimit: number | null;

  smsUsed: number | null;
  whatsappUsed: number | null;

  smsRemaining: number | null;
  whatsappRemaining: number | null;

  price: number | null;
  currency: string | null;

  features: string[]; // ex: ['manage-users', 'view-dashboard', ...]

  isActive: boolean | null;
  isExpiringSoon: boolean | null;
  daysUntilExpiration: number | null;

  // Permissions (booléens)
  customCanManageUsers: boolean | null;
  customCanManageTemplates: boolean | null;
  customCanViewConversations: boolean | null;
  customCanViewAnalytics: boolean | null;
  customPrioritySupport: boolean | null;

  canViewDashboard: boolean | null;
  canManageAPI: boolean | null;

  // Bonus
  bonusSmsEnabled: boolean | null;
  bonusSmsAmount: number | null;
  bonusWhatsappEnabled: boolean | null;
  bonusWhatsappAmount: number | null;

  // Carryover
  allowSmsCarryover: boolean | null;
  allowWhatsappCarryover: boolean | null;
  carriedOverSms: number | null;
  carriedOverWhatsapp: number | null;
}

export interface AccessRestriction {
  feature: string;
  allowed: boolean;
  reason?: string;
  upgradeRequired?: boolean;
}

// ===== INTERFACE SIMPLIFIÉE POUR LES MENUS =====

// Interface pour les éléments de sous-menu
export interface DynamicMegaMenuSubItem {
  label: string;
  routerLink?: string[];
  styleClass?: string;
  icon?: string;
  tooltip?: string;
  disabled?: boolean;
  upgradeRequired?: boolean;
  feature?: string;
}

// Interface principale compatible avec PrimeNG MegaMenu
export interface DynamicMegaMenuItem {
  label: string;
  icon?: string;
  iconComponent?: Type<any>;
  routerLink?: string[];
  styleClass?: string;
  visible?: boolean;
  badge?: string;
  badgeStyleClass?: string;
  tooltip?: string;
  upgradeRequired?: boolean;
  disabled?: boolean;
  feature?: string;

  // ===== STRUCTURE COMPATIBLE PRIMENG =====
  // Pour les sous-menus, on utilise la structure PrimeNG standard
  items?: any[][];
}

// ===== TYPES UTILITAIRES =====

export type MenuItemType = 'simple' | 'submenu' | 'separator';

export interface MenuItemConfig {
  type: MenuItemType;
  requiresAuth: boolean;
  requiredRoles?: string[];
  requiredFeatures?: string[];
  fallbackRoute?: string;
}
