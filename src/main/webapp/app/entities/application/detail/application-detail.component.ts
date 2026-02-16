import { Component, input, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval } from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IApplication, IWebhookTest, ITokensApp } from '../application.model';
import { ApplicationService } from '../service/application.service';

@Component({
  standalone: true,
  selector: 'jhi-application-detail',
  templateUrl: './application-detail.component.html',
  imports: [SharedModule, RouterModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe, CommonModule, FormsModule],
})
export class ApplicationDetailComponent implements OnInit, OnDestroy {
  application = input<IApplication | null>(null);
  get app(): IApplication | null {
    return this.application();
  }
  // États des opérations
  showFullToken = false;
  isTestingWebhook = false;
  isUpdatingStatus = false;
  isResettingCounters = false;
  isLoadingHistory = false;
  isExporting = false;
  isCloning = false;

  // Données d'historique et métriques
  apiCallHistory: any[] = [];
  selectedTimeRange = '24h';
  webhookTestResult: { success: boolean; message: string } | null = null;

  // Messages
  successMessage: string | null = null;
  errorMessage: string | null = null;

  // Subscription pour les updates automatiques
  private refreshSubscription: Subscription | null = null;

  // Services injectés
  private applicationService = inject(ApplicationService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadApiCallHistory();
    this.refreshSubscription = interval(30000).subscribe(() => {
      this.refreshData();
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  /**
   * Récupère le token actif de l'application
   */
  getActiveToken(): ITokensApp | null {
    const app = this.application();
    if (!app?.tokens || app.tokens.length === 0) return null;

    return app.tokens.find(t => t.active && !t.isExpired) || null;
  }

  /**
   * Récupère le token actif ou le premier token disponible
   */
  getDisplayToken(): string | null {
    const activeToken = this.getActiveToken();
    if (activeToken?.token) return activeToken.token;

    const app = this.application();
    if (app?.tokens && app.tokens.length > 0) {
      return app.tokens[0].token || null;
    }

    return null;
  }

  /**
   * Retourne à la page précédente
   */
  previousState(): void {
    this.router.navigate(['/application']);
  }

  /**
   * Copie le token dans le presse-papiers
   */
  async copyToken(token: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(token);
      this.showSuccessMessage('Token copié dans le presse-papiers');
    } catch (err) {
      this.showErrorMessage('Erreur lors de la copie du token');
      console.error('Erreur de copie:', err);
    }
  }

  /**
   * Régénère le token API actif ou crée un nouveau token
   */
  regenerateToken(): void {
    const app = this.application();
    if (!app || !app.id) return;

    const activeToken = this.getActiveToken();

    if (confirm("Êtes-vous sûr de vouloir régénérer le token ? L'ancien token ne fonctionnera plus.")) {
      if (activeToken?.id) {
        // Régénérer le token existant
        this.applicationService.regenerateToken(activeToken.id).subscribe({
          next: response => {
            this.showSuccessMessage('Token régénéré avec succès');
            this.refreshData();
          },
          error: error => {
            this.showErrorMessage('Erreur lors de la régénération du token');
            console.error('Erreur de régénération:', error);
          },
        });
      } else {
        // Créer un nouveau token si aucun token actif n'existe
        const expirationDate = new Date();
        expirationDate.setFullYear(expirationDate.getFullYear() + 1); // Expire dans 1 an

        this.applicationService
          .createToken({
            applicationId: app.id,
            dateExpiration: expirationDate.toISOString(),
            active: true,
          })
          .subscribe({
            next: response => {
              this.showSuccessMessage('Token créé avec succès');
              this.refreshData();
            },
            error: error => {
              this.showErrorMessage('Erreur lors de la création du token');
              console.error('Erreur de création:', error);
            },
          });
      }
    }
  }

  /**
   * Teste le webhook configuré
   */
  testWebhook(): void {
    const app = this.application();
    if (!app || !app.id || !app.webhookUrl) return;

    this.isTestingWebhook = true;
    this.webhookTestResult = null;

    const testData: IWebhookTest = {
      url: app.webhookUrl,
      secret: app.webhookSecret || undefined,
      testPayload: {
        event: 'test',
        application_id: app.id,
        timestamp: new Date().toISOString(),
        data: { message: 'Test webhook from application dashboard' },
      },
    };

    this.applicationService.testWebhook(app.id, testData).subscribe({
      next: response => {
        this.isTestingWebhook = false;
        this.webhookTestResult = {
          success: true,
          message: `Webhook testé avec succès (Status: ${response.status || 200})`,
        };
      },
      error: error => {
        this.isTestingWebhook = false;
        this.webhookTestResult = {
          success: false,
          message: `Erreur: ${error.message || 'Impossible de joindre le webhook'}`,
        };
      },
    });
  }

  /**
   * Change le statut de l'application (actif/inactif)
   */
  toggleStatus(): void {
    const app = this.application();
    if (!app || !app.id) return;

    const newStatus = !app.isActive;
    const action = newStatus ? 'activer' : 'désactiver';

    if (confirm(`Êtes-vous sûr de vouloir ${action} cette application ?`)) {
      this.isUpdatingStatus = true;

      this.applicationService.updateStatus(app.id, newStatus).subscribe({
        next: () => {
          this.isUpdatingStatus = false;
          this.showSuccessMessage(`Application ${newStatus ? 'activée' : 'désactivée'} avec succès`);
          this.refreshData();
        },
        error: error => {
          this.isUpdatingStatus = false;
          this.showErrorMessage('Erreur lors de la mise à jour du statut');
          console.error('Erreur de mise à jour:', error);
        },
      });
    }
  }

  /**
   * Remet à zéro les compteurs d'utilisation
   */
  resetCounters(): void {
    const app = this.application();
    if (!app || !app.id) return;

    if (confirm("Êtes-vous sûr de vouloir remettre à zéro les compteurs d'utilisation ?")) {
      this.isResettingCounters = true;

      this.applicationService.resetUsageCounters(app.id).subscribe({
        next: () => {
          this.isResettingCounters = false;
          this.showSuccessMessage('Compteurs remis à zéro avec succès');
          this.refreshData();
        },
        error: error => {
          this.isResettingCounters = false;
          this.showErrorMessage('Erreur lors de la remise à zéro des compteurs');
          console.error('Erreur de reset:', error);
        },
      });
    }
  }

  /**
   * Charge l'historique des appels API
   */
  loadApiCallHistory(): void {
    const app = this.application();
    if (!app || !app.id) return;

    this.isLoadingHistory = true;

    const params = {
      timeRange: this.selectedTimeRange,
      limit: 100,
    };

    this.applicationService.getApiCallHistory(app.id, params).subscribe({
      next: response => {
        this.isLoadingHistory = false;
        this.apiCallHistory = response.body || [];
      },
      error: error => {
        this.isLoadingHistory = false;
        this.showErrorMessage("Erreur lors du chargement de l'historique");
        console.error('Erreur de chargement historique:', error);
      },
    });
  }

  /**
   * Exporte les données de l'application
   */
  exportData(format: 'json' | 'csv' = 'json'): void {
    const app = this.application();
    if (!app || !app.id) return;

    this.isExporting = true;

    this.applicationService.exportApplicationData(app.id, format).subscribe({
      next: blob => {
        this.isExporting = false;

        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `application_${app.id}_data.${format}`;
        link.click();
        window.URL.revokeObjectURL(url);

        this.showSuccessMessage('Export terminé avec succès');
      },
      error: error => {
        this.isExporting = false;
        this.showErrorMessage("Erreur lors de l'export");
        console.error("Erreur d'export:", error);
      },
    });
  }

  /**
   * Clone l'application
   */
  cloneApplication(): void {
    const app = this.application();
    if (!app || !app.id) return;

    const newName = prompt('Nom de la nouvelle application:', `${app.name} (copie)`);
    if (!newName) return;

    this.isCloning = true;

    this.applicationService.cloneApplication(app.id, newName).subscribe({
      next: response => {
        this.isCloning = false;
        this.showSuccessMessage('Application clonée avec succès');

        if (response.body?.id) {
          this.router.navigate(['/application', response.body.id, 'view']);
        }
      },
      error: error => {
        this.isCloning = false;
        this.showErrorMessage('Erreur lors du clonage');
        console.error('Erreur de clonage:', error);
      },
    });
  }

  /**
   * Ouvre la page des logs
   */
  viewLogs(): void {
    const app = this.application();
    if (!app || !app.id) return;
    this.router.navigate(['/application', app.id, 'logs']);
  }

  /**
   * Ouvre la documentation API
   */
  openApiDocumentation(): void {
    window.open('/api/documentation', '_blank');
  }

  /**
   * Obtient le nom d'affichage d'un service
   */
  getServiceDisplayName(service: string): string {
    const serviceNames: { [key: string]: string } = {
      sms: 'SMS',
      whatsapp: 'WhatsApp',
      email: 'Email',
      voice: 'Vocal',
    };
    console.log('Servicesssss', serviceNames[service] || service);
    return serviceNames[service] || service;
  }

  /**
   * Calcule le pourcentage d'utilisation
   */
  getUsagePercentage(current: number | null | undefined, limit: number | null | undefined): number {
    if (!current || !limit || limit === 0) return 0;
    return Math.min((current / limit) * 100, 100);
  }

  /**
   * Obtient la couleur de la barre de progression
   */
  getUsageBarColor(percentage: number): string {
    if (percentage >= 90) return 'bg-red-500';
    if (percentage >= 75) return 'bg-yellow-500';
    if (percentage >= 50) return 'bg-blue-500';
    return 'bg-green-500';
  }

  /**
   * Formate la date du dernier appel API
   */
  formatLastApiCall(lastCall: string | null | undefined): string {
    if (!lastCall) return 'Jamais';

    const date = new Date(lastCall);
    const now = new Date();
    const diffInMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60));

    if (diffInMinutes < 1) return "À l'instant";
    if (diffInMinutes < 60) return `Il y a ${diffInMinutes} min`;
    if (diffInMinutes < 1440) return `Il y a ${Math.floor(diffInMinutes / 60)} h`;
    return `Il y a ${Math.floor(diffInMinutes / 1440)} j`;
  }

  /**
   * Formate la dernière utilisation du token
   */
  formatLastTokenUse(): string {
    const activeToken = this.getActiveToken();
    if (!activeToken?.lastUsedAt) return 'Jamais utilisé';

    return this.formatLastApiCall(activeToken.lastUsedAt);
  }

  /**
   * Calcule le nombre total d'appels dans l'historique
   */
  getTotalCalls(): number {
    return this.apiCallHistory.length;
  }

  /**
   * Calcule le taux de succès
   */
  getSuccessRate(): number {
    if (this.apiCallHistory.length === 0) return 0;

    const successCalls = this.apiCallHistory.filter(call => call.status < 400).length;
    return Math.round((successCalls / this.apiCallHistory.length) * 100);
  }

  /**
   * Calcule le temps de réponse moyen
   */
  getAverageResponseTime(): number {
    if (this.apiCallHistory.length === 0) return 0;

    const totalTime = this.apiCallHistory.reduce((sum, call) => sum + (call.responseTime || 0), 0);
    return Math.round(totalTime / this.apiCallHistory.length);
  }

  /**
   * Compte le nombre d'erreurs
   */
  getErrorCount(): number {
    return this.apiCallHistory.filter(call => call.status >= 400).length;
  }

  /**
   * Rafraîchit les données de base
   */
  private refreshData(): void {
    this.loadApiCallHistory();
  }

  /**
   * Affiche un message de succès
   */
  private showSuccessMessage(message: string): void {
    this.successMessage = message;
    this.errorMessage = null;
    setTimeout(() => {
      this.successMessage = null;
    }, 5000);
  }

  /**
   * Affiche un message d'erreur
   */
  private showErrorMessage(message: string): void {
    this.errorMessage = message;
    this.successMessage = null;
    setTimeout(() => {
      this.errorMessage = null;
    }, 8000);
  }

  /**
   * Efface tous les messages
   */
  private clearMessages(): void {
    this.errorMessage = null;
    this.successMessage = null;
  }
}
