import { Component, Input, Output, EventEmitter, OnInit, TemplateRef, ViewChild, ViewContainerRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

// CDK Overlay
import { Overlay, OverlayModule, OverlayRef } from '@angular/cdk/overlay';
import { PortalModule, TemplatePortal } from '@angular/cdk/portal';

interface ResetStatistics {
  total: number;
  alreadyPending: number;
  failed: number;
  sent: number;
  toReset: number;
}

interface ResetResponse {
  success: boolean;
  resetCount: number;
  message: string;
  statsBefore: ResetStatistics;
  statsAfter: ResetStatistics;
  error?: string;
}

@Component({
  standalone: true,
  selector: 'app-reset-failed-sms-button',
  imports: [CommonModule, OverlayModule, PortalModule],
  styles: [
    `
      .reset-overlay-backdrop {
        background: rgba(107, 114, 128, 0.75);
      }
      .reset-overlay-panel {
        background: #fff;
        border-radius: 1rem;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
        max-width: 32rem;
        width: 100%;
        overflow: hidden;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
          transform: translateY(-10px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      .animate-fadeIn {
        animation: fadeIn 0.3s ease-out;
      }
    `,
  ],
  template: `
    <button
      (click)="checkResetNeeded()"
      [disabled]="loading || isProcessing"
      class="inline-flex items-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
    >
      <svg *ngIf="!loading" class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="2"
          d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
        />
      </svg>
      <div *ngIf="loading" class="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent mr-2"></div>
      {{ buttonLabel }}
    </button>

    <div *ngIf="showSuccessMessage" class="mt-3 p-3 bg-green-50 rounded-lg border border-green-200 animate-fadeIn">
      <div class="flex items-center">
        <svg class="w-5 h-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span class="text-sm text-green-800">SMS réinitialisés avec succès</span>
      </div>
    </div>

    <ng-template #confirmTpl>
      <div class="reset-overlay-panel" role="dialog" aria-modal="true" aria-label="Réinitialiser les SMS échoués">
        <!-- En-tête -->
        <div class="px-6 pt-6 pb-4 bg-gradient-to-r from-indigo-50 to-purple-50">
          <div class="flex items-center">
            <div class="flex items-center justify-center flex-shrink-0 w-12 h-12 mx-auto bg-indigo-100 rounded-full sm:mx-0">
              <svg class="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                />
              </svg>
            </div>
            <div class="ml-4">
              <h3 class="text-lg font-semibold text-gray-900">Réinitialiser les SMS échoués</h3>
            </div>
          </div>
        </div>

        <!-- Contenu -->
        <div class="px-6 py-4">
          <!-- Alerte Erreur -->
          <div *ngIf="error" class="mb-4 p-3 bg-red-50 rounded-lg border border-red-200">
            <div class="flex items-start">
              <svg class="w-5 h-5 text-red-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <span class="text-sm text-red-800">{{ error }}</span>
            </div>
          </div>

          <!-- Statistiques -->
          <div *ngIf="statistics" class="mb-4">
            <p class="mb-4 text-sm text-gray-600">
              Cette action remettra en état <b>PENDING</b> tous les SMS échoués pour permettre un nouvel envoi.
            </p>

            <div class="p-4 space-y-3 bg-gray-50 rounded-xl">
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-700">Total de SMS</span>
                <span class="font-semibold text-gray-900">{{ statistics.total }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-700">Déjà en attente</span>
                <span class="font-semibold text-green-600">{{ statistics.alreadyPending }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-700">Échoués</span>
                <span class="font-semibold text-red-600">{{ statistics.failed }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-700">Envoyés avec succès</span>
                <span class="font-semibold text-blue-600">{{ statistics.sent }}</span>
              </div>
              <div class="pt-3 mt-3 border-t border-gray-200">
                <div class="flex justify-between items-center">
                  <span class="text-sm font-medium text-gray-900">À réinitialiser</span>
                  <span class="text-lg font-bold text-indigo-600">{{ statistics.toReset }}</span>
                </div>
              </div>
            </div>

            <!-- Aucun à réinitialiser -->
            <div *ngIf="statistics.toReset === 0" class="mt-4 p-3 bg-green-50 rounded-lg border border-green-200">
              <div class="flex items-center">
                <svg class="w-5 h-5 text-green-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span class="text-sm text-green-800">Aucun SMS à réinitialiser</span>
              </div>
            </div>
          </div>

          <!-- Résultat -->
          <div *ngIf="resetResult && !error" class="p-4 bg-green-50 rounded-lg border border-green-200">
            <div class="flex items-start">
              <svg class="w-5 h-5 text-green-500 mr-2 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div class="flex-1">
                <p class="text-sm font-medium text-green-800">{{ resetResult.message }}</p>
                <p class="text-xs text-green-700 mt-1">{{ resetResult.resetCount }} SMS remis en PENDING</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="px-6 py-4 bg-gray-50 sm:flex sm:flex-row-reverse gap-3">
          <button
            *ngIf="!resetResult"
            (click)="confirmReset()"
            [disabled]="resetting || (statistics && statistics.toReset === 0)"
            class="inline-flex justify-center w-full px-4 py-2 text-base font-medium text-white bg-indigo-600 border border-transparent rounded-lg shadow-sm hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:w-auto sm:text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <div *ngIf="resetting" class="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent mr-2"></div>
            {{ resetting ? 'Réinitialisation...' : 'Confirmer' }}
          </button>

          <button
            (click)="closeModal()"
            class="inline-flex justify-center w-full px-4 py-2 mt-3 text-base font-medium text-gray-700 bg-white border border-gray-300 rounded-lg shadow-sm hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm"
          >
            {{ resetResult ? 'Fermer' : 'Annuler' }}
          </button>
        </div>
      </div>
    </ng-template>
  `,
})
export class ResetFailedSmsButtonComponent implements OnInit {
  @Input() sendSmsId!: number | undefined;
  @Input() buttonLabel = 'Réinitialiser les SMS échoués';
  @Input() isProcessing = false; // Pour désactiver pendant un envoi
  @Output() onResetComplete = new EventEmitter<ResetResponse>();

  @ViewChild('confirmTpl') confirmTpl!: TemplateRef<any>;

  loading = false;
  resetting = false;
  showSuccessMessage = false;
  error: string | null = null;
  statistics: ResetStatistics | null = null;
  resetResult: ResetResponse | null = null;

  private overlayRef?: OverlayRef;

  constructor(
    private http: HttpClient,
    private overlay: Overlay,
    private vcr: ViewContainerRef,
  ) {}

  ngOnInit(): void {}

  private openOverlay(): void {
    if (this.overlayRef?.hasAttached()) return;

    this.overlayRef = this.overlay.create({
      hasBackdrop: true,
      backdropClass: 'reset-overlay-backdrop',
      panelClass: 'z-[100000]',
      positionStrategy: this.overlay.position().global().centerHorizontally().centerVertically(),
      scrollStrategy: this.overlay.scrollStrategies.block(),
    });

    const portal = new TemplatePortal(this.confirmTpl, this.vcr);
    this.overlayRef.attach(portal);

    this.overlayRef.backdropClick().subscribe(() => this.cancelReset());
    this.overlayRef.keydownEvents().subscribe(ev => {
      if ((ev as KeyboardEvent).key === 'Escape') this.cancelReset();
    });
  }

  private closeOverlay(): void {
    if (this.overlayRef?.hasAttached()) {
      this.overlayRef.detach();
    }
  }

  async checkResetNeeded(): Promise<void> {
    if (!this.sendSmsId) {
      this.error = 'ID de campagne manquant';
      return;
    }

    this.loading = true;
    this.error = null;

    try {
      const response = await firstValueFrom(
        this.http.get<{ resetNeeded: boolean; statistics: ResetStatistics }>(`/api/send-sms/${this.sendSmsId}/reset-needed`),
      );

      this.statistics = response.statistics;
      // Ouvre la modale dans tous les cas pour montrer stats / message
      this.openOverlay();
    } catch (err: any) {
      this.error = err?.error?.message || 'Erreur lors de la vérification';
      console.error('Erreur checkResetNeeded:', err);
      // Ouvre quand même pour afficher l’erreur
      this.openOverlay();
    } finally {
      this.loading = false;
    }
  }

  async confirmReset(): Promise<void> {
    if (!this.statistics || this.statistics.toReset === 0) return;

    this.resetting = true;
    this.error = null;

    try {
      const response = await firstValueFrom(this.http.post<ResetResponse>(`/api/send-sms/${this.sendSmsId}/reset-failed`, {}));

      this.resetResult = response;

      if (response.success) {
        this.onResetComplete.emit(response);
        // Ferme la modale et affiche succès inline
        this.closeOverlay();
        this.showSuccessMessage = true;
        setTimeout(() => (this.showSuccessMessage = false), 5000);
      } else {
        this.error = response.error || 'Échec de la réinitialisation';
      }
    } catch (err: any) {
      this.error = err?.error?.message || 'Erreur lors de la réinitialisation';
      console.error('Erreur confirmReset:', err);
    } finally {
      this.resetting = false;
    }
  }

  closeModal(): void {
    this.resetResult = null;
    this.error = null;
    this.statistics = null;
    this.closeOverlay();
  }

  cancelReset(): void {
    if (!this.resetting) this.closeModal();
  }
}
