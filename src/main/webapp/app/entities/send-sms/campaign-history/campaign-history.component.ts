import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

export interface CampaignHistoryData {
  campaignId: number;
  retryCount: number;
  lastRetryDate: string;
  firstAttemptDate: string;
  totalMessages: number;
  sentCount: number;
  failedCount: number;
  pendingCount: number;
  totalSuccess: number;
  totalFailed: number;
  totalPending: number;
}

export interface RetryAttempt {
  retryCount: number;
  attemptDate: string;
  totalSuccess: number;
  totalFailed: number;
  lastError?: string;
  startTime?: string;
  endTime?: string;
  durationSeconds?: number;
  completionStatus?: 'COMPLETED' | 'STOPPED_BY_USER' | 'ERROR' | 'IN_PROGRESS';
}

@Component({
  standalone: true,
  selector: 'app-campaign-history',
  imports: [CommonModule],
  template: `
    <div class="space-y-6" *ngIf="historyData">
      <!-- Résumé général -->
      <div class="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-6 border border-blue-100">
        <h3 class="text-lg font-semibold text-blue-800 mb-4 flex items-center">
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
          Résumé de la campagne
        </h3>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="bg-white rounded-xl p-4 text-center border border-blue-100">
            <div class="text-2xl font-bold text-blue-700">{{ historyData.retryCount }}</div>
            <div class="text-sm text-gray-600">Tentatives</div>
          </div>

          <div class="bg-white rounded-xl p-4 text-center border border-green-100">
            <div class="text-2xl font-bold text-green-700">{{ historyData.sentCount }}</div>
            <div class="text-sm text-gray-600">Envoyés</div>
          </div>

          <div class="bg-white rounded-xl p-4 text-center border border-red-100">
            <div class="text-2xl font-bold text-red-700">{{ historyData.failedCount }}</div>
            <div class="text-sm text-gray-600">Échoués</div>
          </div>

          <div class="bg-white rounded-xl p-4 text-center border border-yellow-100">
            <div class="text-2xl font-bold text-yellow-700">{{ historyData.pendingCount }}</div>
            <div class="text-sm text-gray-600">En attente</div>
          </div>
        </div>

        <!-- Barre de progression -->
        <div class="mt-4">
          <div class="flex justify-between text-sm text-gray-600 mb-2">
            <span>Progression</span>
            <span>{{ getSuccessRate() }}% réussite</span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-3">
            <div
              class="bg-gradient-to-r from-green-400 to-green-600 h-3 rounded-full transition-all duration-500"
              [style.width.%]="getSuccessRate()"
            ></div>
          </div>
        </div>
        <div class="mt-4 grid grid-cols-2 md:grid-cols-3 gap-3" *ngIf="getTotalDuration() > 0">
          <div class="bg-white rounded-lg p-3 border border-indigo-100">
            <div class="text-xs text-gray-600 mb-1">Durée totale</div>
            <div class="text-lg font-semibold text-indigo-700">{{ formatDuration(getTotalDuration()) }}</div>
          </div>
          <div class="bg-white rounded-lg p-3 border border-indigo-100">
            <div class="text-xs text-gray-600 mb-1">Durée moyenne</div>
            <div class="text-lg font-semibold text-indigo-700">{{ formatDuration(getAverageDuration()) }}</div>
          </div>
          <div class="bg-white rounded-lg p-3 border border-indigo-100">
            <div class="text-xs text-gray-600 mb-1">Débit moyen</div>
            <div class="text-lg font-semibold text-indigo-700">{{ getAverageRate() }} SMS/s</div>
          </div>
        </div>
      </div>

      <!-- Historique détaillé des tentatives -->
      <div class="bg-white rounded-2xl border border-gray-100 overflow-hidden" *ngIf="retryHistory && retryHistory.length > 0">
        <div class="bg-purple-50 px-6 py-4 border-b border-purple-100">
          <h4 class="font-semibold text-purple-800 flex items-center">
            <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Historique détaillé ({{ retryHistory.length }} tentatives)
          </h4>
        </div>

        <div class="divide-y divide-gray-100">
          <div *ngFor="let attempt of retryHistory; let i = index" class="p-6 hover:bg-gray-50 transition-colors">
            <!-- En-tête de la tentative -->
            <div class="flex items-start justify-between mb-4">
              <div class="flex items-start space-x-4">
                <div
                  class="w-10 h-10 rounded-full flex items-center justify-center text-sm font-semibold"
                  [ngClass]="getAttemptBadgeClass(attempt)"
                >
                  #{{ attempt.retryCount }}
                </div>

                <div>
                  <div class="flex items-center space-x-3">
                    <span class="font-medium text-gray-900"> Tentative {{ attempt.retryCount }} </span>
                    <span
                      class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                      [ngClass]="getStatusBadgeClass(attempt.completionStatus)"
                    >
                      {{ getStatusLabel(attempt.completionStatus) }}
                    </span>
                  </div>

                  <div class="text-sm text-gray-600 mt-1">
                    {{ formatDate(attempt.attemptDate) }}
                  </div>
                </div>
              </div>

              <div class="text-right">
                <div class="text-sm font-medium text-gray-900">{{ getAttemptSuccessRate(attempt) }}%</div>
                <div class="text-xs text-gray-500">taux de réussite</div>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4" *ngIf="attempt.startTime">
              <div class="bg-gray-50 rounded-lg p-3">
                <div class="flex items-center space-x-2 text-xs text-gray-600 mb-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>Début</span>
                </div>
                <div class="text-sm font-medium text-gray-900">
                  {{ formatTime(attempt.startTime) }}
                </div>
              </div>

              <div class="bg-gray-50 rounded-lg p-3" *ngIf="attempt.endTime">
                <div class="flex items-center space-x-2 text-xs text-gray-600 mb-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                  <span>Fin</span>
                </div>
                <div class="text-sm font-medium text-gray-900">
                  {{ formatTime(attempt.endTime) }}
                </div>
              </div>

              <div class="bg-indigo-50 rounded-lg p-3" *ngIf="attempt.durationSeconds">
                <div class="flex items-center space-x-2 text-xs text-indigo-600 mb-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                  <span>Durée</span>
                </div>
                <div class="text-sm font-medium text-indigo-900">
                  {{ formatDuration(attempt.durationSeconds) }}
                </div>
              </div>

              <div class="bg-green-50 rounded-lg p-3" *ngIf="attempt.durationSeconds && attempt.totalSuccess">
                <div class="flex items-center space-x-2 text-xs text-green-600 mb-1">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                  </svg>
                  <span>Débit</span>
                </div>
                <div class="text-sm font-medium text-green-900">{{ calculateRate(attempt) }} SMS/s</div>
              </div>
            </div>

            <div class="mb-4" *ngIf="attempt.completionStatus === 'IN_PROGRESS'">
              <div class="flex items-center space-x-2 text-sm text-blue-600">
                <div class="animate-spin rounded-full h-4 w-4 border-2 border-blue-600 border-t-transparent"></div>
                <span>Envoi en cours...</span>
              </div>
              <div class="mt-2 w-full bg-blue-100 rounded-full h-2">
                <div class="bg-blue-600 h-2 rounded-full animate-pulse" style="width: 60%"></div>
              </div>
            </div>

            <!-- Statistiques de la tentative -->
            <div class="flex flex-wrap gap-4 text-sm mb-3">
              <span class="inline-flex items-center text-green-600">
                <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path
                    fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clip-rule="evenodd"
                  />
                </svg>
                {{ attempt.totalSuccess }} réussis
              </span>
              <span class="inline-flex items-center text-red-600">
                <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                  <path
                    fill-rule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clip-rule="evenodd"
                  />
                </svg>
                {{ attempt.totalFailed }} échoués
              </span>
            </div>

            <!-- Message d'erreur -->
            <div *ngIf="attempt.lastError" class="mt-3">
              <div class="flex items-start space-x-2 p-3 rounded-lg" [ngClass]="getErrorBoxClass(attempt.completionStatus)">
                <svg class="w-5 h-5 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
                </svg>
                <div class="flex-1">
                  <p class="text-sm font-medium">{{ getErrorTitle(attempt.completionStatus) }}</p>
                  <p class="text-sm mt-1">{{ attempt.lastError }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Message si pas d'historique -->
      <div *ngIf="!retryHistory || retryHistory.length === 0" class="bg-gray-50 rounded-2xl p-8 text-center border border-gray-100">
        <svg class="w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
          />
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">Aucun historique détaillé</h3>
        <p class="text-gray-600">L'historique détaillé des tentatives n'est pas encore disponible.</p>
      </div>
    </div>

    <!-- Chargement -->
    <div *ngIf="loading" class="flex items-center justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      <span class="ml-3 text-gray-600">Chargement de l'historique...</span>
    </div>

    <!-- Erreur -->
    <div *ngIf="error" class="bg-red-50 border border-red-200 rounded-lg p-4">
      <div class="flex">
        <svg class="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="ml-3">
          <h3 class="text-sm font-medium text-red-800">Erreur de chargement</h3>
          <p class="text-sm text-red-700 mt-1">{{ error }}</p>
        </div>
      </div>
    </div>
  `,
})
export class CampaignHistoryComponent implements OnInit {
  @Input() campaignId!: number;

  historyData: CampaignHistoryData | null = null;
  retryHistory: RetryAttempt[] = [];
  loading = false;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.loadHistory();
  }

  loadHistory() {
    if (!this.campaignId) {
      this.error = 'ID de campagne requis';
      console.error('[HISTORY] ID manquant');
      return;
    }

    console.log('[HISTORY] Chargement pour campagne', this.campaignId);
    this.loading = true;
    this.error = null;

    this.http.get<CampaignHistoryData>(`/api/campaigns/${this.campaignId}/history`).subscribe({
      next: data => {
        console.log('[HISTORY]  Données reçues:', data);
        this.historyData = data;
        this.loading = false;
        this.loadRetryHistory();
      },
      error: err => {
        console.error('[HISTORY]  Erreur:', err);
        console.error('[HISTORY] Status:', err.status);
        console.error('[HISTORY] Message:', err.message);

        this.error = `Impossible de charger l'historique: ${err.status} ${err.statusText}`;
        this.loading = false;
      },
    });
  }

  loadRetryHistory() {
    console.log('[RETRY-HISTORY] Chargement pour campagne', this.campaignId);

    this.http.get<RetryAttempt[]>(`/api/campaigns/${this.campaignId}/retry-history`).subscribe({
      next: data => {
        console.log('[RETRY-HISTORY]  Données reçues:', data);
        this.retryHistory = data;
      },
      error: err => {
        console.warn('[RETRY-HISTORY] ⚠ Erreur:', err);
        this.http.get<RetryAttempt[]>(`/api/send-sms/${this.campaignId}/retry-history`).subscribe({
          next: data => {
            console.log('[RETRY-HISTORY]  Données reçues (fallback):', data);
            this.retryHistory = data;
          },
          error: err2 => {
            console.warn('[RETRY-HISTORY] Historique détaillé non disponible');
          },
        });
      },
    });
  }

  getSuccessRate(): number {
    if (!this.historyData || this.historyData.totalMessages === 0) return 0;
    return Math.round((this.historyData.sentCount / this.historyData.totalMessages) * 100);
  }

  getAttemptBadgeClass(attempt: RetryAttempt): string {
    const successRate = this.getAttemptSuccessRate(attempt);
    if (successRate >= 80) return 'bg-green-100 text-green-800';
    if (successRate >= 50) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  }

  getAttemptSuccessRate(attempt: RetryAttempt): number {
    const total = attempt.totalSuccess + attempt.totalFailed;
    if (total === 0) return 0;
    return Math.round((attempt.totalSuccess / total) * 100);
  }

  getStatusLabel(status?: string): string {
    const labels: Record<string, string> = {
      COMPLETED: 'Terminé',
      STOPPED_BY_USER: 'Arrêté',
      ERROR: 'Erreur',
      IN_PROGRESS: 'En cours',
    };
    return labels[status || ''] || 'Inconnu';
  }

  getStatusBadgeClass(status?: string): string {
    const classes: Record<string, string> = {
      COMPLETED: 'bg-green-100 text-green-800',
      STOPPED_BY_USER: 'bg-orange-100 text-orange-800',
      ERROR: 'bg-red-100 text-red-800',
      IN_PROGRESS: 'bg-blue-100 text-blue-800',
    };
    return classes[status || ''] || 'bg-gray-100 text-gray-800';
  }

  getErrorBoxClass(status?: string): string {
    if (status === 'STOPPED_BY_USER') return 'bg-orange-50 text-orange-800 border border-orange-200';
    return 'bg-red-50 text-red-800 border border-red-200';
  }

  getErrorTitle(status?: string): string {
    if (status === 'STOPPED_BY_USER') return 'Arrêt manuel';
    return 'Erreur rencontrée';
  }

  formatDuration(seconds?: number): string {
    if (!seconds || seconds === 0) return 'N/A';

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) return `${hours}h ${minutes}m ${secs}s`;
    if (minutes > 0) return `${minutes}m ${secs}s`;
    return `${secs}s`;
  }

  formatTime(dateString?: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  }

  calculateRate(attempt: RetryAttempt): string {
    if (!attempt.durationSeconds || attempt.durationSeconds === 0) return 'N/A';
    const rate = attempt.totalSuccess / attempt.durationSeconds;
    return rate.toFixed(1);
  }

  getTotalDuration(): number {
    return this.retryHistory.filter(a => a.durationSeconds).reduce((sum, a) => sum + (a.durationSeconds || 0), 0);
  }

  getAverageDuration(): number {
    const completedAttempts = this.retryHistory.filter(a => a.durationSeconds);
    if (completedAttempts.length === 0) return 0;
    return Math.round(this.getTotalDuration() / completedAttempts.length);
  }

  getAverageRate(): string {
    const completedAttempts = this.retryHistory.filter(a => a.durationSeconds);
    if (completedAttempts.length === 0) return 'N/A';

    const totalSuccess = completedAttempts.reduce((sum, a) => sum + a.totalSuccess, 0);
    const totalDuration = completedAttempts.reduce((sum, a) => sum + (a.durationSeconds || 0), 0);

    if (totalDuration === 0) return 'N/A';
    return (totalSuccess / totalDuration).toFixed(1);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  }
}
