import { Component, inject, signal, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import SharedModule from 'app/shared/shared.module';
import { AccountService } from 'app/core/auth/account.service';
import { Account } from 'app/core/auth/account.model';
import { MegaMenuItem } from 'primeng/api';
import { MenuModule } from 'primeng/menu';
import { MenubarModule } from 'primeng/menubar';
import { MegaMenuModule } from 'primeng/megamenu';
import { TieredMenuModule } from 'primeng/tieredmenu';
import { PanelMenuModule } from 'primeng/panelmenu';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SendSmsService } from '../entities/send-sms/service/send-sms.service';
import { ChartModule } from 'primeng/chart';
import { CheckIcon } from 'primeng/icons/check';
import { PlanabonnementService } from '../entities/planabonnement/service/planabonnement.service';
import { pricingPlans, services, testimonials, stats, features } from './data/pricing-plans';

// Interface pour les plans d'abonnement
interface SubscriptionPlan {
  id: string;
  name: string;
  description: string;
  price: number | string;
  currency: string;
  period: string;
  features: string[];
  popular: boolean;
  buttonText: string;
  colorClass: string;
}

@Component({
  standalone: true,
  selector: 'jhi-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  imports: [
    SharedModule,
    ChartModule,
    SelectButtonModule,
    RouterModule,
    MenubarModule,
    MegaMenuModule,
    MenuModule,
    PanelMenuModule,
    TieredMenuModule,
    CheckIcon,
  ],
})
export default class HomeComponent implements OnInit, OnDestroy {
  account = signal<Account | null>(null);
  isRefreshed = false;
  private readonly destroy$ = new Subject<void>();
  totalSms = 0;
  data: any;
  totalSendMSCount: number | null = null;
  options: any;
  plansWhatsapp: any[] = [];
  plansSms: any[] = [];
  planFree: any | null = null;
  items: MegaMenuItem[] = [];
  private accountService = inject(AccountService);
  private router = inject(Router);
  // Propriétés pour les animations et interactions
  isScrolled = false;
  currentTestimonial = 0;
  isMenuOpen = false;
  services = services;
  testimonials = testimonials;
  stats = stats;
  features = features;
  pricingPlans = pricingPlans;
  plansPremium: any[] = [];

  @HostListener('window:scroll', [])
  onWindowScroll() {
    this.isScrolled = window.scrollY > 50;
  }

  nextTestimonial() {
    this.currentTestimonial = (this.currentTestimonial + 1) % this.testimonials.length;
  }

  previousTestimonial() {
    this.currentTestimonial = this.currentTestimonial === 0 ? this.testimonials.length - 1 : this.currentTestimonial - 1;
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  scrollToSection(sectionId: string) {
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
    this.isMenuOpen = false;
  }

  requestDemo() {
    console.log('Demande de démo');
  }

  constructor(
    private sendSmsService: SendSmsService,
    private planAbonnementService: PlanabonnementService,
  ) {}

  ngOnInit(): void {
    this.getplanabonement();

    this.accountService
      .getAuthenticationState()
      .pipe(takeUntil(this.destroy$))
      .subscribe(account => {
        this.account.set(account);
        this.isRefreshed = true;

        // Charger les statistiques si l'utilisateur est connecté
        if (account) {
          this.loadSmsStats();
          this.getTotalSendMSCount();
        }
      });
    setInterval(() => {
      this.nextTestimonial();
    }, 5000);
  }
  getplanabonement() {
    this.planAbonnementService.getAllActivePlans().subscribe({
      next: response => {
        let plans = response.body || [];
        console.log('plans', plans);
        const sortedPlans = this.sortPlansByOrder(plans);
        plans = sortedPlans;
        if (plans.length === 0) {
          this.setFallbackPlans();
        } else {
          // Fonction pour parser les fonctionnalités
          const parseFeatures = (features: string | null | undefined): string[] =>
            features
              ? features
                  .split(',')
                  .map(f => f.trim())
                  .filter(f => f.length > 0)
              : [];

          // Filtrer par le nouveau champ planType
          this.plansWhatsapp = plans
            .filter(p => p.planType === 'WHATSAPP')
            .map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));

          this.plansSms = plans.filter(p => p.planType === 'SMS').map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));
          this.plansPremium = plans
            .filter(p => p.planType === 'PREMIUM')
            .map(plan => ({ ...plan, abpFeatures: parseFeatures(plan.abpFeatures) }));

          const freePlan = plans.find(p => p.planType === 'FREE');
          this.planFree = freePlan
            ? {
                ...freePlan,
                abpFeatures: parseFeatures(freePlan.abpFeatures),
              }
            : null;
        }
      },
      error: err => {
        console.error('Erreur lors du chargement des plans:', err);
        // Fallback si erreur
        this.setFallbackPlans();
      },
    });
  }
  sortPlansByOrder(plans: any[]): any[] {
    return plans.sort((a, b) => {
      // Gérer les cas où sortOrder pourrait être null/undefined
      const orderA = a.sortOrder ?? Number.MAX_SAFE_INTEGER;
      const orderB = b.sortOrder ?? Number.MAX_SAFE_INTEGER;

      return orderA - orderB;
    });
  }

  setFallbackPlans() {
    this.plansWhatsapp = this.pricingPlans.whatsapp;
    this.plansSms = this.pricingPlans.sms;
    this.planFree = this.pricingPlans.free;
    this.plansPremium = this.pricingPlans.premium;
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

  getSmsButtonClass(plan: any): string {
    if (plan.active === false) {
      return 'bg-gray-400 text-gray-200 cursor-not-allowed';
    }

    if (plan.abpPrice === null || plan.abpPrice === undefined) {
      return 'bg-gray-900 hover:bg-gray-800 text-white';
    }

    return 'bg-green-600 hover:bg-green-700 text-white';
  }

  selectFreePlan() {
    if (this.planFree?.active === false) {
      console.log('Plan gratuit non disponible');
      return;
    }

    console.log('Plan gratuit sélectionné');
    this.router.navigate(['/account/register'], {
      queryParams: { plan: 'free' },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSmsStats(): void {}

  getTotalSendMSCount(): void {
    this.sendSmsService.getTotalSendMSCount().subscribe({
      next: count => {
        this.totalSendMSCount = count;
        console.log('Nombre total de messages:', count);
      },
      error: err => {
        console.error('Erreur lors de la récupération du nombre de messages:', err);
      },
    });
  }

  login(): void {
    this.router.navigate(['/login']);
  }

  register(): void {
    this.router.navigate(['/account/register']);
  }

  startFreeTrial(): void {
    if (this.account()) {
      // Si l'utilisateur est connecté, rediriger vers le dashboard
      this.router.navigate(['/dashboard']);
    } else {
      // Sinon, rediriger vers l'inscription avec un paramètre d'essai gratuit
      this.router.navigate(['/account/register'], { queryParams: { trial: true } });
    }
  }
}
