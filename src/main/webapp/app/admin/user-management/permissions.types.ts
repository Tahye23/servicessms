// types/permissions.types.ts
export interface Permission {
  id: string;
  label: string;
  description?: string;
  icon: string;
  category: string;
  children?: Permission[];
  defaultChecked?: boolean;
}

export interface PermissionCategory {
  id: string;
  label: string;
  icon: string;
  color: string;
  permissions: Permission[];
}

// constants/permissions.constants.ts
export const PERMISSION_CATEGORIES: PermissionCategory[] = [
  {
    id: 'core',
    label: 'Fonctionnalités principales',
    icon: '⚡',
    color: 'blue',
    permissions: [
      {
        id: 'dashboard',
        label: 'Tableau de bord',
        icon: '📊',
        category: 'core',
        children: [
          { id: 'dashboard.view', label: 'Voir', icon: '👁️', category: 'core', defaultChecked: true },
          { id: 'dashboard.export', label: 'Exporter', icon: '📤', category: 'core' },
        ],
      },
    ],
  },
  {
    id: 'messaging',
    label: 'Messagerie',
    icon: '💬',
    color: 'green',
    permissions: [
      {
        id: 'sms',
        label: 'SMS',
        icon: '📱',
        category: 'messaging',
        children: [
          { id: 'sms.create', label: 'Créer', icon: '➕', category: 'messaging', defaultChecked: true },
          { id: 'sms.edit', label: 'Modifier', icon: '✏️', category: 'messaging', defaultChecked: true },
          { id: 'sms.delete', label: 'Supprimer', icon: '🗑️', category: 'messaging' },
          { id: 'sms.send.single', label: 'Envoi unitaire', icon: '📤', category: 'messaging', defaultChecked: true },
          { id: 'sms.send.bulk', label: 'Envoi en masse', icon: '📦', category: 'messaging' },
        ],
      },
      {
        id: 'whatsapp',
        label: 'WhatsApp',
        icon: '💬',
        category: 'messaging',
        children: [
          { id: 'whatsapp.create', label: 'Créer', icon: '➕', category: 'messaging', defaultChecked: true },
          { id: 'whatsapp.edit', label: 'Modifier', icon: '✏️', category: 'messaging', defaultChecked: true },
          { id: 'whatsapp.delete', label: 'Supprimer', icon: '🗑️', category: 'messaging' },
          { id: 'whatsapp.send.single', label: 'Envoi unitaire', icon: '📤', category: 'messaging', defaultChecked: true },
          { id: 'whatsapp.send.bulk', label: 'Envoi en masse', icon: '📦', category: 'messaging' },
          { id: 'whatsapp.send.marketing', label: 'Envoi Marketing', icon: '📈', category: 'messaging' },
          { id: 'whatsapp.send.otp', label: 'Envoi OTP', icon: '🔐', category: 'messaging' },
          { id: 'whatsapp.send.unit', label: 'Envoi Unitaire', icon: '🎯', category: 'messaging' },
        ],
      },
      {
        id: 'conversations',
        label: 'Conversations',
        icon: '💭',
        category: 'messaging',
        children: [
          { id: 'conversations.view', label: 'Voir', icon: '👁️', category: 'messaging', defaultChecked: true },
          { id: 'conversations.respond', label: 'Répondre', icon: '↩️', category: 'messaging' },
          { id: 'conversations.archive', label: 'Archiver', icon: '📁', category: 'messaging' },
        ],
      },
    ],
  },
  {
    id: 'content',
    label: 'Gestion de contenu',
    icon: '📄',
    color: 'purple',
    permissions: [
      {
        id: 'templates',
        label: 'Modèles',
        icon: '📄',
        category: 'content',
        children: [
          { id: 'templates.create', label: 'Créer', icon: '➕', category: 'content', defaultChecked: true },
          { id: 'templates.edit', label: 'Modifier', icon: '✏️', category: 'content', defaultChecked: true },
          { id: 'templates.delete', label: 'Supprimer', icon: '🗑️', category: 'content' },
          { id: 'templates.approved', label: ' Approuver', icon: '✏️', category: 'content' },
          { id: 'templates.send.sms', label: ' SMS', icon: '📱', category: 'content' },
          { id: 'templates.send.whatsapp', label: ' WhatsApp', icon: '💬', category: 'content' },
        ],
      },
      {
        id: 'contacts',
        label: 'Contacts',
        icon: '👥',
        category: 'content',
        children: [
          { id: 'contacts.create', label: 'Créer', icon: '➕', category: 'content', defaultChecked: true },
          { id: 'contacts.edit', label: 'Modifier', icon: '✏️', category: 'content', defaultChecked: true },
          { id: 'contacts.delete', label: 'Supprimer', icon: '🗑️', category: 'content' },
          { id: 'contacts.import', label: 'Importer', icon: '📥', category: 'content' },
          { id: 'contacts.export', label: 'Exporter', icon: '📤', category: 'content' },
        ],
      },
      {
        id: 'groups',
        label: 'Groupes',
        icon: '👨‍👩‍👧‍👦',
        category: 'content',
        children: [
          { id: 'groups.create', label: 'Créer', icon: '➕', category: 'content', defaultChecked: true },
          { id: 'groups.edit', label: 'Modifier', icon: '✏️', category: 'content', defaultChecked: true },
          { id: 'groups.delete', label: 'Supprimer', icon: '🗑️', category: 'content' },
          { id: 'groups.manage.members', label: 'Gérer les membres', icon: '👥', category: 'content' },
        ],
      },
    ],
  },
  {
    id: 'admin',
    label: 'Administration',
    icon: '⚙️',
    color: 'red',
    permissions: [
      {
        id: 'users',
        label: 'Utilisateurs',
        icon: '👤',
        category: 'admin',
        children: [
          { id: 'users.create', label: 'Créer', icon: '➕', category: 'admin' },
          { id: 'users.edit', label: 'Modifier', icon: '✏️', category: 'admin' },
          { id: 'users.delete', label: 'Supprimer', icon: '🗑️', category: 'admin' },
          { id: 'users.view', label: 'Voir', icon: '👁️', category: 'admin' },
        ],
      },
      {
        id: 'applications',
        label: 'Applications/API',
        icon: '🔌',
        category: 'admin',
        children: [
          { id: 'applications.create', label: 'Créer', icon: '➕', category: 'admin' },
          { id: 'applications.edit', label: 'Modifier', icon: '✏️', category: 'admin' },
          { id: 'applications.delete', label: 'Supprimer', icon: '🗑️', category: 'admin' },
          { id: 'applications.view', label: 'Voir', icon: '👁️', category: 'admin' },
        ],
      },
      {
        id: 'subscriptions',
        label: 'Abonnements',
        icon: '👑',
        category: 'admin',
        children: [
          { id: 'subscriptions.create', label: 'Créer', icon: '➕', category: 'admin' },
          { id: 'subscriptions.edit', label: 'Modifier', icon: '✏️', category: 'admin' },
          { id: 'subscriptions.delete', label: 'Supprimer', icon: '🗑️', category: 'admin' },
          { id: 'subscriptions.view', label: 'Voir', icon: '👁️', category: 'admin' },
        ],
      },
      {
        id: 'config',
        label: 'Configuration système',
        icon: '⚙️',
        category: 'admin',
        children: [
          { id: 'config.view', label: 'Voir', icon: '👁️', category: 'admin' },
          { id: 'config.edit', label: 'Modifier', icon: '✏️', category: 'admin' },
        ],
      },
    ],
  },
];

// services/permissions.service.ts
export class PermissionsService {
  private flattenPermissions(categories: PermissionCategory[]): Permission[] {
    const permissions: Permission[] = [];

    categories.forEach(category => {
      category.permissions.forEach(permission => {
        permissions.push(permission);
        if (permission.children) {
          permissions.push(...permission.children);
        }
      });
    });

    return permissions;
  }

  getAllPermissions(): Permission[] {
    return this.flattenPermissions(PERMISSION_CATEGORIES);
  }

  getPermissionsByRole(role: string): string[] {
    const allPermissions = this.getAllPermissions();

    switch (role) {
      case 'ROLE_ADMIN':
        return allPermissions.map(p => p.id);

      case 'ROLE_PARTNER':
        return allPermissions.filter(p => p.category !== 'admin' || p.id.includes('users')).map(p => p.id);

      case 'ROLE_USER':
      default:
        return allPermissions.filter(p => p.defaultChecked && p.category !== 'admin').map(p => p.id);
    }
  }

  getPermissionsByCategory(categoryId: string): Permission[] {
    const category = PERMISSION_CATEGORIES.find(c => c.id === categoryId);
    return category ? category.permissions : [];
  }

  getPermissionLabel(permissionId: string): string {
    const permission = this.getAllPermissions().find(p => p.id === permissionId);
    return permission ? `${permission.icon} ${permission.label}` : permissionId;
  }

  isParentPermission(permissionId: string): boolean {
    return this.getAllPermissions().some(p => p.children?.some(c => c.id === permissionId));
  }

  getParentPermission(childPermissionId: string): Permission | null {
    for (const category of PERMISSION_CATEGORIES) {
      for (const permission of category.permissions) {
        if (permission.children?.some(c => c.id === childPermissionId)) {
          return permission;
        }
      }
    }
    return null;
  }

  getChildPermissions(parentPermissionId: string): Permission[] {
    const permission = this.getAllPermissions().find(p => p.id === parentPermissionId);
    return permission?.children || [];
  }
}
