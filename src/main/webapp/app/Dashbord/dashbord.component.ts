import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

// Material imports
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';

// Chart imports
import { NgApexchartsModule } from 'ng-apexcharts';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexDataLabels,
  ApexPlotOptions,
  ApexTitleSubtitle,
  ApexLegend,
  ApexNonAxisChartSeries,
} from 'ng-apexcharts';

// Services
import { DashboardService, StatsResponseDTO, SubscriptionInfoDTO } from './service/dashboard.service';
import { AccountService } from '../core/auth/account.service';
import { SubscriptionService } from '../Subscription/service/subscriptionService.service';

@Component({
  selector: 'jhi-dashboard',
  templateUrl: './dashbord.component.html',
  standalone: true,
  imports: [CommonModule, MatFormFieldModule, MatInputModule, MatDatepickerModule, MatNativeDateModule, FormsModule, NgApexchartsModule],
})
export default class DashboardComponent implements OnInit {
  // Services
  private subscriptionService = inject(SubscriptionService);
  private dashboardService = inject(DashboardService);
  private router = inject(Router);
  private accountService = inject(AccountService);
  dailyMessageLimit: number = 1000;

  // Données
  stats = signal<StatsResponseDTO | null>(null);
  subscriptionInfo = signal<SubscriptionInfoDTO | null>(null);
  loading = signal(false);
  errorMessage = signal('');
  pieChartData: any = {};
  barChartData: any = {};

  // User info
  auth_account = this.accountService.trackCurrentAccount();

  // Options de filtres
  stateOptions = [
    { label: 'Total', value: '1', icon: 'pi pi-calendar', description: 'Toutes les données' },
    { label: '7 derniers jours', value: '2', icon: 'pi pi-calendar', description: 'Semaine en cours' },
    { label: 'Mois en cours', value: '3', icon: 'pi pi-calendar-plus', description: 'Mois actuel' },
    { label: 'Mois précédent', value: '4', icon: 'pi pi-calendar-minus', description: 'Mois dernier' },
    { label: 'Personnalisé', value: '5', icon: 'pi pi-sliders-h', description: 'Période spécifique' },
  ];

  value: string = '1';
  customStartDate: Date | null = null;
  customEndDate: Date | null = null;
  now: Date = new Date();

  ngOnInit(): void {
    this.loadSubscriptionInfo();
    this.getStatsByDateRange();
  }

  /**
   * Charge les informations d'abonnement
   */
  loadSubscriptionInfo(): void {
    this.dashboardService.getSubscriptionInfo().subscribe({
      next: info => {
        this.subscriptionInfo.set(info);
        console.log("✅ Informations d'abonnement chargées:", info);
      },
      error: error => {
        console.error('❌ Erreur chargement abonnement:', error);
        // Utiliser des valeurs par défaut
        this.subscriptionInfo.set({
          subscriptionType: 'FREE',
          planName: 'Plan Gratuit',
          canSendSMS: true,
          canSendWhatsApp: false,
          smsLimit: 10,
          whatsappLimit: 0,
          smsRemaining: 10,
          whatsappRemaining: 0,
          canManageTemplates: false,
          canViewAnalytics: false,
          canManageUsers: false,
          isExpiringSoon: false,
        });
      },
    });
  }

  /**
   * Charge les statistiques selon la période
   */
  getStatsByDateRange(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    const range = this.getDateRange();
    const start = range?.start;
    const end = range?.end;

    this.dashboardService.getStatsByDateRange(start, end).subscribe({
      next: (data: StatsResponseDTO) => {
        console.log('📊 Statistiques chargées:', data);
        this.loading.set(false);
        this.stats.set(data);
        this.updateChartData();
        this.errorMessage.set('');
      },
      error: err => {
        this.loading.set(false);
        this.stats.set(null);
        this.errorMessage.set('Erreur lors du chargement des statistiques.');
        console.error('❌ Erreur stats:', err);
      },
    });
  }

  /**
   * Met à jour les données des graphiques
   */
  updateChartData(): void {
    const statsData = this.stats();
    if (!statsData) return;

    // Données pour le graphique en secteurs
    const series = statsData.typeStats?.map(t => t.total) || [0, 0];
    const labels = statsData.typeStats?.map(t => t.type) || ['SMS', 'WHATSAPP'];

    this.pieChartData = {
      series: series,
      chart: {
        type: 'donut',
        width: 280,
        height: 280,
      },
      labels: labels,
      colors: ['#3b82f6', '#10b981'],
      legend: {
        position: 'bottom',
        fontSize: '14px',
      },
      plotOptions: {
        pie: {
          donut: {
            size: '65%',
            labels: {
              show: true,
              total: {
                show: true,
                label: 'Total',
                fontSize: '16px',
                fontWeight: 600,
              },
            },
          },
        },
      },
    };

    // Données pour le graphique en barres
    const categories = statsData.typeStats?.map(t => t.type) || ['SMS', 'WHATSAPP'];

    this.barChartData = {
      series: [
        { name: 'Succès', data: statsData.typeStats?.map(t => t.success) || [0, 0] },
        { name: 'Échecs', data: statsData.typeStats?.map(t => t.failed) || [0, 0] },
        { name: 'En attente', data: statsData.typeStats?.map(t => t.pending) || [0, 0] },
      ],
      chart: {
        type: 'bar',
        height: 350,
        stacked: true,
        toolbar: {
          show: true,
        },
      },
      colors: ['#10b981', '#ef4444', '#f59e0b'],
      xaxis: {
        categories: categories,
        labels: {
          style: {
            fontSize: '12px',
            fontWeight: 500,
          },
        },
      },
      plotOptions: {
        bar: {
          horizontal: false,
          borderRadius: 8,
        },
      },
      title: {
        text: 'Répartition par statut et canal',
        align: 'center',
        style: {
          fontSize: '16px',
          fontWeight: 600,
        },
      },
      dataLabels: {
        enabled: true,
      },
      legend: {
        position: 'top',
        horizontalAlign: 'center',
      },
    };
  }

  /**
   * Calcule la plage de dates selon la sélection
   */
  getDateRange(): { start: string; end: string } | null {
    const now = new Date();
    let end = new Date(now);
    let start: Date;

    switch (this.value) {
      case '2':
        start = new Date();
        start.setDate(start.getDate() - 6);
        break;
      case '3':
        start = new Date(now.getFullYear(), now.getMonth(), 1);
        break;
      case '4':
        start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
        end = new Date(now.getFullYear(), now.getMonth(), 0);
        break;
      case '5':
        if (!this.customStartDate || !this.customEndDate) return null;
        start = this.customStartDate;
        end = this.customEndDate;
        break;
      default:
        return null;
    }

    start.setHours(0, 0, 0, 0);
    end.setHours(23, 59, 59, 999);
    const format = (d: Date) => d.toISOString().slice(0, 16).replace('T', ' ');
    return { start: format(start), end: format(end) };
  }

  /**
   * Gestion du changement de dates personnalisées
   */
  onCustomDateChange(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.getStatsByDateRange();
  }

  /**
   * Volume total de messages
   */
  get volumeTotal(): number {
    const statsData = this.stats();
    return statsData?.typeStats?.reduce((acc: number, t) => acc + (t.total ?? 0), 0) ?? 0;
  }

  // ===== MÉTHODES POUR LE SYSTÈME D'ABONNEMENT =====

  /**
   * Vérifie si l'utilisateur peut envoyer des SMS
   */
  get canUseSMS(): boolean {
    const info = this.subscriptionInfo();
    return !!info?.canSendSMS && (info?.smsRemaining ?? 0) > 0;
  }

  get canUseWhatsApp(): boolean {
    const info = this.subscriptionInfo();
    return !!info?.canSendWhatsApp && (info?.whatsappRemaining ?? 0) > 0;
  }

  /**
   * Vérifie si les crédits sont faibles
   */
  get hasLowCredits(): boolean {
    const info = this.subscriptionInfo();
    if (!info) return false;

    return (info.canSendSMS && info.smsRemaining <= 10) || (info.canSendWhatsApp && info.whatsappRemaining <= 10);
  }

  /**
   * Message d'alerte pour les crédits faibles
   */
  get lowCreditsMessage(): string {
    const info = this.subscriptionInfo();
    if (!info) return '';

    if (info.canSendSMS && info.smsRemaining <= 10) {
      return `Attention: Plus que ${info.smsRemaining} SMS disponibles`;
    }
    if (info.canSendWhatsApp && info.whatsappRemaining <= 10) {
      return `Attention: Plus que ${info.whatsappRemaining} messages WhatsApp disponibles`;
    }
    return '';
  }

  /**
   * Classe CSS pour l'état des crédits SMS
   */
  getSmsStatusClass(): string {
    const info = this.subscriptionInfo();
    if (!info?.canSendSMS) return 'text-gray-500';
    if (info.smsRemaining <= 5) return 'text-red-600';
    if (info.smsRemaining <= 20) return 'text-orange-500';
    return 'text-green-600';
  }

  /**
   * Classe CSS pour l'état des crédits WhatsApp
   */
  getWhatsAppStatusClass(): string {
    const info = this.subscriptionInfo();
    if (!info?.canSendWhatsApp) return 'text-gray-500';
    if (info.whatsappRemaining <= 5) return 'text-red-600';
    if (info.whatsappRemaining <= 20) return 'text-orange-500';
    return 'text-green-600';
  }

  /**
   * Calcule le pourcentage d'utilisation
   */
  getUsagePercentage(type: 'sms' | 'whatsapp'): number {
    const info = this.subscriptionInfo();
    if (!info) return 0;

    if (type === 'sms') {
      if (info.smsLimit === 0) return 0;
      return Math.max(0, Math.min(100, (info.smsRemaining / info.smsLimit) * 100));
    } else {
      if (info.whatsappLimit === 0) return 0;
      return Math.max(0, Math.min(100, (info.whatsappRemaining / info.whatsappLimit) * 100));
    }
  }

  /**
   * Navigation vers les fonctionnalités
   */
  navigateToSMS(): void {
    if (this.canUseSMS) {
      this.router.navigate(['/send-sms']);
    } else {
      this.navigateToUpgrade('send-sms');
    }
  }

  navigateToWhatsApp(): void {
    if (this.canUseWhatsApp) {
      this.router.navigate(['/send-whatsapp']);
    } else {
      this.navigateToUpgrade('send-whatsapp');
    }
  }

  navigateToContacts(): void {
    this.router.navigate(['/contact']);
  }

  navigateToTemplates(): void {
    const info = this.subscriptionInfo();
    if (info?.canManageTemplates) {
      this.router.navigate(['/template']);
    } else {
      this.navigateToUpgrade('templates');
    }
  }

  navigateToUpgrade(feature?: string): void {
    const queryParams = feature ? { feature, returnUrl: '/dashboard' } : { returnUrl: '/dashboard' };
    this.router.navigate(['/upgrade'], { queryParams });
  }

  /**
   * Suggestions personnalisées selon l'abonnement
   */
  getPersonalizedSuggestions(): string[] {
    const info = this.subscriptionInfo();
    const suggestions: string[] = [];

    if (!info) return suggestions;

    if (info.subscriptionType === 'FREE') {
      suggestions.push('Passez à un plan payant pour envoyer plus de messages');
      suggestions.push('Découvrez les modèles pour gagner du temps');
    }

    if (this.hasLowCredits) {
      suggestions.push("Rechargez vos crédits avant qu'ils ne soient épuisés");
    }

    if (!info.canManageTemplates) {
      suggestions.push('Créez des modèles réutilisables avec un plan premium');
    }

    if (this.volumeTotal > 100 && info.subscriptionType === 'FREE') {
      suggestions.push('Votre usage élevé mérite un plan adapté à vos besoins');
    }

    return suggestions;
  }

  /**
   * Actions rapides disponibles
   */
  getQuickActions(): Array<{
    label: string;
    iconType: 'sms' | 'whatsapp' | 'users' | 'file' | 'bolt';
    action: () => void;
    available: boolean;
    tooltip?: string;
  }> {
    const info = this.subscriptionInfo();

    return [
      {
        label: 'Envoyer SMS',
        iconType: 'sms',
        action: () => this.navigateToSMS(),
        available: this.canUseSMS,
        tooltip: !this.canUseSMS ? 'Abonnement SMS requis' : undefined,
      },
      {
        label: 'Envoyer WhatsApp',
        iconType: 'whatsapp',
        action: () => this.navigateToWhatsApp(),
        available: this.canUseWhatsApp,
        tooltip: !this.canUseWhatsApp ? 'Abonnement WhatsApp requis' : undefined,
      },
      {
        label: 'Gérer contacts',
        iconType: 'users',
        action: () => this.navigateToContacts(),
        available: true,
      },
      {
        label: 'Créer modèle',
        iconType: 'file',
        action: () => this.navigateToTemplates(),
        available: info?.canManageTemplates ?? false,
        tooltip: !info?.canManageTemplates ? 'Plan premium requis' : undefined,
      },
    ];
  }

  /**
   * Rafraîchit les données du dashboard
   */
  refreshDashboard(): void {
    this.loadSubscriptionInfo();
    this.getStatsByDateRange();
  }
}
