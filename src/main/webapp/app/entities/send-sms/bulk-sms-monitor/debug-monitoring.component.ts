import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject, timer, BehaviorSubject } from 'rxjs';
import { takeUntil, switchMap, catchError } from 'rxjs/operators';
import { SendSmsService } from '../service/send-sms.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';

interface DebugStats {
  sent: number;
  failed: number;
  pending: number;
  delivered: number;
  read: number;
  total: number;
  successRate: number;
  currentRate: number;
}

interface DebugData {
  bulkId: string;
  totalRecipients: number;
  inProcess: boolean;
  stats: DebugStats;
  error?: string;
  lastUpdate: string;
  sendSmsId: number;
}

@Component({
  selector: 'app-debug-monitoring',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bg-gradient-to-r from-yellow-50 to-orange-50 border border-yellow-200 rounded-2xl p-6 shadow-lg">
      <!-- En-tête -->
      <div class="flex items-center justify-between mb-6">
        <div class="flex items-center">
          <div class="w-10 h-10 bg-yellow-500 rounded-lg flex items-center justify-center mr-3">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <div>
            <h3 class="text-lg font-bold text-gray-800">Debug Monitoring</h3>
            <p class="text-sm text-gray-600">SendSms ID: {{ sendSmsId }}</p>
          </div>
        </div>

        <div class="flex items-center space-x-2">
          <div class="w-3 h-3 rounded-full" [ngClass]="isMonitoring ? 'bg-green-500 animate-pulse' : 'bg-red-500'"></div>
          <span class="text-sm font-medium" [ngClass]="isMonitoring ? 'text-green-700' : 'text-red-700'">
            {{ isMonitoring ? 'En cours' : 'Arrêté' }}
          </span>
        </div>
      </div>

      <!-- Contrôles -->
      <div class="bg-white rounded-xl p-4 mb-6 shadow-sm">
        <div class="flex flex-wrap gap-3 mb-4">
          <button
            (click)="testSingle()"
            [disabled]="isLoading"
            class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            <svg *ngIf="!isLoading" class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            <div *ngIf="isLoading" class="w-4 h-4 mr-2 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
            Test unique
          </button>

          <button
            (click)="startMonitoring()"
            [disabled]="isMonitoring || isLoading"
            class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M14.828 14.828a4 4 0 01-5.656 0M9 10h1m4 0h1m-6 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            Démarrer
          </button>

          <button
            (click)="stopMonitoring()"
            [disabled]="!isMonitoring"
            class="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 10h6v4H9z" />
            </svg>
            Arrêter
          </button>

          <button
            (click)="clearLogs()"
            class="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors flex items-center"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
            Vider logs
          </button>

          <button
            (click)="exportLogs()"
            [disabled]="logs.length === 0"
            class="px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
          >
            <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            Exporter
          </button>
        </div>

        <!-- Configuration -->
        <div class="flex items-center space-x-4 text-sm">
          <div class="flex items-center">
            <label class="text-gray-600 mr-2">Intervalle:</label>
            <select
              [(ngModel)]="pollingInterval"
              (change)="onIntervalChange()"
              [disabled]="isMonitoring"
              class="border border-gray-300 rounded px-2 py-1 disabled:opacity-50"
            >
              <option value="1000">1s</option>
              <option value="2000">2s</option>
              <option value="5000">5s</option>
              <option value="10000">10s</option>
            </select>
          </div>

          <div class="flex items-center">
            <input type="checkbox" id="autoStop" [(ngModel)]="autoStopOnComplete" class="mr-2" />
            <label for="autoStop" class="text-gray-600">Arrêt automatique</label>
          </div>
        </div>
      </div>

      <!-- Statistiques en temps réel -->
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <div class="bg-white rounded-xl p-4 text-center shadow-sm border-l-4 border-blue-500">
          <div class="text-2xl font-bold text-blue-600">{{ currentData?.stats?.sent || 0 }}</div>
          <div class="text-sm text-gray-600">Envoyés</div>
          <div class="text-xs text-gray-500 mt-1">{{ getPercentage(currentData?.stats?.sent || 0) }}%</div>
        </div>

        <div class="bg-white rounded-xl p-4 text-center shadow-sm border-l-4 border-green-500">
          <div class="text-2xl font-bold text-green-600">{{ currentData?.stats?.delivered || 0 }}</div>
          <div class="text-sm text-gray-600">Délivrés</div>
          <div class="text-xs text-gray-500 mt-1">{{ getPercentage(currentData?.stats?.delivered || 0) }}%</div>
        </div>

        <div class="bg-white rounded-xl p-4 text-center shadow-sm border-l-4 border-red-500">
          <div class="text-2xl font-bold text-red-600">{{ currentData?.stats?.failed || 0 }}</div>
          <div class="text-sm text-gray-600">Échoués</div>
          <div class="text-xs text-gray-500 mt-1">{{ getPercentage(currentData?.stats?.failed || 0) }}%</div>
        </div>

        <div class="bg-white rounded-xl p-4 text-center shadow-sm border-l-4 border-yellow-500">
          <div class="text-2xl font-bold text-yellow-600">{{ currentData?.stats?.pending || 0 }}</div>
          <div class="text-sm text-gray-600">En attente</div>
          <div class="text-xs text-gray-500 mt-1">{{ getPercentage(currentData?.stats?.pending || 0) }}%</div>
        </div>

        <div class="bg-white rounded-xl p-4 text-center shadow-sm border-l-4 border-purple-500">
          <div class="text-2xl font-bold text-purple-600">{{ formatRate(currentData?.stats?.currentRate || 0) }}</div>
          <div class="text-sm text-gray-600">Débit</div>
          <div class="text-xs text-gray-500 mt-1">msg/sec</div>
        </div>
      </div>

      <!-- Informations détaillées -->
      <div *ngIf="currentData" class="bg-white rounded-xl p-4 mb-6 shadow-sm">
        <h4 class="font-bold text-gray-800 mb-3 flex items-center">
          <svg class="w-5 h-5 mr-2 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          Détails de la campagne
        </h4>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          <div class="space-y-2">
            <div class="flex justify-between">
              <span class="text-gray-600">Bulk ID:</span>
              <span class="font-mono text-gray-800">{{ currentData.bulkId || 'N/A' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">Total destinataires:</span>
              <span class="font-semibold text-gray-800">{{ currentData.totalRecipients }}</span>
            </div>
          </div>

          <div class="space-y-2">
            <div class="flex justify-between">
              <span class="text-gray-600">Statut:</span>
              <span
                class="px-2 py-1 rounded-full text-xs font-semibold"
                [ngClass]="currentData.inProcess ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'"
              >
                {{ currentData.inProcess ? 'En cours' : 'Terminé' }}
              </span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">Taux de réussite:</span>
              <span class="font-semibold" [ngClass]="getSuccessRateClass()">
                {{ formatPercentage(currentData.stats.successRate || 0) }}%
              </span>
            </div>
          </div>

          <div class="space-y-2">
            <div class="flex justify-between">
              <span class="text-gray-600">Dernière MAJ:</span>
              <span class="font-mono text-gray-800">{{ lastUpdate || 'Jamais' }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gray-600">Appels API:</span>
              <span class="font-semibold text-gray-800">{{ apiCallCount }}</span>
            </div>
          </div>
        </div>

        <!-- Barre de progression -->
        <div class="mt-4">
          <div class="flex justify-between text-sm text-gray-600 mb-2">
            <span>Progression globale</span>
            <span>{{ getGlobalProgress() }}%</span>
          </div>
          <div class="w-full bg-gray-200 rounded-full h-3">
            <div
              class="bg-gradient-to-r from-blue-500 to-green-500 h-3 rounded-full transition-all duration-500"
              [style.width.%]="getGlobalProgress()"
            ></div>
          </div>
        </div>

        <!-- Erreur -->
        <div *ngIf="currentData.error" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <div class="flex items-start">
            <svg class="w-5 h-5 text-red-500 mt-0.5 mr-2 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <div>
              <div class="font-semibold text-red-800">Erreur détectée</div>
              <div class="text-red-700 text-sm mt-1">{{ currentData.error }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Console de logs -->
      <div class="bg-gray-900 rounded-xl overflow-hidden shadow-sm">
        <div class="bg-gray-800 px-4 py-3 flex items-center justify-between">
          <div class="flex items-center">
            <svg class="w-5 h-5 text-gray-400 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <span class="text-gray-300 font-medium">Console de debug</span>
            <span class="ml-2 px-2 py-1 bg-gray-700 text-gray-300 text-xs rounded-full">{{ logs.length }}</span>
          </div>

          <div class="flex items-center space-x-2">
            <button
              (click)="toggleAutoScroll()"
              class="text-xs px-2 py-1 rounded transition-colors"
              [ngClass]="autoScroll ? 'bg-green-600 text-white' : 'bg-gray-700 text-gray-300'"
            >
              Auto-scroll
            </button>
            <button
              (click)="toggleLevelFilter('ERROR')"
              class="text-xs px-2 py-1 rounded transition-colors"
              [ngClass]="levelFilters.ERROR ? 'bg-red-600 text-white' : 'bg-gray-700 text-gray-300'"
            >
              Erreurs
            </button>
            <button
              (click)="toggleLevelFilter('SUCCESS')"
              class="text-xs px-2 py-1 rounded transition-colors"
              [ngClass]="levelFilters.SUCCESS ? 'bg-green-600 text-white' : 'bg-gray-700 text-gray-300'"
            >
              Succès
            </button>
          </div>
        </div>

        <div #logContainer class="p-4 max-h-64 overflow-y-auto bg-gray-900">
          <div *ngFor="let log of getFilteredLogs()" class="text-sm font-mono mb-1 leading-relaxed" [ngClass]="getLogClass(log)">
            {{ log }}
          </div>
          <div *ngIf="getFilteredLogs().length === 0" class="text-gray-500 text-center py-8">
            {{ logs.length === 0 ? 'Aucun log' : 'Aucun log correspondant aux filtres' }}
          </div>
        </div>
      </div>
    </div>
  `,
})
export class DebugMonitoringComponent implements OnInit, OnDestroy {
  @Input() sendSmsId!: number;

  isMonitoring = false;
  isLoading = false;
  lastUpdate: string | null = null;
  currentData: DebugData | null = null;
  logs: string[] = [];
  apiCallCount = 0;
  pollingInterval = 2000;
  autoStopOnComplete = true;
  autoScroll = true;
  levelFilters: { [key: string]: boolean } = {
    ERROR: true,
    SUCCESS: true,
    INFO: true,
    DEBUG: true,
  };

  private destroy$ = new Subject<void>();
  private monitoringSubscription?: any;

  constructor(private sendSmsService: SendSmsService) {}

  ngOnInit(): void {
    this.addLog('INIT', `Composant initialisé avec SendSms ID: ${this.sendSmsId}`);
  }

  ngOnDestroy(): void {
    this.stopMonitoring();
    this.destroy$.next();
    this.destroy$.complete();
  }

  testSingle(): void {
    this.isLoading = true;
    this.addLog('TEST', 'Test appel unique...');

    this.sendSmsService
      .getBulkProgressBySendSmsId(this.sendSmsId)
      .pipe(
        catchError(error => {
          this.addLog('ERROR', `Erreur test: ${error.status} - ${error.message}`);
          return of(null);
        }),
      )
      .subscribe({
        next: data => {
          this.isLoading = false;
          this.apiCallCount++;

          if (data) {
            this.addLog('SUCCESS', `Test réussi: sent=${data.stats?.sent}, total=${data.totalRecipients}`);
            this.updateCurrentData(data);
          }
        },
        error: error => {
          this.isLoading = false;
          this.addLog('ERROR', `Erreur test: ${error.message}`);
        },
      });
  }

  startMonitoring(): void {
    if (this.isMonitoring) return;

    this.addLog('START', `Démarrage monitoring (intervalle: ${this.pollingInterval}ms)`);
    this.isMonitoring = true;

    this.monitoringSubscription = timer(0, this.pollingInterval)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => {
          this.addLog('POLL', `Polling... (appel #${this.apiCallCount + 1})`);
          return this.sendSmsService.getBulkProgressBySendSmsId(this.sendSmsId).pipe(
            catchError(error => {
              this.addLog('ERROR', `Erreur polling: ${error.message}`);
              return of(null);
            }),
          );
        }),
      )
      .subscribe({
        next: data => {
          this.apiCallCount++;

          if (data) {
            this.addLog('DATA', `Données reçues: sent=${data.stats?.sent}, failed=${data.stats?.failed}, inProcess=${data.inProcess}`);
            this.updateCurrentData(data);

            // Arrêter si terminé et auto-stop activé
            if (this.autoStopOnComplete && !data.inProcess) {
              this.addLog('COMPLETE', 'Processus terminé - arrêt automatique');
              this.stopMonitoring();
            }
          }
        },
      });
  }

  stopMonitoring(): void {
    if (!this.isMonitoring) return;

    this.addLog('STOP', 'Arrêt du monitoring');
    this.isMonitoring = false;

    if (this.monitoringSubscription) {
      this.monitoringSubscription.unsubscribe();
      this.monitoringSubscription = null;
    }
  }

  clearLogs(): void {
    this.logs = [];
    this.addLog('CLEAR', 'Logs vidés');
  }

  exportLogs(): void {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const filename = `debug-logs-${this.sendSmsId}-${timestamp}.txt`;

    const content = this.logs.join('\n');
    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();

    window.URL.revokeObjectURL(url);
    this.addLog('EXPORT', `Logs exportés: ${filename}`);
  }

  onIntervalChange(): void {
    if (this.isMonitoring) {
      this.addLog('CONFIG', `Changement intervalle: ${this.pollingInterval}ms (redémarrage requis)`);
    }
  }

  toggleAutoScroll(): void {
    this.autoScroll = !this.autoScroll;
    this.addLog('CONFIG', `Auto-scroll: ${this.autoScroll ? 'activé' : 'désactivé'}`);
  }

  toggleLevelFilter(level: string): void {
    this.levelFilters[level] = !this.levelFilters[level];
  }

  getFilteredLogs(): string[] {
    return this.logs.filter(log => {
      if (log.includes('[ERROR]') && !this.levelFilters.ERROR) return false;
      if (log.includes('[SUCCESS]') && !this.levelFilters.SUCCESS) return false;
      if (log.includes('[INFO]') && !this.levelFilters.INFO) return false;
      if (log.includes('[DEBUG]') && !this.levelFilters.DEBUG) return false;
      return true;
    });
  }

  getLogClass(log: string): string {
    if (log.includes('[ERROR]')) return 'text-red-400';
    if (log.includes('[SUCCESS]')) return 'text-green-400';
    if (log.includes('[WARNING]')) return 'text-yellow-400';
    if (log.includes('[DEBUG]')) return 'text-blue-400';
    return 'text-gray-300';
  }

  getPercentage(value: number): string {
    if (!this.currentData?.totalRecipients) return '0';
    return ((value / this.currentData.totalRecipients) * 100).toFixed(1);
  }

  getGlobalProgress(): number {
    if (!this.currentData?.stats || !this.currentData?.totalRecipients) return 0;
    const processed = (this.currentData.stats.sent || 0) + (this.currentData.stats.failed || 0);
    return Math.round((processed / this.currentData.totalRecipients) * 100);
  }

  getSuccessRateClass(): string {
    const rate = this.currentData?.stats?.successRate || 0;
    if (rate >= 80) return 'text-green-600';
    if (rate >= 60) return 'text-yellow-600';
    return 'text-red-600';
  }

  formatRate(rate: number): string {
    return rate.toFixed(1);
  }

  formatPercentage(value: number): string {
    return value.toFixed(1);
  }

  private updateCurrentData(data: any): void {
    this.currentData = {
      ...data,
      lastUpdate: new Date().toISOString(),
      sendSmsId: this.sendSmsId,
    };
    this.lastUpdate = new Date().toLocaleTimeString();
  }

  private addLog(type: string, message: string): void {
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = `[${timestamp}] [${type}] ${message}`;

    this.logs.unshift(logEntry);

    if (this.logs.length > 100) {
      this.logs = this.logs.slice(0, 100);
    }

    console.log(`[DEBUG-MONITORING] ${logEntry}`);
  }
}
