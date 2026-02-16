import { Component, Input, Output, EventEmitter, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { CommonModule } from '@angular/common';
import dayjs from 'dayjs/esm';
import { ITokensApp } from '../tokens-app.model';
import { TokensAppService, CreateTokenRequest } from '../service/tokens-app.service';
import { NotificationService } from '../../../shared/notification.service';

@Component({
  selector: 'jhi-token-management-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
      <div class="bg-white rounded-xl shadow-2xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div class="modal-header bg-gradient-to-r from-blue-600 to-blue-700 text-white">
          <h4 class="modal-title flex items-center">
            <svg class="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M15 7a2 2 0 012 2m0 0a2 2 0 012 2m-2-2a2 2 0 00-2 2m2-2V5a2 2 0 00-2-2m0 0V3a2 2 0 00-2-2m2 2H9a2 2 0 00-2 2v10a2 2 0 002 2h6a2 2 0 002-2V7z"
              />
            </svg>
            {{ isEditMode ? 'Gérer le Token' : 'Créer un nouveau Token' }}
          </h4>
          <button type="button" class="btn-close btn-close-white" (click)="activeModal.dismiss()"></button>
        </div>

        <div class="modal-body p-6">
          <form [formGroup]="tokenForm" (ngSubmit)="onSubmit()">
            <!-- Application Selection (only for new tokens) -->
            <div *ngIf="!isEditMode" class="mb-4">
              <label class="block text-sm font-semibold text-gray-700 mb-2">Application (optionnelle)</label>
              <select
                formControlName="applicationId"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">Token général (sans application)</option>
                <option *ngFor="let app of applications" [value]="app.id">{{ app.name }}</option>
              </select>
            </div>

            <!-- Expiration Date Selection -->
            <div class="mb-4">
              <label class="block text-sm font-semibold text-gray-700 mb-2">Date d'expiration</label>

              <!-- Preset Options -->
              <div class="grid grid-cols-3 gap-2 mb-3">
                <button
                  type="button"
                  (click)="setExpirationPreset('1month')"
                  class="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-blue-50 focus:ring-2 focus:ring-blue-500"
                  [class.bg-blue-100]="selectedPreset === '1month'"
                >
                  1 mois
                </button>
                <button
                  type="button"
                  (click)="setExpirationPreset('3months')"
                  class="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-blue-50 focus:ring-2 focus:ring-blue-500"
                  [class.bg-blue-100]="selectedPreset === '3months'"
                >
                  3 mois
                </button>
                <button
                  type="button"
                  (click)="setExpirationPreset('6months')"
                  class="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-blue-50 focus:ring-2 focus:ring-blue-500"
                  [class.bg-blue-100]="selectedPreset === '6months'"
                >
                  6 mois
                </button>
              </div>

              <!-- Custom Date -->
              <div class="flex items-center space-x-2">
                <input
                  type="checkbox"
                  [(ngModel)]="useCustomDate"
                  [ngModelOptions]="{ standalone: true }"
                  class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <label class="text-sm text-gray-600">Date personnalisée</label>
              </div>

              <input
                *ngIf="useCustomDate"
                type="datetime-local"
                formControlName="dateExpiration"
                class="w-full mt-2 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <!-- Current Token Display (Edit Mode) -->
            <div *ngIf="isEditMode && currentToken" class="mb-4">
              <label class="block text-sm font-semibold text-gray-700 mb-2">Token actuel</label>
              <div class="flex space-x-2">
                <input
                  type="text"
                  [value]="showCurrentToken ? currentToken.token : '••••••••••••••••••••••••••••••••'"
                  readonly
                  class="flex-1 px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg text-sm"
                />
                <button
                  type="button"
                  (click)="toggleTokenVisibility()"
                  class="px-3 py-1 text-xs bg-gray-600 text-white rounded hover:bg-gray-700"
                >
                  {{ showCurrentToken ? 'Masquer' : 'Afficher' }}
                </button>
                <button
                  type="button"
                  (click)="copyToClipboard(currentToken.token!)"
                  class="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  Copier
                </button>
              </div>
            </div>

            <!-- Status Toggle (Edit Mode) -->
            <div *ngIf="isEditMode" class="mb-4">
              <label class="flex items-center space-x-2">
                <input type="checkbox" formControlName="active" class="rounded border-gray-300 text-green-600 focus:ring-green-500" />
                <span class="text-sm font-semibold text-gray-700">Token actif</span>
              </label>
            </div>

            <!-- Generated Token Display (New Token) -->
            <div *ngIf="generatedToken" class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
              <label class="block text-sm font-semibold text-green-800 mb-2">Token généré avec succès !</label>
              <div class="flex space-x-2">
                <input
                  type="text"
                  [value]="generatedToken"
                  readonly
                  class="flex-1 px-3 py-2 bg-white border border-green-300 rounded-lg text-sm"
                />
                <button
                  type="button"
                  (click)="copyToClipboard(generatedToken)"
                  class="px-3 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
                >
                  Copier
                </button>
              </div>
              <p class="text-xs text-green-700 mt-2">⚠️ Copiez ce token maintenant, il ne sera plus affiché par la suite.</p>
            </div>
          </form>
        </div>

        <div class="modal-footer bg-gray-50 px-6 py-3 flex justify-between">
          <div>
            <button
              *ngIf="isEditMode"
              type="button"
              (click)="regenerateToken()"
              class="px-4 py-2 bg-orange-600 text-white rounded-lg hover:bg-orange-700 transition-colors"
            >
              🔄 Régénérer le token
            </button>
          </div>

          <div class="flex space-x-2">
            <button
              type="button"
              (click)="activeModal.dismiss()"
              class="px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
            >
              Annuler
            </button>

            <button
              *ngIf="!isEditMode"
              type="button"
              (click)="onSubmit()"
              [disabled]="tokenForm.invalid || isLoading"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {{ isLoading ? 'Création...' : ' Créer le token' }}
            </button>

            <button
              *ngIf="isEditMode"
              type="button"
              (click)="updateToken()"
              [disabled]="isLoading"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {{ isLoading ? 'Mise à jour...' : ' Mettre à jour' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class TokenManagementModalComponent implements OnInit {
  @Input() currentToken?: ITokensApp;
  @Input() applications: any[] = [];
  @Output() tokenCreated = new EventEmitter<ITokensApp>();
  @Output() tokenUpdated = new EventEmitter<ITokensApp>();

  tokenForm: FormGroup;
  isEditMode = false;
  isLoading = false;
  generatedToken = '';
  showCurrentToken = false;
  useCustomDate = false;
  selectedPreset = '6months';
  private notificationService = inject(NotificationService);
  private formBuilder = inject(FormBuilder);
  protected activeModal = inject(NgbActiveModal);
  private tokensAppService = inject(TokensAppService);

  constructor() {
    this.tokenForm = this.formBuilder.group({
      applicationId: [''],
      dateExpiration: [''],
      active: [true],
    });

    // Set default expiration to 6 months
    this.setExpirationPreset('6months');
  }

  ngOnInit() {
    if (this.currentToken) {
      this.isEditMode = true;
      this.tokenForm.patchValue({
        applicationId: this.currentToken.application?.id,
        dateExpiration: this.currentToken.dateExpiration?.format('YYYY-MM-DDTHH:mm'),
        active: this.currentToken.active,
      });
    }
  }

  setExpirationPreset(preset: string) {
    this.selectedPreset = preset;
    this.useCustomDate = false;

    const now = dayjs();
    let expirationDate;

    switch (preset) {
      case '1month':
        expirationDate = now.add(1, 'month');
        break;
      case '3months':
        expirationDate = now.add(3, 'months');
        break;
      case '6months':
        expirationDate = now.add(6, 'months');
        break;
      default:
        expirationDate = now.add(6, 'months');
    }

    this.tokenForm.patchValue({
      dateExpiration: expirationDate.format('YYYY-MM-DDTHH:mm'),
    });
  }

  onSubmit() {
    if (this.tokenForm.valid && !this.isEditMode) {
      this.createToken();
    }
  }

  createToken() {
    this.isLoading = true;

    const tokenRequest: CreateTokenRequest = {
      applicationId: this.tokenForm.value.applicationId,
      dateExpiration: dayjs(this.tokenForm.value.dateExpiration),
      active: true,
    };

    this.tokensAppService.createWithCustomExpiration(tokenRequest).subscribe({
      next: (response: any) => {
        this.generatedToken = response.body.token;
        this.tokenCreated.emit(response.body);
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors de la création du token:', error);
        this.isLoading = false;
        this.notificationService.confirm('Erreur lors de la création du token', 'Erreur', 'danger').subscribe();
      },
    });
  }

  updateToken() {
    if (!this.currentToken) return;

    this.isLoading = true;

    const updateData = {
      ...this.currentToken,
      dateExpiration: dayjs(this.tokenForm.value.dateExpiration),
      active: this.tokenForm.value.active,
    };

    this.tokensAppService.update(updateData).subscribe({
      next: (response: any) => {
        this.tokenUpdated.emit(response.body);
        this.activeModal.close();
        this.isLoading = false;
      },
      error: error => {
        console.error('Erreur lors de la mise à jour:', error);
        this.isLoading = false;
        this.notificationService.confirm('Erreur lors de la mise à jour du token', 'Erreur', 'danger').subscribe();
      },
    });
  }

  regenerateToken() {
    if (!this.currentToken) return;

    this.notificationService
      .confirm("Êtes-vous sûr de vouloir régénérer ce token ? L'ancien token ne fonctionnera plus.", 'Régénérer le token', 'warning')
      .subscribe(confirmed => {
        if (confirmed) {
          this.isLoading = true;

          this.tokensAppService.regenerateToken(this.currentToken!.id).subscribe({
            next: (response: any) => {
              this.generatedToken = response.body.token;
              this.tokenUpdated.emit(response.body);
              this.isLoading = false;
            },
            error: error => {
              console.error('Erreur lors de la régénération:', error);
              this.isLoading = false;
              this.notificationService.confirm('Erreur lors de la régénération du token', 'Erreur', 'danger').subscribe();
            },
          });
        }
      });
  }

  copyToClipboard(text: string) {
    navigator.clipboard
      .writeText(text)
      .then(() => {
        this.notificationService.success('Token copié dans le presse-papiers !').subscribe();
      })
      .catch(() => {
        // Fallback pour les anciens navigateurs
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        this.notificationService.success('Token copié dans le presse-papiers !').subscribe();
      });
  }

  toggleTokenVisibility() {
    this.showCurrentToken = !this.showCurrentToken;
  }
}
