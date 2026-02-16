import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PlanabonnementService } from '../entities/planabonnement/service/planabonnement.service';
import { pricingPlans } from '../home/data/pricing-plans'; // adapte le chemin si besoin
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-upgrade-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upgrade/upgrade-page.component.html',
  styleUrls: ['./upgrade/upgrade-page.component.css'],
})
export class UpgradePageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private planAbonnementService = inject(PlanabonnementService);

  reason: string | null = null;
  returnUrl: string | null = null;
  recommendedPlan: string = '';

  plansWhatsapp: any[] = [];
  plansSms: any[] = [];
  plansPremium: any[] = [];

  loading = true;
  errorLoading = false;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.reason = params['reason'];
      this.returnUrl = params['returnUrl'];
      const feature = params['feature'];

      if (feature === 'send-sms') {
        this.recommendedPlan = 'SMS';
      } else if (feature === 'send-whatsapp') {
        this.recommendedPlan = 'WHATSAPP';
      } else {
        this.recommendedPlan = 'PREMIUM';
      }

      this.loadPlans();
    });
  }

  loadPlans() {
    this.loading = true;
    this.planAbonnementService.getAllActivePlans().subscribe({
      next: response => {
        const plans = response.body || [];
        this.errorLoading = false;

        const parseFeatures = (features: string | null | undefined): string[] =>
          features
            ? features
                .split(',')
                .map(f => f.trim())
                .filter(f => f.length > 0)
            : [];

        this.plansWhatsapp = plans
          .filter(p => p.planType === 'WHATSAPP')
          .map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));
        this.plansSms = plans.filter(p => p.planType === 'SMS').map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));

        this.plansPremium = plans
          .filter(p => p.planType === 'PREMIUM')
          .map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));

        if (plans.length === 0) {
          this.setFallbackPlans();
        }
        this.loading = false;
      },
      error: err => {
        console.error('Erreur chargement plans:', err);
        this.errorLoading = true;
        this.setFallbackPlans();
        this.loading = false;
      },
    });
  }

  setFallbackPlans() {
    const parseFeatures = (features: string | null | undefined): string[] =>
      features
        ? features
            .split(',')
            .map(f => f.trim())
            .filter(f => f.length > 0)
        : [];

    this.plansWhatsapp = pricingPlans.whatsapp.map(plan => ({
      ...plan,
      abpFeatures: parseFeatures(plan.abpFeatures),
    }));

    this.plansSms = pricingPlans.sms.map(plan => ({
      ...plan,
      abpFeatures: parseFeatures(plan.abpFeatures),
    }));

    this.plansPremium = pricingPlans.premium.map(plan => ({
      ...plan,
      abpFeatures: parseFeatures(plan.abpFeatures),
    }));
  }

  selectPlan(planId: number | string) {
    this.router.navigate(['/account/request', planId], {
      queryParams: { returnUrl: this.returnUrl },
    });
  }

  formatPeriod(period: string | null | undefined): string {
    if (!period) return '';
    const periodMap: { [key: string]: string } = {
      MONTHLY: '/mois',
      YEARLY: '/an',
      LIFETIME: 'à vie',
    };
    return periodMap[period] || `/${period.toLowerCase()}`;
  }

  formatLimit(limit: number | null | undefined): string {
    if (limit === null || limit === undefined) return '0';
    if (limit === -1) return '∞';
    if (limit >= 1000000) return `${(limit / 1000000).toFixed(1)}M`;
    if (limit >= 1000) return `${(limit / 1000).toFixed(1)}K`;
    return limit.toString();
  }

  showPlanLimits(plan: any): boolean {
    return !!(
      (plan.smsLimit !== null && plan.smsLimit !== undefined) ||
      (plan.whatsappLimit !== null && plan.whatsappLimit !== undefined) ||
      (plan.usersLimit !== null && plan.usersLimit !== undefined)
    );
  }

  getPlanButtonClass(plan: any): string {
    if (plan.active === false) {
      return 'bg-gray-400 text-gray-200 cursor-not-allowed';
    }
    if (plan.abpPrice === null || plan.abpPrice === undefined) {
      return 'bg-gray-900 hover:bg-gray-800 text-white';
    }
    if (plan.abpPopular) {
      return 'bg-white text-blue-600 hover:bg-gray-50';
    }
    return 'bg-blue-600 hover:bg-blue-700 text-white';
  }
}
