import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { PlanabonnementService } from '../service/planabonnement.service';
import { IPlanabonnement, PlanType, PlanPeriod } from '../planabonnement.model';
import { PlanabonnementFormService, PlanabonnementFormGroup } from './planabonnement-form.service';

@Component({
  standalone: true,
  selector: 'jhi-planabonnement-update',
  templateUrl: './planabonnement-update.component.html',
  styleUrls: ['./planabonnement-update.component.scss'],
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class PlanabonnementUpdateComponent implements OnInit {
  isSaving = false;
  planabonnement: IPlanabonnement | null = null;

  // Énumérations disponibles dans le template
  planTypes = Object.values(PlanType);
  planPeriods = Object.values(PlanPeriod);

  protected planabonnementService = inject(PlanabonnementService);
  protected planabonnementFormService = inject(PlanabonnementFormService);
  protected activatedRoute = inject(ActivatedRoute);

  editForm: PlanabonnementFormGroup = this.planabonnementFormService.createPlanabonnementFormGroup();

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ planabonnement }) => {
      this.planabonnement = planabonnement;
      if (planabonnement) {
        this.updateForm(planabonnement);
      }

      // Écouter les changements de type de plan pour ajuster les valeurs par défaut
      this.editForm.get('planType')?.valueChanges.subscribe(planType => {
        if (planType && typeof planType === 'string') {
          this.planabonnementFormService.configureByPlanType(this.editForm, planType);
        }
      });
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    const businessErrors = this.planabonnementFormService.validateBusinessRules(this.editForm);
    if (businessErrors.length > 0) {
      console.error('Erreurs de validation métier:', businessErrors);
      return;
    }

    this.isSaving = true;
    const planabonnement = this.planabonnementFormService.getPlanabonnement(this.editForm);
    if (planabonnement.abpFeatures) {
      planabonnement.abpFeatures = planabonnement.abpFeatures
        .split(',')
        .map(feature => feature.trim())
        .filter(feature => feature.length > 0)
        .join(',');
    }
    this.processFormValuesForSave(planabonnement);

    if (planabonnement.id !== null) {
      this.subscribeToSaveResponse(this.planabonnementService.update(planabonnement));
    } else {
      this.subscribeToSaveResponse(this.planabonnementService.create(planabonnement));
    }
  }

  /**
   * Traite les valeurs du formulaire selon le type de plan avant sauvegarde
   */
  private processFormValuesForSave(planabonnement: IPlanabonnement | any): void {
    // Pour les plans gratuits, s'assurer que le prix est 0
    if (planabonnement.planType === PlanType.FREE) {
      planabonnement.abpPrice = 0;
    }

    // Pour les valeurs illimitées, utiliser -1
    if (planabonnement.smsLimit === null || planabonnement.smsLimit === undefined) {
      planabonnement.smsLimit = 0;
    }
    if (planabonnement.whatsappLimit === null || planabonnement.whatsappLimit === undefined) {
      planabonnement.whatsappLimit = 0;
    }

    // S'assurer que les limites d'utilisateurs sont au minimum 1 (sauf si illimité)
    if (planabonnement.usersLimit !== -1 && planabonnement.usersLimit < 1) {
      planabonnement.usersLimit = 1;
    }
  }

  /**
   * Gère les changements de type de plan
   */
  onPlanTypeChange(event: any): void {
    const planType = event.target.value;
    if (planType && typeof planType === 'string') {
      this.planabonnementFormService.configureByPlanType(this.editForm, planType);
    }
  }

  /**
   * Détermine si la section tarification doit être affichée
   */
  shouldShowPricingSection(): boolean {
    const planType = this.editForm.get('planType')?.value;
    const customPlan = this.editForm.get('customPlan')?.value;
    return planType !== null && planType !== undefined && planType !== 'FREE' && !customPlan;
  }

  /**
   * Détermine si c'est un plan spécialisé (permet options avancées)
   */
  isSpecializedPlan(): boolean {
    const planType = this.editForm.get('planType')?.value;
    return planType !== null && planType !== undefined && ['SMS', 'WHATSAPP', 'PREMIUM', 'ENTERPRISE'].includes(planType);
  }

  /**
   * Détermine si la limite SMS doit être affichée
   */
  shouldShowSmsLimit(): boolean {
    const planType = this.editForm.get('planType')?.value;
    return planType !== null && planType !== undefined && ['FREE', 'SMS', 'PREMIUM', 'ENTERPRISE'].includes(planType);
  }

  /**
   * Détermine si la limite WhatsApp doit être affichée
   */
  shouldShowWhatsappLimit(): boolean {
    const planType = this.editForm.get('planType')?.value;
    return planType !== null && planType !== undefined && ['FREE', 'WHATSAPP', 'PREMIUM', 'ENTERPRISE'].includes(planType);
  }

  /**
   * Détermine si les limites illimitées sont possibles
   */
  canHaveUnlimited(): boolean {
    const planType = this.editForm.get('planType')?.value;
    return planType !== null && planType !== undefined && ['PREMIUM', 'ENTERPRISE'].includes(planType);
  }

  /**
   * Toggle pour activer/désactiver les limites illimitées
   */
  toggleUnlimitedLimits(event: any): void {
    const isUnlimited = event.target.checked;

    if (isUnlimited) {
      this.editForm.patchValue({
        smsLimit: -1,
        whatsappLimit: -1,
        usersLimit: -1,
        templatesLimit: -1,
        maxApiCallsPerDay: -1,
      });
    } else {
      // Remettre des valeurs par défaut selon le type de plan
      const planType = this.editForm.get('planType')?.value;
      if (planType && typeof planType === 'string') {
        this.planabonnementFormService.configureByPlanType(this.editForm, planType);
      }
    }
  }

  /**
   * Obtient le label du type de plan
   */
  getPlanTypeLabel(): string {
    const planType = this.editForm.get('planType')?.value;
    const labels: { [key: string]: string } = {
      FREE: 'Gratuit',
      SMS: 'SMS',
      WHATSAPP: 'WhatsApp',
      PREMIUM: 'Premium',
      ENTERPRISE: 'Enterprise',
    };
    return planType && typeof planType === 'string' ? labels[planType] || '' : '';
  }

  /**
   * Obtient le label de la période
   */
  getPeriodLabel(): string {
    const period = this.editForm.get('abpPeriod')?.value;
    const labels: { [key: string]: string } = {
      MONTHLY: 'mois',
      YEARLY: 'an',
      LIFETIME: 'Aucune limite',
    };
    return period && typeof period === 'string' ? labels[period] || '' : '';
  }

  /**
   * Génère un aperçu des fonctionnalités formatées
   */
  getFormattedFeatures(): string[] {
    const features = this.editForm.get('abpFeatures')?.value;
    if (!features) return [];

    return features
      .split(',')
      .map((feature: string) => feature.trim())
      .filter((feature: string) => feature.length > 0);
  }

  /**
   * Obtient les erreurs de validation
   */
  getValidationErrors(): string[] {
    const errors: string[] = [];

    // Erreurs du formulaire Angular
    if (this.editForm.get('abpName')?.invalid && this.editForm.get('abpName')?.touched) {
      errors.push('Le nom du plan est obligatoire');
    }

    if (this.editForm.get('planType')?.invalid && this.editForm.get('planType')?.touched) {
      errors.push('Le type de plan est obligatoire');
    }

    if (this.shouldShowPricingSection()) {
      if (this.editForm.get('abpPrice')?.invalid && this.editForm.get('abpPrice')?.touched) {
        errors.push('Le prix est obligatoire pour ce type de plan');
      }
    }

    // Erreurs métier
    const businessErrors = this.planabonnementFormService.validateBusinessRules(this.editForm);
    errors.push(...businessErrors);

    return errors;
  }

  /**
   * Prévisualise le plan (fonctionnalité future)
   */
  previewPlan(): void {
    // TODO: Implémenter la prévisualisation du plan
    const planData = this.planabonnementFormService.getPlanabonnement(this.editForm);
    console.log('Prévisualisation du plan:', planData);

    // Ici, vous pourriez ouvrir une modal ou naviguer vers une page de prévisualisation
    // this.router.navigate(['/plan-preview'], { state: { plan: planData } });
  }

  /**
   * Valide la cohérence des données du formulaire
   */
  validateForm(): boolean {
    const formValue = this.editForm.value;

    // Vérifications spécifiques selon le type de plan
    if (formValue.planType === PlanType.FREE && formValue.abpPrice && formValue.abpPrice > 0) {
      alert('Un plan gratuit ne peut pas avoir un prix supérieur à 0');
      return false;
    }

    if (formValue.usersLimit && formValue.usersLimit < 1 && formValue.usersLimit !== -1) {
      alert("La limite d'utilisateurs doit être au moins 1 ou -1 pour illimité");
      return false;
    }

    // Vérification des limites cohérentes
    if (formValue.planType === PlanType.SMS && (!formValue.smsLimit || formValue.smsLimit <= 0)) {
      if (formValue.smsLimit !== -1) {
        alert('Un plan SMS doit avoir une limite SMS positive ou illimitée (-1)');
        return false;
      }
    }

    if (formValue.planType === PlanType.WHATSAPP && (!formValue.whatsappLimit || formValue.whatsappLimit <= 0)) {
      if (formValue.whatsappLimit !== -1) {
        alert('Un plan WhatsApp doit avoir une limite WhatsApp positive ou illimitée (-1)');
        return false;
      }
    }

    return true;
  }

  /**
   * Réinitialise le formulaire avec des valeurs par défaut selon le type
   */
  resetFormByType(planType: string): void {
    // Sauvegarder les valeurs importantes à conserver
    const preservedValues = {
      abpName: this.editForm.get('abpName')?.value,
      abpDescription: this.editForm.get('abpDescription')?.value,
      id: this.editForm.get('id')?.value,
    };

    // Appliquer la configuration par défaut du type
    this.planabonnementFormService.configureByPlanType(this.editForm, planType);

    // Restaurer les valeurs à conserver
    this.editForm.patchValue(preservedValues);
  }

  /**
   * Formate une limite pour l'affichage (-1 = Illimité, 0 = Désactivé)
   */
  formatLimit(value: number | null | undefined): string {
    if (value === -1) return 'Illimité';
    if (value === 0) return 'Désactivé';
    if (value === null || value === undefined) return 'Non défini';
    return value.toString();
  }

  /**
   * Vérifie si une valeur représente une limite illimitée
   */
  isUnlimited(value: number | null | undefined): boolean {
    return value === -1;
  }

  /**
   * Obtient un résumé des caractéristiques du plan pour validation
   */
  getPlanSummary(): any {
    const formValue = this.editForm.value;
    return {
      name: formValue.abpName,
      type: formValue.planType,
      price: formValue.abpPrice,
      currency: formValue.abpCurrency,
      period: formValue.abpPeriod,
      smsLimit: this.formatLimit(formValue.smsLimit),
      whatsappLimit: this.formatLimit(formValue.whatsappLimit),
      usersLimit: this.formatLimit(formValue.usersLimit),
      templatesLimit: this.formatLimit(formValue.templatesLimit),
      features: this.getFormattedFeatures(),
      permissions: {
        canManageUsers: formValue.canManageUsers,
        canManageTemplates: formValue.canManageTemplates,
        canViewConversations: formValue.canViewConversations,
        canViewAnalytics: formValue.canViewAnalytics,
        prioritySupport: formValue.prioritySupport,
      },
      popular: formValue.abpPopular,
      active: formValue.active,
    };
  }

  /**
   * Exporte la configuration du plan (pour debug ou backup)
   */
  exportPlanConfig(): void {
    const config = this.getPlanSummary();
    const dataStr = JSON.stringify(config, null, 2);
    const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);

    const exportFileDefaultName = `plan-${config.name || 'nouveau'}-${Date.now()}.json`;

    const linkElement = document.createElement('a');
    linkElement.setAttribute('href', dataUri);
    linkElement.setAttribute('download', exportFileDefaultName);
    linkElement.click();
  }

  /**
   * Duplique le plan actuel (créer une copie)
   */
  duplicatePlan(): void {
    if (this.planabonnement?.id) {
      const currentValues = this.editForm.value;

      // Réinitialiser le formulaire pour création
      this.editForm.patchValue({
        ...currentValues,
        id: null,
        abpName: `${currentValues.abpName} (Copie)`,
        active: false, // Désactiver la copie par défaut
      });

      this.planabonnement = null;
    }
  }

  /**
   * Applique un preset de configuration selon le type métier
   */

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IPlanabonnement>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: error => this.onSaveError(error),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(error?: any): void {
    // Gestion améliorée des erreurs
    console.error('Erreur lors de la sauvegarde:', error);

    // Ici vous pourriez afficher un message d'erreur plus spécifique
    // selon le type d'erreur retournée par l'API
    if (error?.status === 409) {
      alert('Un plan avec ce nom existe déjà');
    } else if (error?.status === 400) {
      alert('Données invalides. Veuillez vérifier votre saisie.');
    } else {
      alert('Erreur lors de la sauvegarde. Veuillez réessayer.');
    }
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(planabonnement: IPlanabonnement): void {
    this.planabonnement = planabonnement;
    this.planabonnementFormService.resetForm(this.editForm, planabonnement);
  }

  /**
   * Méthodes utilitaires pour le template
   */

  // Vérifie si le formulaire a été modifié
  isFormDirty(): boolean {
    return this.editForm.dirty;
  }

  // Vérifie si le formulaire est valide
  isFormValid(): boolean {
    return this.editForm.valid && this.getValidationErrors().length === 0;
  }

  // Compte le nombre de fonctionnalités
  getFeaturesCount(): number {
    return this.getFormattedFeatures().length;
  }

  // Vérifie si c'est un nouveau plan
  isNewPlan(): boolean {
    return !this.planabonnement?.id;
  }

  // Calcule un score de complétude du formulaire (pour UX)
  getCompletionScore(): number {
    const requiredFields = ['abpName', 'planType'];
    const optionalFields = ['abpDescription', 'abpFeatures', 'abpButtonText'];

    let filledRequired = 0;
    let filledOptional = 0;

    requiredFields.forEach(field => {
      if (this.editForm.get(field)?.value) filledRequired++;
    });

    optionalFields.forEach(field => {
      if (this.editForm.get(field)?.value) filledOptional++;
    });

    const requiredScore = (filledRequired / requiredFields.length) * 70; // 70% pour les champs requis
    const optionalScore = (filledOptional / optionalFields.length) * 30; // 30% pour les champs optionnels

    return Math.round(requiredScore + optionalScore);
  }
}
