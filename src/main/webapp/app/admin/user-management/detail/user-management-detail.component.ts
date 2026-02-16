import { Component, input, inject, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import SharedModule from 'app/shared/shared.module';
import { LANGUAGES } from 'app/config/language.constants';
import { User } from '../user-management.model';
import { UserManagementService } from '../service/user-management.service';
import { AccountService } from 'app/core/auth/account.service';

@Component({
  standalone: true,
  selector: 'jhi-user-mgmt-detail',
  templateUrl: './user-management-detail.component.html',
  imports: [RouterModule, SharedModule, CommonModule],
})
export default class UserManagementDetailComponent implements OnInit, OnDestroy {
  user = input<User | null>(null);

  // États du composant
  showActionsMenu = false;
  showToast = false;
  toastMessage = '';
  private routeur = inject(Router);
  // Services injectés
  private userService = inject(UserManagementService);
  private accountService = inject(AccountService);

  ngOnInit(): void {
    // Initialisation si nécessaire
  }

  ngOnDestroy(): void {
    // Nettoyage si nécessaire
  }

  /**
   * Fermer le menu d'actions quand on clique ailleurs
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    this.showActionsMenu = false;
  }

  /**
   * Obtenir les initiales de l'utilisateur
   */
  getUserInitials(): string {
    const currentUser = this.user();
    if (!currentUser) return '?';

    if (currentUser.firstName) {
      return currentUser.firstName.charAt(0).toUpperCase();
    }

    return currentUser.login?.charAt(0).toUpperCase() || '?';
  }

  /**
   * Obtenir le nom complet de l'utilisateur
   */
  getFullName(): string {
    const currentUser = this.user();
    if (!currentUser) return '';

    const firstName = currentUser.firstName || '';
    const lastName = currentUser.lastName || '';

    if (firstName && lastName) {
      return `${firstName} ${lastName}`;
    }

    return firstName || lastName || currentUser.login || 'Utilisateur';
  }
  resetPasswordUrl: string | null = null;
  loadingResetLink = false;

  loadResetPasswordLink(): void {
    if (!this.user()?.email) return;
    this.loadingResetLink = true;
    this.userService.generateResetPasswordLink(this.user()!.email ?? '').subscribe({
      next: (resetKey: string) => {
        console.log('resetKey:', resetKey);
        if (resetKey && resetKey !== 'Lien généré (si utilisateur trouvé)') {
          const frontendBaseUrl = window.location.origin + '/account/reset/finish?key=';
          this.resetPasswordUrl = frontendBaseUrl + resetKey;
        } else {
          this.resetPasswordUrl = null; // ou un message d'erreur
        }
        this.loadingResetLink = false;
      },
      error: (error: any) => {
        console.error('Erreur:', error);
        this.resetPasswordUrl = null;
        this.loadingResetLink = false;
      },
    });
  }

  /**
   * Obtenir le rôle principal (le plus élevé)
   */
  getPrimaryRole(): string {
    const authorities = this.user()?.authorities || [];

    // Ordre de priorité des rôles
    const roleHierarchy = ['ROLE_ADMIN', 'ROLE_PARTNER', 'ROLE_MANAGER', 'ROLE_MODERATOR', 'ROLE_USER'];

    for (const role of roleHierarchy) {
      if (authorities.includes(role)) {
        return this.getRoleDisplayName(role);
      }
    }

    return 'Utilisateur';
  }

  /**
   * Nom d'affichage pour les rôles
   */
  getRoleDisplayName(role: string): string {
    const roleNames: { [key: string]: string } = {
      ROLE_ADMIN: 'Administrateur',
      ROLE_PARTNER: 'Partenaire',
      ROLE_MANAGER: 'Gestionnaire',
      ROLE_MODERATOR: 'Modérateur',
      ROLE_USER: 'Utilisateur',
    };

    return roleNames[role] || role.replace('ROLE_', '');
  }

  /**
   * Obtenir les permissions de l'utilisateur
   */
  getUserPermissions(): string[] {
    const currentUser = this.user();
    if (!currentUser?.permissions) return [];

    try {
      return JSON.parse(currentUser.permissions);
    } catch {
      return [];
    }
  }

  /**
   * Label d'affichage pour les permissions
   */
  getPermissionLabel(permission: string): string {
    const labels: { [key: string]: string } = {
      canViewDashboard: 'Tableau de bord',
      canSendSMS: 'Envoi SMS',
      canSendWhatsApp: 'Envoi WhatsApp',
      canViewConversations: 'Conversations',
      canManageTemplates: 'Modèles',
      canManageContacts: 'Contacts',
      canManageGroups: 'Groupes',
      canManageUsers: 'Utilisateurs',
      canManageAPI: 'Applications/API',
      canManageSubscriptions: 'Abonnements',
      canViewConfig: 'Configuration',
    };

    return labels[permission] || permission;
  }

  /**
   * Obtenir l'âge du compte
   */
  getAccountAge(): string {
    const currentUser = this.user();
    if (!currentUser?.createdDate) return 'Inconnu';

    const createdDate = new Date(currentUser.createdDate);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - createdDate.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 7) {
      return `${diffDays} jour${diffDays > 1 ? 's' : ''}`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `${weeks} semaine${weeks > 1 ? 's' : ''}`;
    } else if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return `${months} mois`;
    } else {
      const years = Math.floor(diffDays / 365);
      return `${years} an${years > 1 ? 's' : ''}`;
    }
  }

  /**
   * Affichage de la langue
   */
  getLanguageDisplay(langKey: string): string {
    const languages: { [key: string]: string } = {
      fr: 'Français',
      en: 'English',
      es: 'Español',
      de: 'Deutsch',
      it: 'Italiano',
    };

    return languages[langKey] || langKey.toUpperCase();
  }

  /**
   * Résumé de l'utilisateur
   */
  getUserSummary(): string {
    const currentUser = this.user();
    if (!currentUser) return '';

    const role = this.getPrimaryRole();
    const permissions = this.getUserPermissions();
    const status = currentUser.activated ? 'actif' : 'inactif';

    return `${role} avec ${permissions.length} permission${permissions.length > 1 ? 's' : ''} • Compte ${status}`;
  }

  /**
   * Toggle du menu d'actions
   */
  toggleActionsMenu(): void {
    this.showActionsMenu = !this.showActionsMenu;
  }

  /**
   * Copier l'email dans le presse-papiers
   */
  async copyToClipboard(text: string | undefined): Promise<void> {
    try {
      if (typeof text === 'string') {
        await navigator.clipboard.writeText(text);
      }
      this.showToastMessage('Email copié dans le presse-papiers');
    } catch (err) {
      console.error('Erreur lors de la copie:', err);
      this.showToastMessage('Erreur lors de la copie');
    }
  }

  /**
   * Copier les détails de l'utilisateur
   */
  async copyUserDetails(): Promise<void> {
    const currentUser = this.user();
    if (!currentUser) return;

    const details = `
Utilisateur: ${this.getFullName()}
Login: ${currentUser.login}
Email: ${currentUser.email}
Rôle: ${this.getPrimaryRole()}
Statut: ${currentUser.activated ? 'Actif' : 'Inactif'}
Créé le: ${currentUser.createdDate ? new Date(currentUser.createdDate).toLocaleDateString('fr-FR') : 'Inconnu'}
    `.trim();

    try {
      await navigator.clipboard.writeText(details);
      this.showToastMessage('Détails copiés dans le presse-papiers');
    } catch (err) {
      console.error('Erreur lors de la copie:', err);
    }

    this.showActionsMenu = false;
  }

  /**
   * Exporter les données utilisateur en JSON
   */
  exportUserData(): void {
    const currentUser = this.user();
    if (!currentUser) return;

    const exportData = {
      id: currentUser.id,
      login: currentUser.login,
      firstName: currentUser.firstName,
      lastName: currentUser.lastName,
      email: currentUser.email,
      activated: currentUser.activated,
      authorities: currentUser.authorities,
      permissions: this.getUserPermissions(),
      createdDate: currentUser.createdDate,
      lastModifiedDate: currentUser.lastModifiedDate,
    };

    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });

    const link = document.createElement('a');
    link.href = URL.createObjectURL(dataBlob);
    link.download = `user_${currentUser.login}_${new Date().toISOString().split('T')[0]}.json`;
    link.click();

    this.showToastMessage('Données exportées avec succès');
    this.showActionsMenu = false;
  }

  /**
   * Vérifier si l'utilisateur connecté peut toggle l'activation
   */
  canToggleActivation(): boolean {
    const account = this.accountService.trackCurrentAccount()();
    const currentUser = this.user();

    if (!account || !currentUser) return false;

    // L'utilisateur ne peut pas se désactiver lui-même
    if (account.login === currentUser.login) return false;

    // Seuls les ADMIN et PARTNER peuvent activer/désactiver
    return account.authorities.includes('ROLE_ADMIN') || account.authorities.includes('ROLE_PARTNER');
  }

  /**
   * Toggle l'activation de l'utilisateur
   */
  toggleUserActivation(): void {
    const currentUser = this.user();
    if (!currentUser || !this.canToggleActivation()) return;

    if (confirm(`Êtes-vous sûr de vouloir ${currentUser.activated ? 'désactiver' : 'activer'} ce compte ?`)) {
      const updatedUser = { ...currentUser, activated: !currentUser.activated };

      this.userService.update(updatedUser).subscribe({
        next: response => {
          // Mettre à jour l'utilisateur local (si possible via un signal)
          this.showToastMessage(`Compte ${updatedUser.activated ? 'activé' : 'désactivé'} avec succès`);
          // Note: Idéalement, vous devriez émettre un événement pour mettre à jour l'utilisateur dans le parent
        },
        error: error => {
          console.error('Erreur lors de la mise à jour:', error);
          this.showToastMessage('Erreur lors de la mise à jour du compte');
        },
      });
    }

    this.showActionsMenu = false;
  }

  /**
   * Afficher un message toast
   */
  goToDetail() {
    const login = this.user()?.login;
    if (login) {
      this.routeur.navigate(['/admin/user-management', login, 'edit']);
    }
  }
  private showToastMessage(message: string): void {
    this.toastMessage = message;
    this.showToast = true;

    setTimeout(() => {
      this.showToast = false;
    }, 3000);
  }

  /**
   * Obtenir la couleur du statut
   */
  getStatusColor(): string {
    return this.user()?.activated ? 'text-green-600' : 'text-red-600';
  }

  /**
   * Vérifier si l'utilisateur a une permission spécifique
   */
  hasPermission(permission: string): boolean {
    return this.getUserPermissions().includes(permission);
  }

  /**
   * Obtenir les statistiques rapides
   */
  getQuickStats() {
    const currentUser = this.user();
    if (!currentUser) return null;

    return {
      accountAge: this.getAccountAge(),
      status: currentUser.activated ? 'Actif' : 'Inactif',
      rolesCount: currentUser.authorities?.length || 0,
      permissionsCount: this.getUserPermissions().length,
    };
  }

  /**
   * Formater une date pour l'affichage
   */
  formatDate(date: Date | string | undefined): string {
    if (!date) return 'Non défini';

    const dateObj = typeof date === 'string' ? new Date(date) : date;
    return dateObj.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
