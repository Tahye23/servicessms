export interface IUser {
  id: number | null;
  login?: string;
  firstName?: string | null;
  lastName?: string | null;
  email?: string;
  activated?: boolean;
  langKey?: string;
  authorities?: string[];
  expediteur?: string;
  phone?: string;
  permissions?: string; // Nouveau champ pour stocker les permissions JSON
  createdBy?: string;
  createdDate?: Date;
  lastModifiedBy?: string;
  lastModifiedDate?: Date;
  customFields?: ICustomField[];
}

export interface ICustomField {
  label: string;
  apiName: string;
}

export class User implements IUser {
  constructor(
    public id: number | null,
    public login?: string,
    public firstName?: string | null,
    public lastName?: string | null,
    public email?: string,
    public activated?: boolean,
    public langKey?: string,
    public authorities?: string[],
    public expediteur?: string,
    public phone?: string,
    public permissions?: string, // Nouveau champ
    public createdBy?: string,
    public createdDate?: Date,
    public lastModifiedBy?: string,
    public lastModifiedDate?: Date,
    public customFields?: ICustomField[],
  ) {}
}

// Enum pour les permissions disponibles
export enum UserPermissions {
  CAN_VIEW_DASHBOARD = 'canViewDashboard',
  CAN_SEND_SMS = 'canSendSMS',
  CAN_SEND_WHATSAPP = 'canSendWhatsApp',
  CAN_VIEW_CONVERSATIONS = 'canViewConversations',
  CAN_MANAGE_TEMPLATES = 'canManageTemplates',
  CAN_MANAGE_CONTACTS = 'canManageContacts',
  CAN_MANAGE_GROUPS = 'canManageGroups',
  CAN_MANAGE_USERS = 'canManageUsers',
  CAN_MANAGE_API = 'canManageAPI',
  CAN_MANAGE_SUBSCRIPTIONS = 'canManageSubscriptions',
  CAN_VIEW_CONFIG = 'canViewConfig',
}

// Utilitaires pour les permissions
export class UserPermissionUtils {
  /**
   * Parser les permissions depuis JSON
   */
  static parsePermissions(permissionsJson: string): string[] {
    try {
      return JSON.parse(permissionsJson) || [];
    } catch {
      return [];
    }
  }

  /**
   * Vérifier si un utilisateur a une permission spécifique
   */
  static hasPermission(user: IUser, permission: UserPermissions): boolean {
    if (!user.permissions) return false;
    const permissions = this.parsePermissions(user.permissions);
    return permissions.includes(permission);
  }

  /**
   * Vérifier si un utilisateur peut accéder à une fonctionnalité du menu
   */
  static canAccessMenuItem(user: IUser, feature: string): boolean {
    const permissionMap: { [key: string]: UserPermissions } = {
      dashboard: UserPermissions.CAN_VIEW_DASHBOARD,
      'send-sms': UserPermissions.CAN_SEND_SMS,
      'send-whatsapp': UserPermissions.CAN_SEND_WHATSAPP,
      conversations: UserPermissions.CAN_VIEW_CONVERSATIONS,
      templates: UserPermissions.CAN_MANAGE_TEMPLATES,
      contacts: UserPermissions.CAN_MANAGE_CONTACTS,
      groups: UserPermissions.CAN_MANAGE_GROUPS,
      'manage-users': UserPermissions.CAN_MANAGE_USERS,
      api: UserPermissions.CAN_MANAGE_API,
      subscriptions: UserPermissions.CAN_MANAGE_SUBSCRIPTIONS,
      config: UserPermissions.CAN_VIEW_CONFIG,
    };

    const permission = permissionMap[feature];
    return permission ? this.hasPermission(user, permission) : false;
  }
}
