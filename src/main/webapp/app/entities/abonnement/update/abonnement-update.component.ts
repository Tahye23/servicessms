import { Component, inject, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IExtendedUser } from 'app/entities/extended-user/extended-user.model';
import { ExtendedUserService } from 'app/entities/extended-user/service/extended-user.service';
import { IPlanabonnement } from 'app/entities/planabonnement/planabonnement.model';
import { PlanabonnementService } from 'app/entities/planabonnement/service/planabonnement.service';
import { AbonnementService } from '../service/abonnement.service';
import { IAbonnement } from '../abonnement.model';
import { AbonnementFormGroup, AbonnementFormService } from './abonnement-form.service';

@Component({
  standalone: true,
  selector: 'jhi-abonnement-update',
  templateUrl: './abonnement-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class AbonnementUpdateComponent implements OnInit {
  isSaving = false;
  abonnement: IAbonnement | null = null;
  selectedPlan: IPlanabonnement | null = null;
  isCustomPlan = false;
  showResourcesSummary = false;

  extendedUsersSharedCollection: IExtendedUser[] = [];
  planabonnementsSharedCollection: IPlanabonnement[] = [];

  subscriptionStatusOptions = ['ACTIVE', 'SUSPENDED', 'EXPIRED', 'CANCELLED', 'TRIAL', 'PENDING_PAYMENT'];

  statusDisplayNames = {
    ACTIVE: 'Actif',
    SUSPENDED: 'Suspendu',
    EXPIRED: 'Expiré',
    CANCELLED: 'Annulé',
    TRIAL: "Période d'essai",
    PENDING_PAYMENT: 'Paiement en attente',
  };

  protected abonnementService = inject(AbonnementService);
  protected abonnementFormService = inject(AbonnementFormService);
  protected extendedUserService = inject(ExtendedUserService);
  protected planabonnementService = inject(PlanabonnementService);
  protected activatedRoute = inject(ActivatedRoute);

  editForm: AbonnementFormGroup = this.abonnementFormService.createAbonnementFormGroup();

  compareExtendedUser = (o1: IExtendedUser | null, o2: IExtendedUser | null): boolean =>
    this.extendedUserService.compareExtendedUser(o1, o2);

  comparePlanabonnement = (o1: IPlanabonnement | null, o2: IPlanabonnement | null): boolean =>
    this.planabonnementService.comparePlanabonnement(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ abonnement }) => {
      this.abonnement = abonnement;
      if (abonnement) {
        this.updateForm(abonnement);
        this.isCustomPlan = abonnement.isCustomPlan || false;
        this.selectedPlan = abonnement.plan || null;
        this.toggleCustomFields(!this.isCustomPlan);
        this.showResourcesSummary = true;
      }
      this.loadRelationshipsOptions();
    });
    /* this.editForm.get('isCustomPlan')?.valueChanges.subscribe((value: any) => {

      this.isCustomPlan = value === true || value === 'true';
      this.onPlanTypeChange();
    });

    /*this.editForm.get('plan')?.valueChanges.subscribe(() => {
      if (!this.isCustomPlan) {
        this.onPlanChange();
      }
    });*/

    //  this.editForm.valueChanges.subscribe(() => {
    //   this.updateResourcesSummary();
    //  });
  }

  previousState(): void {
    window.history.back();
  }

  saveWithValidation(): void {
    if (!this.validateForm()) {
      return;
    }
    this.save();
  }

  save(): void {
    this.isSaving = true;
    const selectedUser = this.editForm.get('user')?.value ?? null;
    if (selectedUser) {
      this.editForm.patchValue({
        userId: selectedUser.id,
      });
    }
    const abonnement = this.abonnementFormService.getAbonnement(this.editForm);

    if (typeof abonnement.isCustomPlan === 'string') {
      abonnement.isCustomPlan = abonnement.isCustomPlan === 'true';
    }
    console.log('Form valide ?', this.editForm.valid);
    console.log('abonnement', abonnement);

    if (abonnement.id !== null && abonnement.id !== undefined) {
      this.subscribeToSaveResponse(this.abonnementService.update(abonnement));
    } else {
      this.subscribeToSaveResponse(this.abonnementService.create(abonnement));
    }
  }

  onPlanTypeChange(): void {
    this.isCustomPlan = this.editForm.get('isCustomPlan')?.value ?? false;

    if (this.isCustomPlan) {
      this.editForm.get('plan')?.clearValidators();
      this.editForm.get('plan')?.setValue(null);

      this.toggleCustomFields(false);

      // Initialise à zéro les limites si vides
      if (!this.editForm.get('customSmsLimit')?.value) this.editForm.get('customSmsLimit')?.setValue(0);
      if (!this.editForm.get('customWhatsappLimit')?.value) this.editForm.get('customWhatsappLimit')?.setValue(0);

      this.selectedPlan = null;
    } else {
      this.editForm.get('plan')?.setValidators([
        /* Validators si nécessaire */
      ]);
      this.editForm.get('plan')?.updateValueAndValidity();

      if (this.editForm.get('plan')?.value) {
        this.onPlanChange();
      }
    }
    this.updateResourcesSummary();
  }

  onPlanChange(): void {
    const selectedPlan = this.editForm.get('plan')?.value ?? null;

    this.selectedPlan = selectedPlan;
    if (selectedPlan) {
      this.editForm.patchValue({
        customName: selectedPlan.abpName,
        customPrice: selectedPlan.abpPrice,
        customPeriod: selectedPlan.abpPeriod,
        customSmsLimit: selectedPlan.smsLimit,
        planId: selectedPlan.id,
        plan: selectedPlan,
        customWhatsappLimit: selectedPlan.whatsappLimit,
        customUsersLimit: selectedPlan.usersLimit,
        customTemplatesLimit: selectedPlan.templatesLimit,
        customStorageLimitMb: selectedPlan.storageLimitMb,
        customCanManageUsers: selectedPlan.canManageUsers,
        customCanManageTemplates: selectedPlan.canManageTemplates,
        customCanViewConversations: selectedPlan.canViewConversations,
        customCanViewAnalytics: selectedPlan.canViewAnalytics,
        customPrioritySupport: selectedPlan.prioritySupport,
        canViewDashboard: selectedPlan.canViewDashboard,
        canManageAPI: selectedPlan.canManageAPI,
      });
      console.log('selectedPlan', this.editForm.value);
      if (selectedPlan && selectedPlan.customPlan === true) {
        this.toggleCustomFields(false);
      } else {
        this.toggleCustomFields(true);
      }
    } else {
      // Si aucun plan, réactive saisie des champs custom
      this.toggleCustomFields(false);
      this.clearCustomPlanFields();
    }

    this.updateResourcesSummary();
  }

  private toggleCustomFields(disable: boolean): void {
    const controls = [
      'customName',
      'customPrice',
      'customPeriod',
      'customDescription',
      'canManageAPI',
      'canViewDashboard',
      'customSmsLimit',
      'customWhatsappLimit',
      'customUsersLimit',
      'customTemplatesLimit',
      'customApiCallsLimit',
      'customStorageLimitMb',
      'customCanManageUsers',
      'customCanManageTemplates',
      'customCanViewConversations',
      'customCanViewAnalytics',
      'customPrioritySupport',
    ];

    controls.forEach(controlName => {
      const control = this.editForm.get(controlName);
      if (!control) return;

      if (disable) {
        control.disable({ emitEvent: false });
      } else {
        control.enable({ emitEvent: false });
      }
    });
  }

  private clearCustomPlanFields(): void {
    const customFields = [
      'customName',
      'customPrice',
      'customPeriod',
      'customDescription',
      'customSmsLimit',
      'customWhatsappLimit',
      'customUsersLimit',
      'customTemplatesLimit',
      'customApiCallsLimit',
      'customStorageLimitMb',
      'customCanManageUsers',
      'customCanManageTemplates',
      'customCanViewConversations',
      'customCanViewAnalytics',
      'customPrioritySupport',
    ];

    customFields.forEach(field => {
      this.editForm.get(field)?.setValue(null);
    });
  }

  private updateResourcesSummary(): void {
    this.showResourcesSummary = !!(this.selectedPlan || this.isCustomPlan);
  }

  getStatusDisplayName(status: string): string {
    return this.statusDisplayNames[status as keyof typeof this.statusDisplayNames] || status;
  }

  formatLimit(limit: number | null | undefined): string {
    if (limit === null || limit === undefined) return 'Non défini';
    if (limit === -1) return 'Illimité';
    return limit.toString();
  }

  validateForm(): boolean {
    const formValue = this.editForm.value;

    if (!this.isCustomPlan && !formValue.plan) {
      alert('Veuillez sélectionner un plan ou créer un plan personnalisé.');
      return false;
    }

    if (formValue.endDate && formValue.startDate) {
      const start = new Date(formValue.startDate);
      const end = new Date(formValue.endDate);
      if (end <= start) {
        alert('La date de fin doit être postérieure à la date de début.');
        return false;
      }
    }

    if (formValue.isTrial && typeof formValue.trialEndDate === 'string' && typeof formValue.startDate === 'string') {
      const trial = new Date(formValue.trialEndDate);
      const start = new Date(formValue.startDate);
      if (trial <= start) {
        alert("La date de fin d'essai doit être postérieure à la date de début.");
        return false;
      }
    }
    if (!formValue.user) {
      // Optionnel : marquer tous les champs comme "touched" pour afficher les erreurs
      this.editForm.markAllAsTouched();
      console.warn('Formulaire invalide, sauvegarde annulée');
      alert('Formulaire invalide, sauvegarde annulée');
      return false;
    }

    return true;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IAbonnement>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    alert('Erreur lors de la sauvegarde. Veuillez réessayer.');
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(abonnement: IAbonnement): void {
    this.abonnement = abonnement;
    this.abonnementFormService.resetForm(this.editForm, abonnement);

    this.extendedUsersSharedCollection = this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(
      this.extendedUsersSharedCollection,
      abonnement.user,
    );
    this.planabonnementsSharedCollection = this.planabonnementService.addPlanabonnementToCollectionIfMissing<IPlanabonnement>(
      this.planabonnementsSharedCollection,
      abonnement.plan,
    );

    this.isCustomPlan = abonnement.isCustomPlan || false;
    this.selectedPlan = abonnement.plan || null;
    this.toggleCustomFields(!this.isCustomPlan);
  }

  protected loadRelationshipsOptions(): void {
    this.extendedUserService
      .query()
      .pipe(map((res: HttpResponse<IExtendedUser[]>) => res.body ?? []))
      .pipe(
        map((extendedUsers: IExtendedUser[]) =>
          this.extendedUserService.addExtendedUserToCollectionIfMissing<IExtendedUser>(extendedUsers, this.abonnement?.user),
        ),
      )
      .subscribe((extendedUsers: IExtendedUser[]) => {
        this.extendedUsersSharedCollection = extendedUsers;
      });

    this.planabonnementService
      .query({ filter: 'active-eq-true' })
      .pipe(map((res: HttpResponse<IPlanabonnement[]>) => res.body ?? []))
      .pipe(
        map((planabonnements: IPlanabonnement[]) =>
          this.planabonnementService.addPlanabonnementToCollectionIfMissing<IPlanabonnement>(planabonnements, this.abonnement?.plan),
        ),
      )
      .subscribe((planabonnements: IPlanabonnement[]) => {
        this.planabonnementsSharedCollection = planabonnements;
      });
  }
}
