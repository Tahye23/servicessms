import { Component, input, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe } from 'app/shared/date';
import { IPlanabonnement } from '../planabonnement.model';
import { PlanabonnementService } from '../service/planabonnement.service';

@Component({
  standalone: true,
  selector: 'jhi-planabonnement-detail',
  templateUrl: './planabonnement-detail.component.html',
  styleUrls: ['./planabonnement-detail.component.scss'],
  imports: [SharedModule, RouterModule, CommonModule, DurationPipe, FormatMediumDatetimePipe, FormatMediumDatePipe],
})
export class PlanabonnementDetailComponent {
  planabonnement = input<IPlanabonnement | null>(null);

  protected router = inject(Router);
  protected planabonnementService = inject(PlanabonnementService);

  previousState(): void {
    window.history.back();
  }

  /**
   * Parse les fonctionnalités du plan
   */
  parseFeatures(features: string | null | undefined): string[] {
    if (!features) return [];
    return features
      .split(',')
      .map(feature => feature.trim())
      .filter(feature => feature.length > 0);
  }

  /**
   * Formate la période d'affichage
   */
  formatPeriod(period: string | null | undefined): string {
    if (!period) return '';

    const periodMap: { [key: string]: string } = {
      MONTHLY: 'mois',
      YEARLY: 'an',
      LIFETIME: 'à vie',
    };

    return periodMap[period] || period.toLowerCase();
  }

  /**
   * Formate les limites (gère les valeurs illimitées)
   */
  formatLimit(limit: number | null | undefined): string {
    if (limit === null || limit === undefined) return '0';
    if (limit === -1) return '∞';
    if (limit >= 1000000) return `${(limit / 1000000).toFixed(1)}M`;
    if (limit >= 1000) return `${(limit / 1000).toFixed(1)}K`;
    return limit.toString();
  }

  /**
   * Formate les limites de stockage en unités appropriées
   */
  formatStorageLimit(limitMb: number | null | undefined): string {
    if (limitMb === null || limitMb === undefined) return '0 MB';
    if (limitMb === -1) return '∞';
    if (limitMb >= 1024) {
      const gb = (limitMb / 1024).toFixed(1);
      return `${gb} GB`;
    }
    return `${limitMb} MB`;
  }

  /**
   * Vérifie si le plan a des permissions définies
   */
  hasPermissions(plan: IPlanabonnement): boolean {
    return !!(plan.canManageUsers || plan.canManageTemplates || plan.canViewConversations || plan.canViewAnalytics || plan.prioritySupport);
  }

  /**
   * Duplique le plan actuel
   */
  duplicatePlan(plan: IPlanabonnement): void {
    if (!plan.id) return;

    // Créer une copie du plan sans l'ID
    const duplicatedPlan = {
      ...plan,
      id: null,
      abpName: `${plan.abpName} (Copie)`,
      active: false, // Nouvelle copie inactive par défaut
      abpPopular: false, // Nouvelle copie non populaire par défaut
      createdDate: null,
      updatedDate: null,
    };

    this.planabonnementService.create(duplicatedPlan).subscribe({
      next: response => {
        if (response.body?.id) {
          // Rediriger vers la page d'édition du nouveau plan
          this.router.navigate(['/planabonnement', response.body.id, 'edit']);
        }
      },
      error: error => {
        console.error('Erreur lors de la duplication:', error);
        // Optionnel: afficher un message d'erreur à l'utilisateur
      },
    });
  }

  /**
   * Exporte les détails du plan vers un fichier JSON
   */
  exportPlanDetails(plan: IPlanabonnement): void {
    const exportData = {
      plan: plan,
      exportDate: new Date().toISOString(),
      exportedBy: 'Plan Abonnement System',
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: 'application/json',
    });

    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `plan-${plan.abpName || plan.id}-details.json`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  /**
   * Calcule un score de fonctionnalités pour le plan
   */
  calculateFeatureScore(plan: IPlanabonnement): number {
    let score = 0;

    // Points pour les limites
    if (plan.smsLimit === -1) score += 10;
    else if (plan.smsLimit && plan.smsLimit > 1000) score += 5;
    else if (plan.smsLimit && plan.smsLimit > 100) score += 2;

    if (plan.whatsappLimit === -1) score += 10;
    else if (plan.whatsappLimit && plan.whatsappLimit > 1000) score += 5;
    else if (plan.whatsappLimit && plan.whatsappLimit > 100) score += 2;

    if (plan.usersLimit === -1) score += 8;
    else if (plan.usersLimit && plan.usersLimit > 10) score += 4;
    else if (plan.usersLimit && plan.usersLimit > 5) score += 2;

    // Points pour les permissions
    if (plan.canManageUsers) score += 3;
    if (plan.canManageTemplates) score += 3;
    if (plan.canViewConversations) score += 2;
    if (plan.canViewAnalytics) score += 4;
    if (plan.prioritySupport) score += 5;

    // Points pour les limites techniques
    if (plan.maxApiCallsPerDay === -1) score += 6;
    else if (plan.maxApiCallsPerDay && plan.maxApiCallsPerDay > 5000) score += 3;

    if (plan.storageLimitMb && plan.storageLimitMb > 2000) score += 3;

    return score;
  }

  /**
   * Retourne la catégorie du plan basée sur son score
   */
  getPlanCategory(plan: IPlanabonnement): string {
    const score = this.calculateFeatureScore(plan);

    if (score >= 40) return 'Enterprise';
    if (score >= 25) return 'Premium';
    if (score >= 15) return 'Professional';
    if (score >= 5) return 'Basic';
    return 'Starter';
  }

  /**
   * Retourne la couleur associée à la catégorie
   */
  getCategoryColor(plan: IPlanabonnement): string {
    const category = this.getPlanCategory(plan);

    const colorMap: { [key: string]: string } = {
      Enterprise: 'text-gray-800',
      Premium: 'text-purple-600',
      Professional: 'text-blue-600',
      Basic: 'text-green-600',
      Starter: 'text-yellow-600',
    };

    return colorMap[category] || 'text-gray-600';
  }

  /**
   * Vérifie si une fonctionnalité est disponible dans le plan
   */
  hasFeature(plan: IPlanabonnement, feature: string): boolean {
    const features = this.parseFeatures(plan.abpFeatures);
    return features.some(f => f.toLowerCase().includes(feature.toLowerCase()));
  }

  /**
   * Retourne les recommandations pour améliorer le plan
   */
  getPlanRecommendations(plan: IPlanabonnement): string[] {
    const recommendations: string[] = [];

    if (!plan.canViewAnalytics) {
      recommendations.push("Ajouter l'accès aux analytics pour un meilleur suivi");
    }

    if (!plan.prioritySupport && plan.planType !== 'FREE') {
      recommendations.push('Inclure le support prioritaire pour fidéliser les clients');
    }

    if (plan.smsLimit !== null && plan.smsLimit !== undefined && plan.smsLimit < 100 && plan.planType !== 'FREE') {
      recommendations.push("Augmenter la limite SMS pour plus d'attractivité");
    }

    if (!plan.canManageTemplates && plan.planType === 'PREMIUM') {
      recommendations.push('Activer la gestion des templates pour les plans Premium');
    }

    if (
      plan.maxApiCallsPerDay !== null &&
      plan.maxApiCallsPerDay !== undefined &&
      plan.maxApiCallsPerDay < 1000 &&
      plan.planType === 'ENTERPRISE'
    ) {
      recommendations.push('Augmenter les appels API pour les besoins Enterprise');
    }

    return recommendations;
  }

  /**
   * Génère un rapport d'analyse du plan
   */
  generatePlanReport(plan: IPlanabonnement): void {
    const report = {
      planDetails: {
        id: plan.id,
        name: plan.abpName,
        type: plan.planType,
        price: plan.abpPrice,
        currency: plan.abpCurrency,
        period: plan.abpPeriod,
        active: plan.active,
        popular: plan.abpPopular,
      },
      limits: {
        sms: plan.smsLimit,
        whatsapp: plan.whatsappLimit,
        users: plan.usersLimit,
        templates: plan.templatesLimit,
        apiCalls: plan.maxApiCallsPerDay,
        storage: plan.storageLimitMb,
      },
      permissions: {
        manageUsers: plan.canManageUsers,
        manageTemplates: plan.canManageTemplates,
        viewConversations: plan.canViewConversations,
        viewAnalytics: plan.canViewAnalytics,
        prioritySupport: plan.prioritySupport,
      },
      analysis: {
        featureScore: this.calculateFeatureScore(plan),
        category: this.getPlanCategory(plan),
        recommendations: this.getPlanRecommendations(plan),
      },
      features: this.parseFeatures(plan.abpFeatures),
      generatedAt: new Date().toISOString(),
    };

    const blob = new Blob([JSON.stringify(report, null, 2)], {
      type: 'application/json',
    });

    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `rapport-plan-${plan.abpName || plan.id}.json`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  /**
   * Compare ce plan avec un autre type de plan
   */
  getComparisonInsights(plan: IPlanabonnement): string[] {
    const insights: string[] = [];

    if (plan.planType === 'FREE') {
      insights.push('Plan idéal pour tester les fonctionnalités de base');
      insights.push('Parfait pour les petites entreprises débutantes');
    } else if (plan.planType === 'SMS') {
      insights.push('Spécialisé pour les campagnes SMS marketing');
      insights.push('Adapté aux entreprises privilégiant les SMS');
    } else if (plan.planType === 'WHATSAPP') {
      insights.push('Optimisé pour la communication WhatsApp Business');
      insights.push('Idéal pour le service client moderne');
    } else if (plan.planType === 'PREMIUM') {
      insights.push('Équilibre parfait entre fonctionnalités et prix');
      insights.push('Recommandé pour les entreprises en croissance');
    } else if (plan.planType === 'ENTERPRISE') {
      insights.push('Solution complète pour les grandes organisations');
      insights.push('Toutes les fonctionnalités avancées incluses');
    }

    return insights;
  }
}
