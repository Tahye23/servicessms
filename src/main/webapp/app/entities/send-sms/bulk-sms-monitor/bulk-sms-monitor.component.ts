// bulk-sms-monitor.component.ts
import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, OnChanges, SimpleChanges } from '@angular/core';
import { Subject, timer, EMPTY, interval } from 'rxjs';
import { takeUntil, switchMap, expand, catchError, distinctUntilChanged, startWith } from 'rxjs/operators';
import { SendSmsService, BulkProgressResponse, RealtimeMetrics } from '../service/send-sms.service';
import { DatePipe, DecimalPipe, NgClass, NgForOf, NgIf } from '@angular/common';

export interface MonitoringAlert {
  type: 'success' | 'warning' | 'error' | 'info';
  message: string;
  timestamp: Date;
}

@Component({
  selector: 'app-bulk-sms-monitor',
  templateUrl: './bulk-sms-monitor.component.html',
  styleUrls: ['./bulk-sms-monitor.component.scss'],
  standalone: true,
  imports: [DecimalPipe, DatePipe, NgClass, NgIf, NgForOf],
})
export class BulkSmsMonitorComponent implements OnInit, OnDestroy, OnChanges {
  @Input() sendSmsId!: number;
  @Input() bulkId?: string | null | undefined;
  @Input() autoStart = true;
  @Input() isWhatsapp = false;
  @Input() showDetailsByDefault = false;

  @Output() monitoringComplete = new EventEmitter<BulkProgressResponse>();
  @Output() monitoringError = new EventEmitter<string>();
  @Output() progressUpdate = new EventEmitter<BulkProgressResponse>();

  isMonitoring = false;
  showDetailedView = false;
  progressData: BulkProgressResponse | null = null;
  realtimeMetrics: RealtimeMetrics | null = null;
  alerts: MonitoringAlert[] = [];
  errorMessage: string | null = null;

  private progressHistory: BulkProgressResponse[] = [];
  private readonly maxHistorySize = 120; // 1 minute à 500ms
  private readonly destroy$ = new Subject<void>();
  private monitoringSubscription?: any;

  constructor(private sendSmsService: SendSmsService) {}

  ngOnInit(): void {
    // Test de connectivité d'abord
    this.sendSmsService.testConnectivity().subscribe(connected => {
      if (!connected) {
        console.error("Problème de connectivité avec l'API");
        return;
      }

      this.sendSmsService.debugMonitoring(this.sendSmsId).subscribe(
        data => console.log('Debug data:', data),
        error => console.error('Debug error:', error),
      );

      if (this.autoStart && this.sendSmsId) {
        this.startMonitoring();
      }
    });
  }

  startMonitoring(): void {
    if (!this.sendSmsId || this.isMonitoring) return;

    console.log(' [COMPONENT] Démarrage monitoring pour:', this.sendSmsId);

    this.isMonitoring = true;

    this.monitoringSubscription = this.sendSmsService
      .getBulkProgressStream(this.sendSmsId, 'sendSmsId')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: progress => {
          console.log(' [COMPONENT] Progression reçue:', progress);
          this.handleProgressUpdate(progress);
        },
        error: error => {
          console.error(' [COMPONENT] Erreur monitoring:', error);
          this.handleError(error);
        },
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sendSmsId'] && this.sendSmsId && this.autoStart) {
      this.startMonitoring();
    }
  }

  /**
   * Arrête le monitoring
   */
  stopMonitoring(): void {
    this.isMonitoring = false;
    this.monitoringSubscription?.unsubscribe();
    this.monitoringSubscription = null;
  }

  /**
   * Force une actualisation des données
   */
  refreshData(): void {
    if (!this.sendSmsId) return;

    this.sendSmsService.getBulkProgressBySendSmsId(this.sendSmsId).subscribe({
      next: progress => this.handleProgressUpdate(progress),
      error: error => this.handleError(error),
    });
  }

  /**
   * Gère les mises à jour de progression
   */
  private handleProgressUpdate(progress: BulkProgressResponse): void {
    if (progress.error) {
      this.handleError(new Error(progress.error));
      return;
    }

    const previousProgress = this.progressData;
    this.progressData = progress;

    this.updateHistory(progress);
    this.updateMetrics();
    this.checkAlerts(progress, previousProgress);

    this.progressUpdate.emit(progress);

    // Vérifier si terminé
    if (this.sendSmsService.isProcessComplete(progress)) {
      this.completeMonitoring();
    }
  }

  /**
   * Met à jour l'historique
   */
  private updateHistory(progress: BulkProgressResponse): void {
    this.progressHistory.push(progress);

    if (this.progressHistory.length > this.maxHistorySize) {
      this.progressHistory = this.progressHistory.slice(-this.maxHistorySize);
    }
  }

  /**
   * Met à jour les métriques temps réel
   */
  private updateMetrics(): void {
    if (!this.progressData || this.progressHistory.length === 0) return;

    this.realtimeMetrics = this.sendSmsService.calculateRealtimeMetrics(this.progressData, this.progressHistory);
  }

  /**
   * Vérifie les alertes
   */
  private checkAlerts(current: BulkProgressResponse, previous?: BulkProgressResponse | null): void {
    // Alerte débit faible
    if (current.currentRate < 3 && current.currentRate > 0 && current.inProcess) {
      this.addAlert('warning', `Débit faible détecté: ${current.currentRate.toFixed(1)} msg/sec`);
    }

    // Alerte taux d'erreur
    if (this.realtimeMetrics && this.realtimeMetrics.errorRate > 25) {
      this.addAlert('error', `Taux d'erreur élevé: ${this.realtimeMetrics.errorRate.toFixed(1)}%`);
    }

    // Alerte fin d'insertion
    if (current.insertionComplete && (!previous || !previous.insertionComplete)) {
      this.addAlert('info', "Phase d'insertion terminée, envoi des messages en cours...");
    }

    // Alerte progression bloquée
    if (this.isProgressStuck(current)) {
      this.addAlert('warning', 'Progression semble bloquée depuis plus de 30 secondes');
    }
  }

  /**
   * Détecte si la progression est bloquée
   */
  private isProgressStuck(progress: BulkProgressResponse): boolean {
    if (this.progressHistory.length < 20) return false; // 10 secondes

    const recent = this.progressHistory.slice(-20);
    const firstSent = recent[0].stats.sent;
    const lastSent = recent[recent.length - 1].stats.sent;

    return lastSent === firstSent && progress.inProcess && progress.currentRate < 0.1;
  }

  private handleError(error: any): void {
    console.error('Erreur monitoring:', error);
    this.errorMessage = error.message || 'Erreur inconnue';
    this.addAlert('error', `Erreur: ${this.errorMessage}`);
    // @ts-ignore
    this.monitoringError.emit(this.errorMessage);
    this.stopMonitoring();
  }

  private completeMonitoring(): void {
    this.stopMonitoring();

    if (this.progressData) {
      const stats = this.progressData.stats;
      this.addAlert('success', `Envoi terminé: ${stats.sent} messages, ${stats.successRate.toFixed(1)}% de succès`);
      this.monitoringComplete.emit(this.progressData);
    }
  }

  private addAlert(type: MonitoringAlert['type'], message: string): void {
    const isDuplicate = this.alerts.some(
      alert => alert.message === message && Date.now() - alert.timestamp.getTime() < 10000, // 10 secondes
    );

    if (!isDuplicate) {
      this.alerts.unshift({ type, message, timestamp: new Date() });

      if (this.alerts.length > 5) {
        this.alerts = this.alerts.slice(0, 5);
      }
    }
  }

  toggleDetailedView(): void {
    this.showDetailedView = !this.showDetailedView;
  }

  clearAlerts(): void {
    this.alerts = [];
  }

  get isInserting(): boolean {
    return this.progressData ? !this.progressData.insertionComplete : false;
  }
  get currentPhase(): string {
    if (!this.progressData) return 'Initialisation...';
    return this.isInserting ? 'Insertion en base' : 'Envoi des messages';
  }

  get progressPercentage(): number {
    if (!this.progressData) return 0;
    return this.isInserting ? this.progressData.insertionProgress : this.progressData.sendProgress;
  }

  get estimatedTimeRemaining(): number {
    if (!this.progressData) return -1;
    return this.isInserting ? this.progressData.etaInsertSeconds : this.progressData.etaSendSeconds;
  }

  get systemHealthStatus(): 'excellent' | 'good' | 'warning' | 'critical' {
    if (!this.realtimeMetrics) return 'good';

    const errorRate = this.realtimeMetrics.errorRate;
    const efficiency = this.realtimeMetrics.efficiency;

    if (errorRate > 30 || efficiency < 30) return 'critical';
    if (errorRate > 15 || efficiency < 60) return 'warning';
    if (errorRate < 5 && efficiency > 90) return 'excellent';
    return 'good';
  }

  ngOnDestroy(): void {
    this.stopMonitoring();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
