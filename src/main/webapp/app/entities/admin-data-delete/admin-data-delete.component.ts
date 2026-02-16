import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DataService, GlobalMigrationResult, MigrationResult, QuotaInfo, UpdateQuotaResponse } from './data.service';
import { FormsModule } from '@angular/forms';
import { NgClass, NgForOf, NgIf } from '@angular/common';
import { AccountService } from '../../core/auth/account.service';

@Component({
  selector: 'app-admin-data-delete',
  templateUrl: './admin-data-delete.component.html',
  standalone: true,
  imports: [FormsModule, NgIf, NgClass, NgForOf],
})
export class AdminDataDeleteComponent {
  codeSecret = '';
  accessGranted = false;
  private accountService = inject(AccountService);
  readonly correctSecretCode = 'Richatt2025!';

  // Migration
  migrationUserLogin = '';
  isLoadingMigration = false;
  migrationMessage = '';
  migrationSuccess = false;

  // Général
  sendSmsId: number | null = null;
  groupeId: number | null = null;
  userLogin: string = '';
  isLoading = false;
  message: string = '';
  success: boolean = true;

  // Gestion des quotas
  quotaUserLogin = '';
  isLoadingQuota = false;
  quotaMessage = '';
  quotaSuccess = false;
  quotaInfo: QuotaInfo | null = null;

  // Pour augmenter les quotas
  smsIncrease: number | null = null;
  whatsappIncrease: number | null = null;

  // Pour remplacer les quotas
  newSmsLimit: number | null = null;
  newWhatsappLimit: number | null = null;

  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));

  constructor(
    private dataService: DataService,
    private router: Router,
  ) {}

  verifySecretCode() {
    if (this.codeSecret === this.correctSecretCode) {
      this.accessGranted = true;
    } else {
      alert('Code secret incorrect.');
    }
  }

  // ==================== GESTION DES QUOTAS ====================

  /**
   * Consulter les quotas d'un utilisateur
   */
  viewQuota(): void {
    if (!this.quotaUserLogin || this.quotaUserLogin.trim() === '') {
      this.showQuotaMessage('❌ Veuillez entrer un login utilisateur', false);
      return;
    }

    this.isLoadingQuota = true;
    this.quotaMessage = '🔍 Consultation des quotas...';

    this.dataService.viewUserQuota(this.quotaUserLogin.trim()).subscribe({
      next: (response: QuotaInfo) => {
        this.isLoadingQuota = false;
        this.quotaSuccess = true;
        this.quotaInfo = response;

        let message = `✅ Quotas de ${response.userLogin}:\n\n`;
        response.abonnements.forEach((abo, index) => {
          message += `📦 Abonnement ${index + 1}: ${abo.planName}\n`;
          message += `  • SMS: ${abo.smsUsed}/${abo.smsLimit} (${abo.smsRemaining} restants)\n`;
          message += `  • WhatsApp: ${abo.whatsappUsed}/${abo.whatsappLimit} (${abo.whatsappRemaining} restants)\n`;
          message += `  • Statut: ${abo.status}\n\n`;
        });

        this.quotaMessage = message;
      },
      error: error => {
        this.isLoadingQuota = false;
        this.quotaSuccess = false;
        this.quotaInfo = null;
        this.quotaMessage = `❌ Erreur: ${error.error?.error || error.message}`;
      },
    });
  }

  /**
   * Augmenter les quotas d'un utilisateur
   */
  increaseQuota(): void {
    if (!this.quotaUserLogin || this.quotaUserLogin.trim() === '') {
      this.showQuotaMessage('❌ Veuillez entrer un login utilisateur', false);
      return;
    }

    if ((!this.smsIncrease || this.smsIncrease <= 0) && (!this.whatsappIncrease || this.whatsappIncrease <= 0)) {
      this.showQuotaMessage('❌ Veuillez entrer au moins une augmentation (SMS ou WhatsApp)', false);
      return;
    }

    this.isLoadingQuota = true;
    this.quotaMessage = '⬆️ Augmentation des quotas...';

    this.dataService
      .increaseUserQuota(this.quotaUserLogin.trim(), this.smsIncrease || undefined, this.whatsappIncrease || undefined)
      .subscribe({
        next: (response: UpdateQuotaResponse) => {
          this.isLoadingQuota = false;
          this.quotaSuccess = response.success;

          let message = `✅ ${response.message}\n\n`;
          response.abonnements.forEach(abo => {
            message += `📦 ${abo.planName} (${abo.type}):\n`;
            if (abo.smsIncrease) {
              message += `  • SMS: ${abo.oldSmsLimit} → ${abo.newSmsLimit} (+${abo.smsIncrease})\n`;
            }
            if (abo.whatsappIncrease) {
              message += `  • WhatsApp: ${abo.oldWhatsappLimit} → ${abo.newWhatsappLimit} (+${abo.whatsappIncrease})\n`;
            }
            message += '\n';
          });

          this.quotaMessage = message;

          // Réinitialiser les champs
          this.smsIncrease = null;
          this.whatsappIncrease = null;
        },
        error: error => {
          this.isLoadingQuota = false;
          this.quotaSuccess = false;
          this.quotaMessage = `❌ Erreur: ${error.error?.error || error.message}`;
        },
      });
  }

  /**
   * Mettre à jour (remplacer) les quotas d'un utilisateur
   */
  updateQuota(): void {
    if (!this.quotaUserLogin || this.quotaUserLogin.trim() === '') {
      this.showQuotaMessage('❌ Veuillez entrer un login utilisateur', false);
      return;
    }

    if (!this.newSmsLimit && !this.newWhatsappLimit) {
      this.showQuotaMessage('❌ Veuillez entrer au moins une nouvelle limite', false);
      return;
    }

    this.isLoadingQuota = true;
    this.quotaMessage = '🔄 Mise à jour des quotas...';

    this.dataService
      .updateUserQuota(this.quotaUserLogin.trim(), this.newSmsLimit || undefined, this.newWhatsappLimit || undefined)
      .subscribe({
        next: (response: UpdateQuotaResponse) => {
          this.isLoadingQuota = false;
          this.quotaSuccess = response.success;

          let message = `✅ ${response.message}\n\n`;
          response.abonnements.forEach(abo => {
            message += `📦 ${abo.planName} (${abo.type}):\n`;
            if (abo.newSmsLimit !== undefined) {
              message += `  • SMS: ${abo.oldSmsLimit} → ${abo.newSmsLimit}\n`;
            }
            if (abo.newWhatsappLimit !== undefined) {
              message += `  • WhatsApp: ${abo.oldWhatsappLimit} → ${abo.newWhatsappLimit}\n`;
            }
            message += '\n';
          });

          this.quotaMessage = message;

          // Réinitialiser les champs
          this.newSmsLimit = null;
          this.newWhatsappLimit = null;
        },
        error: error => {
          this.isLoadingQuota = false;
          this.quotaSuccess = false;
          this.quotaMessage = `❌ Erreur: ${error.error?.error || error.message}`;
        },
      });
  }

  private showQuotaMessage(msg: string, success: boolean) {
    this.quotaMessage = msg;
    this.quotaSuccess = success;
    this.isLoadingQuota = false;
    setTimeout(() => (this.quotaMessage = ''), 8000);
  }

  // ==================== MÉTHODES EXISTANTES ====================

  deleteAllContacts() {
    if (confirm('Es-tu sûr de vouloir supprimer TOUS les contacts ?')) {
      this.dataService.deleteAllContacts().subscribe(() => {
        alert('Tous les contacts ont été supprimés.');
      });
    }
  }

  deleteAllTempales() {
    if (confirm('Es-tu sûr de vouloir supprimer TOUS les templates ?')) {
      this.dataService.deleteAllTemplates().subscribe(() => {
        alert('Tous les templates ont été supprimés.');
      });
    }
  }

  deleteAllSms() {
    if (confirm('Es-tu sûr de vouloir supprimer TOUS les SMS ?')) {
      this.dataService.deleteAllSms().subscribe(() => {
        alert('Tous les SMS ont été supprimés.');
      });
    }
  }

  syncDeliveryStatus(): void {
    if (!this.sendSmsId) {
      this.showMessage('Veuillez entrer un ID.', false);
      return;
    }
    this.isLoading = true;
    this.dataService.syncDeliveryStatus(this.sendSmsId).subscribe({
      next: () => this.showMessage('Synchronisation DeliveryStatus réussie.', true),
      error: () => this.showMessage('Erreur lors de la synchronisation.', false),
    });
  }

  updateSendSmsStatus(): void {
    if (!this.sendSmsId) {
      this.showMessage('Veuillez entrer un ID.', false);
      return;
    }
    this.isLoading = true;
    this.dataService.updateSendSmsStatus(this.sendSmsId).subscribe({
      next: () => this.showMessage('Mise à jour des totaux réussie.', true),
      error: () => this.showMessage('Erreur lors de la mise à jour.', false),
    });
  }

  deleteSendSmsWithMessages(): void {
    if (!this.sendSmsId) {
      this.showMessage('Veuillez entrer un ID de SendSms.', false);
      return;
    }

    if (!confirm(`⚠️ Supprimer SendSms ${this.sendSmsId} et TOUS ses SMS ?\n\nCette opération est irréversible !`)) {
      return;
    }

    this.isLoading = true;
    this.dataService.deleteSendSmsWithMessages(this.sendSmsId).subscribe({
      next: (response: any) => {
        this.showMessage(`✅ SendSms ${response.deletedSendSmsId} supprimé avec ${response.deletedSmsCount} SMS`, true);
        this.sendSmsId = null;
      },
      error: err => {
        this.showMessage('❌ Erreur lors de la suppression.', false);
        console.error(err);
      },
    });
  }

  deleteGroupeWithContactsAndMessages(): void {
    if (!this.groupeId) {
      this.showMessage('Veuillez entrer un ID de groupe.', false);
      return;
    }

    if (
      !confirm(
        `⚠️ ATTENTION !\n\n` +
          `Vous allez supprimer :\n` +
          `- Le groupe ${this.groupeId}\n` +
          `- TOUS les contacts liés\n` +
          `- TOUS les SMS des contacts\n` +
          `- TOUS les SendSms du groupe\n\n` +
          `Cette action est IRRÉVERSIBLE !\n\nConfirmer ?`,
      )
    ) {
      return;
    }

    this.isLoading = true;
    this.dataService.deleteGroupeWithContactsAndMessages(this.groupeId).subscribe({
      next: (response: any) => {
        this.showMessage(
          `✅ Groupe ${response.deletedGroupId} supprimé avec:\n` +
            `- ${response.deletedContactsCount} contacts\n` +
            `- ${response.deletedSmsCount} SMS\n` +
            `- ${response.deletedSendSmsCount} SendSms`,
          true,
        );
        this.groupeId = null;
      },
      error: err => {
        this.showMessage('❌ Erreur lors de la suppression complète.', false);
        console.error(err);
      },
    });
  }

  recalculateUserAbonnement(): void {
    if (!this.userLogin || this.userLogin.trim() === '') {
      this.showMessage('Veuillez entrer un login utilisateur.', false);
      return;
    }

    this.isLoading = true;
    this.dataService.recalculateAbonnement(this.userLogin.trim()).subscribe({
      next: (response: any) => {
        this.showMessage(
          `✅ Abonnement recalculé pour ${response.userLogin}:\n` +
            `- SMS utilisés: ${response.smsUsed || response.totalSmsUsed}\n` +
            `- WhatsApp utilisés: ${response.whatsappUsed || response.totalWhatsappUsed}`,
          true,
        );
      },
      error: err => {
        this.showMessage('❌ Erreur lors du recalcul.', false);
        console.error(err);
      },
    });
  }

  private showMessage(msg: string, success: boolean) {
    this.message = msg;
    this.success = success;
    this.isLoading = false;
    setTimeout(() => (this.message = ''), 6000);
  }

  migrateUserLogin(): void {
    if (!this.migrationUserLogin || this.migrationUserLogin.trim() === '') {
      this.migrationMessage = '❌ Veuillez entrer un login utilisateur';
      this.migrationSuccess = false;
      return;
    }

    this.isLoadingMigration = true;
    this.migrationMessage = '🔄 Migration en cours...';
    this.migrationSuccess = false;

    this.dataService.migrateUserLogin(this.migrationUserLogin).subscribe({
      next: (response: MigrationResult) => {
        this.isLoadingMigration = false;
        this.migrationSuccess = response.success;
        this.migrationMessage = this.formatMigrationMessage(response);
      },
      error: error => {
        this.isLoadingMigration = false;
        this.migrationSuccess = false;
        this.migrationMessage = this.formatMigrationError(error);
      },
    });
  }

  migrateAllUsers(): void {
    const confirmMessage =
      '⚠️ ATTENTION: Migration globale\n\n' +
      'Vous allez migrer TOUS les utilisateurs du système.\n\n' +
      '• Cette opération peut prendre plusieurs minutes\n' +
      '• Elle va traiter tous les SendSms et SMS du système\n' +
      "• L'opération ne peut pas être annulée une fois lancée\n\n" +
      'Voulez-vous vraiment continuer ?';

    if (!confirm(confirmMessage)) {
      return;
    }

    this.isLoadingMigration = true;
    this.migrationMessage =
      "🔄 Migration globale en cours...\n\n⏳ Cela peut prendre plusieurs minutes selon le nombre d'utilisateurs.\nVeuillez patienter...";
    this.migrationSuccess = false;

    this.dataService.migrateAllUsers().subscribe({
      next: (response: GlobalMigrationResult) => {
        this.isLoadingMigration = false;
        this.migrationSuccess = response.success;
        this.migrationMessage = this.formatGlobalMigrationMessage(response);
      },
      error: error => {
        this.isLoadingMigration = false;
        this.migrationSuccess = false;
        this.migrationMessage = this.formatMigrationError(error);
      },
    });
  }

  private formatRecalculateMessage(response: any): string {
    return (
      `✅ ${response.message}\n\n` +
      `📊 Résumé:\n` +
      `• Utilisateur: ${response.userLogin}\n` +
      `• SMS utilisés: ${response.totalSmsUsed}\n` +
      `• WhatsApp utilisés: ${response.totalWhatsappUsed}\n` +
      `• Abonnements mis à jour: ${response.abonnementsCount}`
    );
  }

  private formatMigrationMessage(response: MigrationResult): string {
    if (!response.success) {
      return `❌ ${response.message}`;
    }

    return (
      `✅ ${response.message}\n\n` +
      `📊 Détails de la migration:\n` +
      `• Utilisateur: ${response.userLogin}\n` +
      `• SendSms traités: ${response.totalSendSms}\n` +
      `• SMS migrés: ${response.migrated}\n\n` +
      `✨ Tous les SMS de cet utilisateur ont maintenant le champ user_login rempli!`
    );
  }

  private formatGlobalMigrationMessage(response: GlobalMigrationResult): string {
    if (!response.success) {
      return `❌ ${response.message}`;
    }

    return (
      `✅ ${response.message}\n\n` +
      `📊 Statistiques globales:\n` +
      `• Utilisateurs traités: ${response.totalUsersProcessed}\n` +
      `• SendSms traités: ${response.totalSendSmsProcessed}\n` +
      `• SMS migrés: ${response.totalSmsUpdated}\n\n` +
      `🎉 Migration globale terminée avec succès!\n\n` +
      `💡 Vous pouvez maintenant recalculer les abonnements de chaque utilisateur.`
    );
  }

  private formatMigrationError(error: any): string {
    const errorMessage = error.error?.message || error.message || 'Erreur inconnue';
    return `❌ Erreur lors de la migration:\n\n${errorMessage}\n\n` + `💡 Vérifiez les logs du serveur pour plus de détails.`;
  }
}
