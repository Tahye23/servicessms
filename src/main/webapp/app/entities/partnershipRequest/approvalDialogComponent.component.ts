import { Component, Inject } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef, MatDialogTitle } from '@angular/material/dialog';
import { IPartnershipRequest } from './partnership-request.model';
import { MatButton } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatFormField } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatIcon } from '@angular/material/icon';
import { NgForOf, NgIf } from '@angular/common';

@Component({
  selector: 'app-approval-dialog',
  standalone: true,
  imports: [
    MatDialogActions,
    MatButton,
    FormsModule,
    MatFormField,
    MatInput,
    MatFormFieldModule,
    MatDialogContent,
    MatDialogTitle,
    MatIcon,
    NgIf,
    NgForOf,
  ],
  template: `
    <div class="modern-dialog-container">
      <!-- En-tête avec gradient -->
      <div class="bg-gradient-to-r from-blue-500 to-purple-600 text-white p-6 -m-6 mb-6 rounded-t-2xl">
        <div class="flex items-center space-x-4">
          <div class="w-12 h-12 bg-white/20 backdrop-blur-sm rounded-2xl flex items-center justify-center">
            <mat-icon class="text-white">
              {{ data.action === 'approve' || data.action === 'bulk-approve' ? 'check_circle' : 'cancel' }}
            </mat-icon>
          </div>
          <div>
            <h2 class="text-2xl font-bold">
              {{ getDialogTitle() }}
            </h2>
            <p class="text-blue-100 mt-1">
              {{ getDialogSubtitle() }}
            </p>
          </div>
        </div>
      </div>

      <mat-dialog-content class="space-y-6">
        <!-- Informations de la demande pour action simple -->
        <div class="bg-gradient-to-r from-slate-50 to-blue-50 rounded-2xl p-6" *ngIf="data.request && !data.requests">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-500 rounded-xl flex items-center justify-center">
                <mat-icon class="text-white text-sm">person</mat-icon>
              </div>
              <div>
                <p class="text-sm text-slate-500">Demandeur</p>
                <p class="font-semibold text-slate-900">{{ getFullName(data.request) }}</p>
              </div>
            </div>

            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-gradient-to-r from-emerald-500 to-cyan-500 rounded-xl flex items-center justify-center">
                <mat-icon class="text-white text-sm">business</mat-icon>
              </div>
              <div>
                <p class="text-sm text-slate-500">Entreprise</p>
                <p class="font-semibold text-slate-900">{{ data.request.companyName }}</p>
              </div>
            </div>

            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-gradient-to-r from-amber-500 to-orange-500 rounded-xl flex items-center justify-center">
                <mat-icon class="text-white text-sm">card_membership</mat-icon>
              </div>
              <div>
                <p class="text-sm text-slate-500">Plan sélectionné</p>
                <p class="font-semibold text-slate-900">{{ data.request.selectedPlanName }}</p>
              </div>
            </div>

            <div class="flex items-center space-x-3">
              <div class="w-10 h-10 bg-gradient-to-r from-pink-500 to-rose-500 rounded-xl flex items-center justify-center">
                <mat-icon class="text-white text-sm">email</mat-icon>
              </div>
              <div>
                <p class="text-sm text-slate-500">Email</p>
                <p class="font-semibold text-slate-900">{{ data.request.email }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Informations pour actions groupées -->
        <div class="bg-gradient-to-r from-slate-50 to-blue-50 rounded-2xl p-6" *ngIf="data.requests && data.requests.length > 0">
          <div class="flex items-center space-x-4 mb-4">
            <div class="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-500 rounded-2xl flex items-center justify-center">
              <mat-icon class="text-white">group</mat-icon>
            </div>
            <div>
              <p class="font-bold text-lg text-slate-900">{{ data.requests.length }} demandes sélectionnées</p>
              <p class="text-sm text-slate-600">Action qui sera appliquée à toutes les demandes</p>
            </div>
          </div>

          <!-- Liste des demandes sélectionnées (limitée) -->
          <div class="space-y-2 max-h-40 overflow-y-auto">
            <div
              *ngFor="let request of data.requests.slice(0, 5); let i = index"
              class="flex items-center justify-between p-3 bg-white rounded-xl border border-slate-200"
            >
              <div class="flex items-center space-x-3">
                <div
                  class="w-8 h-8 bg-gradient-to-r from-slate-400 to-slate-500 rounded-lg flex items-center justify-center text-white text-sm font-semibold"
                >
                  {{ getFullName(request).charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="font-medium text-slate-900">{{ getFullName(request) }}</p>
                  <p class="text-sm text-slate-500">{{ request.companyName }}</p>
                </div>
              </div>
              <span class="text-xs text-slate-400">#{{ request.id }}</span>
            </div>

            <div *ngIf="data.requests.length > 5" class="text-center p-3 text-slate-500 text-sm border-t border-slate-200">
              Et {{ data.requests.length - 5 }} autres demandes...
            </div>
          </div>
        </div>

        <!-- Zone de saisie des notes -->
        <div class="space-y-4">
          <mat-form-field appearance="outline" class="w-full modern-textarea">
            <mat-label>
              <span class="flex items-center">
                <mat-icon class="mr-2 text-sm">note_add</mat-icon>
                Notes administratives
              </span>
            </mat-label>
            <textarea
              matInput
              [(ngModel)]="adminNotes"
              rows="4"
              placeholder="Ajoutez des notes concernant cette décision... (optionnel)"
              class="resize-none rounded-xl"
            >
            </textarea>
            <mat-hint>Ces notes seront ajoutées au dossier de la demande</mat-hint>
          </mat-form-field>
        </div>

        <!-- Zone d'avertissement pour les rejets -->
        <div *ngIf="isRejectAction()" class="bg-gradient-to-r from-red-50 to-pink-50 border-l-4 border-red-400 p-4 rounded-r-xl">
          <div class="flex items-start space-x-3">
            <mat-icon class="text-red-500 mt-1">warning</mat-icon>
            <div>
              <h4 class="font-semibold text-red-800">Attention</h4>
              <p class="text-sm text-red-700 mt-1">
                Cette action rejettera définitivement {{ isGroupAction() ? 'les demandes sélectionnées' : 'la demande' }}. Un email de
                notification sera envoyé {{ isGroupAction() ? 'aux demandeurs' : 'au demandeur' }}.
              </p>
            </div>
          </div>
        </div>

        <!-- Zone de confirmation pour les approbations -->
        <div *ngIf="isApprovalAction()" class="bg-gradient-to-r from-emerald-50 to-green-50 border-l-4 border-emerald-400 p-4 rounded-r-xl">
          <div class="flex items-start space-x-3">
            <mat-icon class="text-emerald-500 mt-1">check_circle</mat-icon>
            <div>
              <h4 class="font-semibold text-emerald-800">Confirmation</h4>
              <p class="text-sm text-emerald-700 mt-1">
                {{ isGroupAction() ? 'Les demandes seront approuvées' : 'La demande sera approuvée' }} et
                {{ isGroupAction() ? 'les demandeurs recevront' : 'le demandeur recevra' }} un email de confirmation avec les prochaines
                étapes.
              </p>
            </div>
          </div>
        </div>
      </mat-dialog-content>

      <!-- Actions du dialogue -->
      <mat-dialog-actions class="flex justify-end space-x-4 pt-6 border-t border-slate-200">
        <button
          mat-stroked-button
          (click)="onCancel()"
          class="px-6 py-2 border-2 border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-all duration-200"
        >
          <mat-icon class="mr-2">close</mat-icon>
          Annuler
        </button>

        <button
          mat-raised-button
          [class]="getConfirmButtonClass()"
          (click)="onConfirm()"
          class="px-8 py-2 rounded-xl shadow-lg hover:shadow-xl transition-all duration-300"
        >
          <mat-icon class="mr-2">
            {{ data.action === 'approve' || data.action === 'bulk-approve' ? 'check_circle' : 'cancel' }}
          </mat-icon>
          {{ getConfirmButtonText() }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .modern-dialog-container {
        @apply p-6 bg-white rounded-3xl max-w-2xl;
      }

      .modern-textarea ::ng-deep .mat-mdc-form-field-flex {
        @apply rounded-2xl bg-slate-50/80 border-slate-200;
      }

      .modern-textarea ::ng-deep .mat-mdc-form-field-outline {
        @apply rounded-2xl;
      }

      ::ng-deep .modern-dialog .mat-mdc-dialog-container {
        @apply rounded-3xl shadow-2xl border border-slate-200;
      }

      ::ng-deep .modern-backdrop {
        @apply backdrop-blur-sm;
      }
    `,
  ],
})
export class ApprovalDialogComponent {
  adminNotes = '';

  constructor(
    public dialogRef: MatDialogRef<ApprovalDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {}

  getFullName(request: IPartnershipRequest): string {
    return `${request.firstName || ''} ${request.lastName || ''}`.trim();
  }

  getDialogTitle(): string {
    switch (this.data.action) {
      case 'approve':
        return 'Approuver la demande';
      case 'reject':
        return 'Rejeter la demande';
      case 'bulk-approve':
        return `Approuver ${this.data.requests?.length || 0} demandes`;
      case 'bulk-reject':
        return `Rejeter ${this.data.requests?.length || 0} demandes`;
      default:
        return this.data.title || 'Action sur la demande';
    }
  }

  getDialogSubtitle(): string {
    switch (this.data.action) {
      case 'approve':
        return "Confirmer l'approbation de cette demande de partenariat";
      case 'reject':
        return 'Confirmer le rejet de cette demande de partenariat';
      case 'bulk-approve':
        return 'Approuver toutes les demandes sélectionnées en une fois';
      case 'bulk-reject':
        return 'Rejeter toutes les demandes sélectionnées en une fois';
      default:
        return 'Veuillez confirmer votre action';
    }
  }

  getConfirmButtonText(): string {
    switch (this.data.action) {
      case 'approve':
        return 'Approuver';
      case 'reject':
        return 'Rejeter';
      case 'bulk-approve':
        return `Approuver tout (${this.data.requests?.length || 0})`;
      case 'bulk-reject':
        return `Rejeter tout (${this.data.requests?.length || 0})`;
      default:
        return 'Confirmer';
    }
  }

  getConfirmButtonClass(): string {
    const baseClasses = 'inline-flex items-center font-semibold';

    if (this.isApprovalAction()) {
      return `${baseClasses} bg-gradient-to-r from-emerald-500 to-green-500 text-white hover:from-emerald-600 hover:to-green-600`;
    } else {
      return `${baseClasses} bg-gradient-to-r from-red-500 to-pink-500 text-white hover:from-red-600 hover:to-pink-600`;
    }
  }

  isApprovalAction(): boolean {
    return this.data.action === 'approve' || this.data.action === 'bulk-approve';
  }

  isRejectAction(): boolean {
    return this.data.action === 'reject' || this.data.action === 'bulk-reject';
  }

  isGroupAction(): boolean {
    return this.data.action === 'bulk-approve' || this.data.action === 'bulk-reject';
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onConfirm(): void {
    this.dialogRef.close({
      adminNotes: this.adminNotes,
      action: this.data.action,
      confirmed: true,
    });
  }
}
