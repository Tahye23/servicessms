// partner-request.component.ts CORRIGÉ
import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { FormGroup, FormControl, Validators, ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { Subject, takeUntil, timer } from 'rxjs';

import SharedModule from 'app/shared/shared.module';
import { PlanabonnementService } from '../../entities/planabonnement/service/planabonnement.service';
import { IPlanabonnement } from '../../entities/planabonnement/planabonnement.model';
import { PartnershipRequestService } from '../../entities/partnershipRequest/service/partnershipRequestService.service';
import { NewPartnershipRequest, PartnershipRequestFormData } from '../../entities/partnershipRequest/partnership-request.model';
import { finalize } from 'rxjs/operators';
import { AccountService } from '../../core/auth/account.service';

// Interface pour la demande de partenariat
export interface PartnerRequest {
  // Informations du plan
  planType: string;
  planName: string;
  planId?: number;

  // Informations personnelles
  firstName: string;
  lastName: string;
  email: string;
  phone: string;

  // Informations entreprise
  companyName: string;
  industry: string;
  projectDescription: string;

  // Besoins spécifiques
  monthlyVolume?: string;
  launchDate?: string;

  // Métadonnées
  requestDate: Date;
  status: string;
}

@Component({
  standalone: true,
  selector: 'jhi-partner-request',
  imports: [SharedModule, ReactiveFormsModule, RouterModule],
  templateUrl: './request.component.html',
  styleUrls: ['./request.component.scss'],
})
export default class RequestComponent implements OnInit, OnDestroy {
  // Destroy subject pour la gestion des subscriptions
  private destroy$ = new Subject<void>();
  private partnershipRequestService = inject(PartnershipRequestService);

  // États du composant avec signals
  loading = false;
  success = signal(false);
  error = signal(false);
  private fb = inject(FormBuilder);

  // Données du plan sélectionné
  selectedPlanType = '';
  selectedPlanId!: number;
  selectedPlanName = '';
  selectedPlan: IPlanabonnement | null = null;
  account = inject(AccountService).trackCurrentAccount();
  // Messages
  successMessage = '';
  errorMessage = '';
  requestForm!: FormGroup;

  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private planAbonnementService = inject(PlanabonnementService);

  ngOnInit(): void {
    // Récupération des paramètres de la route
    this.extractRouteParams();
    this.intialForms();
    // Configuration du formulaire
    this.setupFormValidation();
  }

  intialForms() {
    this.requestForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      phone: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(20)]],
      companyName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
      industry: ['', [Validators.required]],
      projectDescription: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000)]],
      monthlyVolume: [''],
      launchDate: [''],
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Extraction des paramètres de la route
   */
  private extractRouteParams(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const planParam = params['plan'];
      console.log('Plan sélectionné:', { params });
      if (planParam) {
        this.selectedPlanId = planParam;
        this.loadPlanById(planParam);
      }
    });
  }

  /**
   * Configuration de la validation du formulaire
   */
  private setupFormValidation(): void {
    // Réinitialise les erreurs quand l'utilisateur modifie le formulaire
    this.requestForm.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      if (this.error()) {
        this.clearError();
      }
    });
  }

  /**
   * Chargement du plan par ID
   */
  private loadPlanById(planId: number): void {
    this.planAbonnementService.find(planId).subscribe(plan => {
      console.log('plan', plan);
      this.selectedPlan = plan.body;
      this.selectedPlanName = plan.body?.abpName || '';
    });
  }

  /**
   * NOUVELLES MÉTHODES POUR LE MODÈLE CORRIGÉ
   */

  /**
   * Formate la période d'affichage
   */
  formatPeriod(period: string | null | undefined): string {
    if (!period) return '';

    const periodMap: { [key: string]: string } = {
      MONTHLY: '/mois',
      YEARLY: '/an',
      LIFETIME: 'à vie',
    };

    return periodMap[period] || `/${period.toLowerCase()}`;
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
   * Vérifie si le plan a des limites à afficher
   */
  showPlanLimits(): boolean {
    if (!this.selectedPlan) return false;

    return !!(
      (this.selectedPlan.smsLimit !== null && this.selectedPlan.smsLimit !== undefined) ||
      (this.selectedPlan.whatsappLimit !== null && this.selectedPlan.whatsappLimit !== undefined) ||
      (this.selectedPlan.usersLimit !== null && this.selectedPlan.usersLimit !== undefined) ||
      (this.selectedPlan.templatesLimit !== null && this.selectedPlan.templatesLimit !== undefined)
    );
  }

  /**
   * Vérifie si le plan a des permissions à afficher
   */
  hasPlanPermissions(): boolean {
    if (!this.selectedPlan) return false;

    return !!(
      this.selectedPlan.canManageUsers ||
      this.selectedPlan.canManageTemplates ||
      this.selectedPlan.canViewConversations ||
      this.selectedPlan.canViewAnalytics ||
      this.selectedPlan.prioritySupport
    );
  }

  /**
   * Parse les fonctionnalités du plan - CORRIGÉ
   */
  get abpFeaturesArray(): string[] {
    if (!this.selectedPlan || !this.selectedPlan.abpFeatures) return [];
    return this.selectedPlan.abpFeatures
      .split(',')
      .map(f => f.trim())
      .filter(f => f.length > 0);
  }

  /**
   * Retourne le type de plan formaté
   */
  getPlanTypeDisplay(): string {
    if (!this.selectedPlan?.planType) return '';

    const typeMap: { [key: string]: string } = {
      FREE: 'Gratuit',
      SMS: 'SMS Marketing',
      WHATSAPP: 'WhatsApp Business',
      PREMIUM: 'Premium',
      ENTERPRISE: 'Enterprise',
    };

    return typeMap[this.selectedPlan.planType] || this.selectedPlan.planType;
  }

  /**
   * Retourne la description du plan avec ses limites
   */
  getPlanSummary(): string {
    if (!this.selectedPlan) return '';

    const parts: string[] = [];

    if (this.selectedPlan.smsLimit !== null && this.selectedPlan.smsLimit !== undefined) {
      parts.push(`${this.formatLimit(this.selectedPlan.smsLimit)} SMS`);
    }

    if (this.selectedPlan.whatsappLimit !== null && this.selectedPlan.whatsappLimit !== undefined) {
      parts.push(`${this.formatLimit(this.selectedPlan.whatsappLimit)} WhatsApp`);
    }

    if (this.selectedPlan.usersLimit !== null && this.selectedPlan.usersLimit !== undefined) {
      parts.push(`${this.formatLimit(this.selectedPlan.usersLimit)} utilisateurs`);
    }

    return parts.join(' • ');
  }

  /**
   * Vérifie si le plan est actif
   */
  isPlanActive(): boolean {
    return this.selectedPlan?.active !== false;
  }

  /**
   * Retourne la couleur du badge selon le type de plan
   */
  getPlanBadgeColor(): string {
    if (!this.selectedPlan?.planType) return 'bg-gray-500';

    const colorMap: { [key: string]: string } = {
      FREE: 'bg-green-500',
      SMS: 'bg-blue-500',
      WHATSAPP: 'bg-green-600',
      PREMIUM: 'bg-purple-500',
      ENTERPRISE: 'bg-gray-800',
    };

    return colorMap[this.selectedPlan.planType] || 'bg-gray-500';
  }

  /**
   * MÉTHODES EXISTANTES INCHANGÉES
   */

  private resetStates(): void {
    this.success.set(false);
    this.error.set(false);
    this.errorMessage = '';
  }

  /**
   * Soumission de la demande partenaire
   */
  submitRequest(): void {
    if (this.requestForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.loading = true;
    this.resetStates();

    // Préparation des données
    const formData: PartnershipRequestFormData = this.requestForm.value;
    const requestData: NewPartnershipRequest = {
      id: null,
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      phone: formData.phone,
      companyName: formData.companyName,
      industry: formData.industry,
      projectDescription: formData.projectDescription,
      monthlyVolume: formData.monthlyVolume || null,
      launchDate: formData.launchDate || null,
      selectedPlanId: this.selectedPlan?.id || null,
      selectedPlanName: this.selectedPlanName || null,
      status: null, // Sera défini côté backend
      createdDate: null, // Sera défini côté backend
      processedDate: null,
      adminNotes: null,
    };

    // Envoi de la requête
    this.partnershipRequestService
      .create(requestData)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: response => {
          console.log('Demande de partenariat créée avec succès:', response.body);
          this.success.set(true);
          this.error.set(false);

          // Scroll vers le haut pour voir le message de succès
          window.scrollTo({ top: 0, behavior: 'smooth' });

          // Optionnel: Redirection automatique après quelques secondes
          // setTimeout(() => {
          //   this.router.navigate(['/']);
          // }, 5000);
        },
        error: error => {
          console.error('Erreur lors de la création de la demande:', error);
          this.error.set(true);
          this.success.set(false);

          // Gestion des messages d'erreur
          if (error.status === 400 && error.error?.message) {
            this.errorMessage = error.error.message;
          } else if (error.status === 409) {
            this.errorMessage = 'Une demande similaire existe déjà pour cette adresse email et ce plan.';
          } else if (error.status === 0) {
            this.errorMessage = 'Erreur de connexion. Vérifiez votre connexion internet.';
          } else {
            this.errorMessage = 'Une erreur inattendue est survenue. Veuillez réessayer plus tard.';
          }

          // Scroll vers le message d'erreur
          window.scrollTo({ top: 0, behavior: 'smooth' });
        },
      });
  }

  /**
   * Gestion du succès de l'envoi
   */
  private handleSubmissionSuccess(response: any): void {
    this.loading = false;
    this.success.set(true);
    this.error.set(false);

    this.successMessage = response?.message || 'Votre demande de partenariat a été envoyée avec succès !';

    // Scroll vers le haut pour voir le message
    this.scrollToTop();

    // Feedback haptique de succès
    this.triggerSuccessHapticFeedback();

    // Auto-redirection après 10 secondes
    timer(10000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.router.navigate(['/']);
      });
  }

  /**
   * Gestion des erreurs d'envoi
   */
  private handleSubmissionError(error: any): void {
    this.loading = false;
    this.error.set(true);

    // Gestion personnalisée des messages d'erreur
    if (error.status === 409) {
      this.errorMessage = 'Une demande existe déjà pour cette adresse email.';
    } else if (error.status === 400) {
      this.errorMessage = 'Données invalides. Veuillez vérifier vos informations.';
    } else if (error.status === 0) {
      this.errorMessage = 'Impossible de se connecter au serveur. Vérifiez votre connexion internet.';
    } else if (error.status >= 500) {
      this.errorMessage = 'Erreur interne du serveur. Veuillez réessayer plus tard.';
    } else {
      this.errorMessage = error.error?.message || "Une erreur est survenue lors de l'envoi de votre demande.";
    }

    // Scroll vers le haut pour voir l'erreur
    this.scrollToTop();

    // Feedback haptique d'erreur
    this.triggerErrorHapticFeedback();
  }

  /**
   * Réessaie l'envoi en cas d'erreur
   */
  retrySubmission(): void {
    this.clearMessages();
    this.submitRequest();
  }

  /**
   * Retour à la page précédente
   */
  goBack(): void {
    this.router.navigate(['/']);
  }

  /**
   * Réinitialise le formulaire
   */
  resetForm(): void {
    this.requestForm.reset();
    this.clearMessages();

    // Focus sur le premier champ
    timer(100)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        const firstInput = document.getElementById('firstName');
        if (firstInput) {
          firstInput.focus();
        }
      });
  }

  /**
   * Marque tous les champs du formulaire comme touchés pour afficher les erreurs
   */
  private markFormGroupTouched(): void {
    Object.keys(this.requestForm.controls).forEach(key => {
      const control = this.requestForm.get(key);
      if (control) {
        control.markAsTouched();
      }
    });
  }

  /**
   * Efface tous les messages
   */
  private clearMessages(): void {
    this.success.set(false);
    this.error.set(false);
    this.successMessage = '';
    this.errorMessage = '';
  }

  /**
   * Efface uniquement les messages d'erreur
   */
  private clearError(): void {
    this.error.set(false);
    this.errorMessage = '';
  }

  /**
   * Scroll vers le haut de la page
   */
  private scrollToTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * Feedback haptique de succès
   */
  private triggerSuccessHapticFeedback(): void {
    if ('vibrate' in navigator) {
      navigator.vibrate([100, 50, 100, 50, 100]);
    }
  }

  /**
   * Feedback haptique d'erreur
   */
  private triggerErrorHapticFeedback(): void {
    if ('vibrate' in navigator) {
      navigator.vibrate([200, 100, 200]);
    }
  }

  /**
   * Getters pour la validation des champs
   */
  get isFirstNameInvalid(): boolean {
    const control = this.requestForm.get('firstName');
    return !!(control?.invalid && control?.touched);
  }

  get isLastNameInvalid(): boolean {
    const control = this.requestForm.get('lastName');
    return !!(control?.invalid && control?.touched);
  }

  get isEmailInvalid(): boolean {
    const control = this.requestForm.get('email');
    return !!(control?.invalid && control?.touched);
  }

  get isPhoneInvalid(): boolean {
    const control = this.requestForm.get('phone');
    return !!(control?.invalid && control?.touched);
  }

  get isCompanyNameInvalid(): boolean {
    const control = this.requestForm.get('companyName');
    return !!(control?.invalid && control?.touched);
  }

  get isIndustryInvalid(): boolean {
    const control = this.requestForm.get('industry');
    return !!(control?.invalid && control?.touched);
  }

  get isProjectDescriptionInvalid(): boolean {
    const control = this.requestForm.get('projectDescription');
    return !!(control?.invalid && control?.touched);
  }

  /**
   * Messages d'erreur personnalisés
   */
  getFirstNameError(): string {
    const control = this.requestForm.get('firstName');
    if (control?.hasError('required')) return 'Le prénom est obligatoire';
    if (control?.hasError('minlength')) return 'Au moins 2 caractères requis';
    if (control?.hasError('maxlength')) return 'Maximum 100 caractères';
    return '';
  }

  getLastNameError(): string {
    const control = this.requestForm.get('lastName');
    if (control?.hasError('required')) return 'Le nom est obligatoire';
    if (control?.hasError('minlength')) return 'Au moins 2 caractères requis';
    if (control?.hasError('maxlength')) return 'Maximum 100 caractères';
    return '';
  }

  getEmailError(): string {
    const control = this.requestForm.get('email');
    if (control?.hasError('required')) return "L'email est obligatoire";
    if (control?.hasError('email')) return "Format d'email invalide";
    if (control?.hasError('maxlength')) return 'Maximum 254 caractères';
    return '';
  }

  getPhoneError(): string {
    const control = this.requestForm.get('phone');
    if (control?.hasError('required')) return 'Le téléphone est obligatoire';
    if (control?.hasError('minlength')) return 'Au moins 10 caractères requis';
    if (control?.hasError('maxlength')) return 'Maximum 20 caractères';
    return '';
  }

  getCompanyNameError(): string {
    const control = this.requestForm.get('companyName');
    if (control?.hasError('required')) return "Le nom de l'entreprise est obligatoire";
    if (control?.hasError('minlength')) return 'Au moins 2 caractères requis';
    if (control?.hasError('maxlength')) return 'Maximum 150 caractères';
    return '';
  }

  getProjectDescriptionError(): string {
    const control = this.requestForm.get('projectDescription');
    if (control?.hasError('required')) return 'La description du projet est obligatoire';
    if (control?.hasError('minlength')) return 'Au moins 20 caractères requis';
    if (control?.hasError('maxlength')) return 'Maximum 2000 caractères';
    return '';
  }

  /**
   * Validation en temps réel du formulaire
   */
  get isFormValid(): boolean {
    return this.requestForm.valid;
  }

  /**
   * Pourcentage de completion du formulaire
   */
  get formCompletionPercentage(): number {
    const totalFields = Object.keys(this.requestForm.controls).length;
    const filledFields = Object.values(this.requestForm.controls).filter(
      control => control.value && control.value.toString().trim() !== '',
    ).length;

    return Math.round((filledFields / totalFields) * 100);
  }

  /**
   * Calcule un score de valeur pour le plan sélectionné
   */
  getPlanValueScore(): number {
    if (!this.selectedPlan) return 0;

    let score = 0;

    // Points pour les limites élevées
    if (this.selectedPlan.smsLimit === -1 || this.selectedPlan.whatsappLimit === -1) score += 10;
    else if (
      (this.selectedPlan.smsLimit && this.selectedPlan.smsLimit > 100000) ||
      (this.selectedPlan.whatsappLimit && this.selectedPlan.whatsappLimit > 100000)
    )
      score += 5;

    // Points pour les permissions
    if (this.selectedPlan.canManageUsers) score += 2;
    if (this.selectedPlan.canManageTemplates) score += 2;
    if (this.selectedPlan.canViewAnalytics) score += 3;
    if (this.selectedPlan.prioritySupport) score += 4;

    return score;
  }

  /**
   * Retourne des recommandations basées sur le plan sélectionné
   */
  getPlanRecommendations(): string[] {
    if (!this.selectedPlan) return [];

    const recommendations: string[] = [];

    if (this.selectedPlan.planType === 'FREE') {
      recommendations.push('Parfait pour découvrir nos services sans engagement');
      recommendations.push('Idéal pour tester les fonctionnalités de base');
    } else if (this.selectedPlan.planType === 'PREMIUM') {
      recommendations.push('Excellent choix pour les entreprises en croissance');
      recommendations.push('Toutes les fonctionnalités avancées incluses');
    } else if (this.selectedPlan.planType === 'ENTERPRISE') {
      recommendations.push('Solution complète pour les grandes organisations');
      recommendations.push('Support dédié et intégrations personnalisées');
    }

    return recommendations;
  }
}
