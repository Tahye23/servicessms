import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { finalize, map } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { Validators } from '@angular/forms';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IApi } from 'app/entities/api/api.model';
import { ApiService } from 'app/entities/api/service/api.service';
import { IPlanabonnement } from 'app/entities/planabonnement/planabonnement.model';
import { PlanabonnementService } from 'app/entities/planabonnement/service/planabonnement.service';
import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { ApplicationService } from '../service/application.service';
import { IApplication, IWebhookTest, NewApplication } from '../application.model';
import { ApplicationFormService, ApplicationFormGroup } from './application-form.service';

@Component({
  standalone: true,
  selector: 'jhi-application-update',
  templateUrl: './application-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule, CommonModule],
})
export class ApplicationUpdateComponent implements OnInit, OnDestroy {
  isSaving = false;
  application: IApplication | null = null;

  // Relations
  apisSharedCollection: IApi[] = [];
  planabonnementsSharedCollection: IPlanabonnement[] = [];
  extendedUsersSharedCollection: IExtendedUser[] = [];

  // États spécifiques
  isTestingWebhook = false;
  webhookTestResult: { success: boolean; message: string } | null = null;

  // Messages
  successMessage: string | null = null;
  errorMessage: string | null = null;

  // Subscriptions
  private routeSubscription: Subscription | null = null;

  // Services injectés
  protected applicationService = inject(ApplicationService);
  protected applicationFormService = inject(ApplicationFormService);
  protected apiService = inject(ApiService);
  protected planabonnementService = inject(PlanabonnementService);
  protected extendedUserService = inject(ExtendedUserService);
  protected activatedRoute = inject(ActivatedRoute);
  protected router = inject(Router);

  // Formulaire
  editForm: ApplicationFormGroup = this.applicationFormService.createApplicationFormGroup();

  // Fonctions de comparaison
  compareApi = (o1: IApi | null, o2: IApi | null): boolean => this.apiService.compareApi(o1, o2);
  comparePlanabonnement = (o1: IPlanabonnement | null, o2: IPlanabonnement | null): boolean =>
    this.planabonnementService.comparePlanabonnement(o1, o2);
  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  // Fonctions de tracking pour les listes
  trackApi = (index: number, item: IApi): any => item.id;
  trackPlan = (index: number, item: IPlanabonnement): any => item.id;
  trackUser = (index: number, item: IExtendedUser): any => item.id;

  ngOnInit(): void {
    this.setupFormValidations();

    this.routeSubscription = this.activatedRoute.data.subscribe(({ application }) => {
      this.application = application;
      if (application) {
        this.updateForm(application);
      }

      this.loadRelationshipsOptions();
    });

    // Écouter les changements du formulaire pour la validation en temps réel
    this.setupFormWatchers();
  }

  ngOnDestroy(): void {
    if (this.routeSubscription) {
      this.routeSubscription.unsubscribe();
    }
  }

  /**
   * Configure les validations du formulaire
   */
  private setupFormValidations(): void {
    // Le nom est obligatoire
    const nameControl = this.editForm.get('name');
    if (nameControl) {
      nameControl.setValidators([Validators.required, Validators.minLength(2), Validators.maxLength(100)]);
    }

    // Validation de l'URL du webhook
    const webhookUrlControl = this.editForm.get('webhookUrl');
    if (webhookUrlControl) {
      webhookUrlControl.setValidators([Validators.pattern(/^https?:\/\/.+/)]);
    }

    // Validation des limites
    const dailyLimitControl = this.editForm.get('dailyLimit');
    if (dailyLimitControl) {
      dailyLimitControl.setValidators([Validators.min(1), Validators.max(1000000)]);
    }

    const monthlyLimitControl = this.editForm.get('monthlyLimit');
    if (monthlyLimitControl) {
      monthlyLimitControl.setValidators([Validators.min(1), Validators.max(10000000)]);
    }
  }

  /**
   * Configure les watchers du formulaire
   */
  private setupFormWatchers(): void {
    // Auto-génération du nom basé sur l'environnement
    this.editForm.get('environment')?.valueChanges.subscribe(environment => {
      const nameControl = this.editForm.get('name');
      if (nameControl && !nameControl.value && environment) {
        const suggestedName = this.generateApplicationName(environment);
        nameControl.setValue(suggestedName);
      }
    });

    // Validation en temps réel de l'URL webhook
    this.editForm.get('webhookUrl')?.valueChanges.subscribe(url => {
      if (url) {
        this.validateWebhookUrl(url);
      } else {
        this.webhookTestResult = null;
      }
    });

    //  GESTION DE LA CASE À COCHER "JAMAIS EXPIRER"
    this.editForm.get('tokenNeverExpires')?.valueChanges.subscribe(neverExpires => {
      const dateControl = this.editForm.get('tokenDateExpiration');
      if (neverExpires) {
        // Si "jamais expirer" est coché, désactiver et vider le champ date
        dateControl?.clearValidators();
        dateControl?.setValue(null);
        dateControl?.disable();
      } else {
        // Sinon, activer et rendre obligatoire
        dateControl?.setValidators([Validators.required]);
        dateControl?.enable();
      }
      dateControl?.updateValueAndValidity();
    });
  }

  /**
   * Génère un nom d'application suggéré
   */
  private generateApplicationName(environment: string): string {
    const envNames = {
      development: 'App Dev',
      staging: 'App Test',
      production: 'App Prod',
    };
    return envNames[environment as keyof typeof envNames] || 'Mon Application';
  }

  /**
   * Valide une URL de webhook
   */
  private validateWebhookUrl(url: string): void {
    if (url.match(/^https?:\/\/.+/)) {
      this.applicationService.validateWebhookUrl(url).subscribe({
        next: response => {
          const result = response.body;
          if (result && result.valid) {
            this.webhookTestResult = { success: true, message: 'URL valide' };
          } else {
            this.webhookTestResult = {
              success: false,
              message: result?.message || 'URL invalide',
            };
          }
        },
        error: () => {
          this.webhookTestResult = {
            success: false,
            message: "Impossible de valider l'URL",
          };
        },
      });
    }
  }

  /**
   * Retourne à la page précédente
   */
  previousState(): void {
    this.router.navigate(['/application']);
  }

  /**
   * Sauvegarde l'application
   *  CORRIGÉ: Type correction avec NewApplication
   */
  save(): void {
    if (this.editForm.invalid) {
      this.markFormGroupTouched(this.editForm);
      this.showErrorMessage('Veuillez corriger les erreurs dans le formulaire.');
      return;
    }

    this.isSaving = true;
    this.clearMessages();

    const rawApplication = this.applicationFormService.getApplication(this.editForm);

    //  CONVERSION DES DONNÉES AVANT ENVOI
    // Vérifier si c'est une création (NewApplication) ou une mise à jour (IApplication)
    const application = this.prepareApplicationForSave(rawApplication);

    if (application.id !== null && application.id !== undefined) {
      //  Type casting pour la mise à jour
      this.subscribeToSaveResponse(this.applicationService.update(application as IApplication));
    } else {
      //  Type casting pour la création
      this.subscribeToSaveResponse(this.applicationService.create(application as NewApplication));
    }
  }

  /**
   *  MÉTHODE : Prépare les données avant envoi au backend
   * Accepte IApplication | NewApplication et retourne le même type
   */
  private prepareApplicationForSave(application: IApplication | NewApplication): IApplication | NewApplication {
    // 1. Convertir la date du token en ISO si elle existe
    if (application.tokenDateExpiration) {
      application.tokenDateExpiration = new Date(application.tokenDateExpiration).toISOString();
    }

    // 2. Construire le tableau allowedServices depuis les checkboxes
    const allowedServices: string[] = [];
    if (this.editForm.get('allowSms')?.value) allowedServices.push('sms');
    if (this.editForm.get('allowWhatsapp')?.value) allowedServices.push('whatsapp');
    if (this.editForm.get('allowEmail')?.value) allowedServices.push('email');
    if (this.editForm.get('allowVoice')?.value) allowedServices.push('voice');

    //  Assigner le tableau (jamais vide ou null)
    application.allowedServices = allowedServices.length > 0 ? allowedServices : [];

    // 3. Convertir les limites en nombres entiers
    if (application.dailyLimit !== null && application.dailyLimit !== undefined) {
      application.dailyLimit = parseInt(String(application.dailyLimit), 10);
    }
    if (application.monthlyLimit !== null && application.monthlyLimit !== undefined) {
      application.monthlyLimit = parseInt(String(application.monthlyLimit), 10);
    }

    // 4. Nettoyer les champs vides
    if (!application.webhookUrl) {
      application.webhookUrl = null;
    }
    if (!application.webhookSecret) {
      application.webhookSecret = null;
    }

    return application;
  }

  /**
   * Teste le webhook configuré
   */
  testWebhook(): void {
    const webhookUrl = this.editForm.get('webhookUrl')?.value;
    const webhookSecret = this.editForm.get('webhookSecret')?.value;

    if (!webhookUrl) return;

    this.isTestingWebhook = true;
    this.webhookTestResult = null;

    const testData: IWebhookTest = {
      url: webhookUrl,
      secret: webhookSecret || undefined,
      testPayload: {
        event: 'test',
        timestamp: new Date().toISOString(),
        data: { message: 'Test webhook from application form' },
      },
    };

    const appId = this.application?.id || 0;

    this.applicationService.testWebhook(appId, testData).subscribe({
      next: response => {
        this.isTestingWebhook = false;

        const result = response.body;
        this.webhookTestResult = {
          success: true,
          message: `Test réussi ! Statut: ${response.status}${
            result?.response?.responseTime ? `, Temps: ${result.response.responseTime}ms` : ''
          }`,
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
   * Obtient les services sélectionnés
   */
  getSelectedServices(): string[] {
    const services: string[] = [];

    if (this.editForm.get('allowSms')?.value) services.push('sms');
    if (this.editForm.get('allowWhatsapp')?.value) services.push('whatsapp');
    if (this.editForm.get('allowEmail')?.value) services.push('email');
    if (this.editForm.get('allowVoice')?.value) services.push('voice');

    return services;
  }

  /**
   * Vérifie si un champ a des erreurs
   */
  hasFieldError(fieldName: string): boolean {
    const control = this.editForm.get(fieldName);
    return !!(control && control.errors && control.touched);
  }

  /**
   * Obtient les erreurs d'un champ
   */
  getFieldErrors(fieldName: string): string[] {
    const control = this.editForm.get(fieldName);
    const errors: string[] = [];

    if (control && control.errors && control.touched) {
      if (control.errors['required']) {
        errors.push('Ce champ est obligatoire');
      }
      if (control.errors['minlength']) {
        errors.push(`Minimum ${control.errors['minlength'].requiredLength} caractères`);
      }
      if (control.errors['maxlength']) {
        errors.push(`Maximum ${control.errors['maxlength'].requiredLength} caractères`);
      }
      if (control.errors['pattern']) {
        errors.push('Format invalide');
      }
      if (control.errors['min']) {
        errors.push(`Valeur minimum: ${control.errors['min'].min}`);
      }
      if (control.errors['max']) {
        errors.push(`Valeur maximum: ${control.errors['max'].max}`);
      }
    }

    return errors;
  }

  /**
   * Marque tous les champs comme touchés
   */
  private markFormGroupTouched(formGroup: any): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control && typeof control === 'object' && control.controls) {
        this.markFormGroupTouched(control);
      }
    });
  }

  /**
   * Gère la réponse de sauvegarde
   */
  protected subscribeToSaveResponse(result: Observable<HttpResponse<IApplication>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: response => this.onSaveSuccess(response),
      error: error => this.onSaveError(error),
    });
  }

  /**
   * Succès de la sauvegarde
   */
  protected onSaveSuccess(response: HttpResponse<IApplication>): void {
    const isCreation = !this.application?.id;
    const actionText = isCreation ? 'créée' : 'modifiée';

    this.showSuccessMessage(`Application ${actionText} avec succès !`);

    // Rediriger vers la page de détail après création/modification
    if (response.body?.id) {
      setTimeout(() => {
        this.router.navigate(['/application', response.body!.id, 'view']);
      }, 1500);
    } else {
      setTimeout(() => {
        this.previousState();
      }, 1500);
    }
  }

  /**
   * Erreur de sauvegarde
   */
  protected onSaveError(error: any): void {
    console.error('Erreur lors de la sauvegarde:', error);

    let errorMessage = 'Une erreur est survenue lors de la sauvegarde.';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.status === 400) {
      errorMessage = 'Données invalides. Veuillez vérifier les informations saisies.';
    } else if (error.status === 409) {
      errorMessage = 'Une application avec ce nom existe déjà.';
    } else if (error.status === 403) {
      errorMessage = "Vous n'avez pas les permissions pour effectuer cette action.";
    }

    this.showErrorMessage(errorMessage);
  }

  /**
   * Finalise la sauvegarde
   */
  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  /**
   * Met à jour le formulaire avec les données de l'application
   */
  protected updateForm(application: IApplication): void {
    this.application = application;
    this.applicationFormService.resetForm(this.editForm, application);

    // Mettre à jour les checkboxes des services
    if (application.allowedServices) {
      this.editForm.patchValue({
        allowSms: application.allowedServices.includes('sms'),
        allowWhatsapp: application.allowedServices.includes('whatsapp'),
        allowEmail: application.allowedServices.includes('email'),
        allowVoice: application.allowedServices.includes('voice'),
      });
    }

    // Mettre à jour les collections partagées
    this.apisSharedCollection = this.apiService.addApiToCollectionIfMissing<IApi>(this.apisSharedCollection, application.api);
    this.planabonnementsSharedCollection = this.planabonnementService.addPlanabonnementToCollectionIfMissing<IPlanabonnement>(
      this.planabonnementsSharedCollection,
      application.planabonnement,
    );
    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      application.utilisateur,
    );
  }

  /**
   * Charge les options de relations
   */
  protected loadRelationshipsOptions(): void {
    this.apiService
      .query()
      .pipe(map((res: HttpResponse<IApi[]>) => res.body ?? []))
      .pipe(map((apis: IApi[]) => this.apiService.addApiToCollectionIfMissing<IApi>(apis, this.application?.api)))
      .subscribe((apis: IApi[]) => (this.apisSharedCollection = apis));

    this.planabonnementService
      .query()
      .pipe(map((res: HttpResponse<IPlanabonnement[]>) => res.body ?? []))
      .pipe(
        map((planabonnements: IPlanabonnement[]) =>
          this.planabonnementService.addPlanabonnementToCollectionIfMissing<IPlanabonnement>(
            planabonnements,
            this.application?.planabonnement,
          ),
        ),
      )
      .subscribe((planabonnements: IPlanabonnement[]) => (this.planabonnementsSharedCollection = planabonnements));

    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.application?.utilisateur),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => {
        this.extendedUsersSharedCollection = extendedUsers.sort((a, b) => (a.user?.login || '').localeCompare(b.user?.login || ''));
      });
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

  /**
   * Obtient la date minimale pour le datepicker (aujourd'hui)
   */
  getMinDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Calcule une date suggérée (6 mois)
   */
  getSuggestedExpirationDate(): string {
    const date = new Date();
    date.setMonth(date.getMonth() + 6);
    return date.toISOString().split('T')[0];
  }
}
