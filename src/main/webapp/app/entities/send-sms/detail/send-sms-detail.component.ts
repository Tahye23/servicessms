import { Component, computed, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { HttpParams } from '@angular/common/http';

import SharedModule from 'app/shared/shared.module';
import { DurationPipe, FormatMediumDatePipe, FormatMediumDatetimePipe } from 'app/shared/date';
import { ISendSms, MessageType, Sms } from '../send-sms.model';
import { BulkProgressResponse, EntityResponseType, SendSmsResponse, SendSmsService } from '../service/send-sms.service';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Ripple } from 'primeng/ripple';
import { ToastComponent } from '../../toast/toast.component';
import { AccountService } from '../../../core/auth/account.service';
import { TemplateRendererComponent } from '../../template/detailMessage/template-renderer.component';
import { IGroupe } from '../../groupe/groupe.model';
import { GroupeService } from '../../groupe/service/groupe.service';
import { SmsListComponent } from '../sms-list/sms-list.component';
import { BulkSmsMonitorComponent } from '../bulk-sms-monitor/bulk-sms-monitor.component';
import { DebugMonitoringComponent } from '../bulk-sms-monitor/debug-monitoring.component';
import { CampaignHistoryComponent } from '../campaign-history/campaign-history.component';
import { ResetFailedSmsButtonComponent } from './reset-failed-sms-button.component';
import { DataService } from 'app/entities/admin-data-delete/data.service';

interface MonitoringStats {
  totalProcessed: number;
  totalSuccess: number;
  totalFailed: number;
  activeThreads: number;
  successRate: number;
  targetRatePerSecond: number;
  circuitBreakerOpen: boolean;
  currentQueueSize: number;
  activeThreadsPool: number;
  lastUpdate: Date;
}
type MetricKey = 'totalSent' | 'totalDelivered' | 'totalRead' | 'totalPending' | 'totalFailed';
interface RealTimeMetrics {
  currentRate: number;
  averageRate: number;
  peakRate: number;
  efficiency: number;
  estimatedCompletion: Date | null;
  errorRate: number;
  retryCount: number;
}

@Component({
  standalone: true,
  selector: 'jhi-send-sms-detail',
  templateUrl: './send-sms-detail.component.html',
  imports: [
    SharedModule,
    Ripple,
    RouterModule,
    ToastModule,
    DurationPipe,
    FormatMediumDatetimePipe,
    FormatMediumDatePipe,
    ToastComponent,
    TemplateRendererComponent,
    SmsListComponent,
    BulkSmsMonitorComponent,
    DebugMonitoringComponent,
    CampaignHistoryComponent,
    ResetFailedSmsButtonComponent,
  ],
  providers: [MessageService],
})
export class SendSmsDetailComponent implements OnDestroy, OnInit {
  sendSmsInput!: ISendSms;
  loading = false;
  showMonitoringModal = false;
  showDebugModal = false;
  showHistoryModal = false;
  showHistory = false;
  isTestMode: boolean = false;
  bulkId = '';
  testGroupModalVisible = false;
  testGroups: IGroupe[] = [];
  selectedTestGroupId: number | null = null;
  sendingTest = false;
  isLoading = false;
  messageError = '';
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  progress = 0;
  totalRecipients = 0;
  isWhatsapp = false;
  inprocess = true;
  isPolling = false;
  elapsedSeconds = 0;
  showSendConfirmation = false;
  showStopConfirmation = false;
  isStopping = false;

  sendConfirmationData = {
    totalRecipients: 0,
    messagePreview: '',
    isTestMode: false,
  };
  alerts: { type: 'success' | 'warning' | 'error' | 'info'; message: string; timestamp: Date }[] = [];
  private monitoringSubscription?: Subscription;
  private dataService = inject(DataService);
  private hasCompleted = false;
  smsList: Sms[] = [];
  isLoadingSms = false;
  private pollingSub?: Subscription;
  pageSize = 10;
  currentPage = 0;
  searchTerm = '';
  totalPages = 0;
  id!: number;

  @ViewChild('toast', { static: true }) toast!: ToastComponent;

  private accountService = inject(AccountService);
  private readonly ALL_KEYS: MetricKey[] = ['totalSent', 'totalDelivered', 'totalRead', 'totalPending', 'totalFailed'];

  private metric = (k: MetricKey): number => this.sendSmsInput?.[k] ?? 0;

  private sum = (keys: MetricKey[]): number => keys.reduce((acc, k) => acc + this.metric(k), 0);

  constructor(
    private route: ActivatedRoute,
    private sendSmsService: SendSmsService,
    private groupeService: GroupeService,
  ) {
    this.route.paramMap.subscribe(params => {
      this.id = Number(params.get('id'));
    });
  }

  ngOnInit(): void {
    if (this.id) {
      this.loading = true;
      this.loadSendSms();
    }
  }

  openSendConfirmation(): void {
    this.sendConfirmationData = {
      totalRecipients: (this.sendSmsInput?.totalPending || 0) + (this.sendSmsInput?.totalFailed || 0),
      messagePreview: this.sendSmsInput?.msgdata?.substring(0, 100) || '',
      isTestMode: this.isTestMode,
    };
    this.showSendConfirmation = true;
  }

  closeSendConfirmation(): void {
    this.showSendConfirmation = false;
  }

  confirmSend(): void {
    this.closeSendConfirmation();
    this.sendSmsToBackend(this.id);
  }

  // === MODALE D'ARRÊT ===
  openStopConfirmation(): void {
    this.showStopConfirmation = true;
  }

  closeStopConfirmation(): void {
    this.showStopConfirmation = false;
  }

  confirmStop(): void {
    this.closeStopConfirmation();
    this.stopBulkSending();
  }

  openMonitoringModal() {
    this.showMonitoringModal = true;
  }

  openDebugModal() {
    this.showDebugModal = true;
  }

  openHistoryModal() {
    this.showHistoryModal = true;
  }

  closeMonitoringModal() {
    this.showMonitoringModal = false;
  }
  handleResetComplete(result: any) {
    this.loadSendSms();
  }
  closeDebugModal() {
    this.showDebugModal = false;
  }

  closeHistoryModal() {
    this.showHistoryModal = false;
  }
  /**
   * Arrête l'envoi en cours
   */
  stopBulkSending(): void {
    if (!this.sendSmsInput?.id || !this.sendSmsInput.inprocess) {
      return;
    }

    this.isStopping = true;

    this.sendSmsService.stopBulkSending(this.sendSmsInput.id).subscribe({
      next: response => {
        this.isStopping = false;

        if (response.success) {
          this.toast.showToast('Envoi arrêté avec succès.', 'success');
          this.loadSendSms();
          this.stopPolling();
        } else {
          this.toast.showToast(response.message, 'error');
        }
      },
      error: error => {
        this.isStopping = false;
        this.toast.showToast("Erreur lors de l'arrêt de l'envoi.", 'error');
        console.error('Erreur arrêt envoi:', error);
      },
    });
  }

  sendSmsToBackend(id: number): void {
    this.loading = true;
    let params = new HttpParams();
    if (this.isTestMode) {
      params = params.set('test', 'true');
    }

    this.sendSmsService.sendSmsM(id, { params }).subscribe({
      next: (res: SendSmsResponse) => {
        this.loading = false;
        if (res.templateExiste) {
          this.bulkId = res.bulkId;
          this.totalRecipients = res.totalRecipients;
          this.onRefresh();
        } else {
          this.toast.showToast('Template non approuvé.', 'warn');
        }
      },
      error: (error: any) => {
        this.loading = false;
        this.messageError = error.error.message + error.error.detail;
        this.toast.showToast("Échec du lancement de l'envoi.", 'error');
      },
    });
  }

  /**
   * Calcule un taux en pourcentage.
   * @param numerators  une ou plusieurs métriques au numérateur
   * @param denominators métriques du dénominateur (défaut: ALL_KEYS)
   * @returns number en pourcentage (0..100)
   */
  private rate(numerators: MetricKey | MetricKey[], denominators: MetricKey[] = this.ALL_KEYS): number {
    const num = Array.isArray(numerators) ? this.sum(numerators) : this.metric(numerators);
    const den = this.sum(denominators);
    return den > 0 ? (num / den) * 100 : 0;
  }

  get successRate(): number {
    return this.rate('totalSent', ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }
  get failedRate(): number {
    return this.rate('totalFailed', ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }
  get pendingRate(): number {
    return this.rate('totalPending', ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }

  get delivredRate(): number {
    return this.rate('totalDelivered', ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }

  get readRate(): number {
    return this.rate('totalRead', ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }

  get readDelivredRate(): number {
    return this.rate(['totalRead', 'totalDelivered'], ['totalSent', 'totalPending', 'totalFailed', 'totalDelivered', 'totalRead']);
  }

  private stopPolling(): void {
    this.isLoading = false;
    this.isPolling = false;
    this.pollingSub?.unsubscribe();
    this.monitoringSubscription?.unsubscribe();
    this.pollingSub = undefined;
    this.monitoringSubscription = undefined;
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  loadSendSms(): void {
    const id = this.id;
    if (id == null) {
      console.warn('Aucun ID de sendSms défini, impossible de charger.');
      return;
    }

    this.sendSmsService.getSendSmsById(id).subscribe({
      next: (smsData: ISendSms) => {
        this.loading = false;
        this.inprocess = smsData.inprocess ?? false;
        this.sendSmsInput = smsData;

        if (this.sendSmsInput.type == MessageType.WHATSAPP) {
          this.isWhatsapp = true;
        }

        this.bulkId = this.sendSmsInput.bulkId ?? '';

        this.loadSmsPage();
      },
      error: (error: any) => {
        this.loading = false;
        this.messageError = 'Erreur lors du chargement du SMS' + error.error.message + error.error.detail;
        console.error('Erreur lors du chargement du SMS', error);
      },
    });
  }
  /**
   * Gestionnaires d'événements du composant de monitoring
   */
  onMonitoringComplete(finalProgress: BulkProgressResponse): void {
    console.log('Monitoring terminé:', finalProgress);
    this.toast.showToast(
      `Envoi terminé avec ${finalProgress.stats.successRate.toFixed(1)}% de succès`,
      finalProgress.stats.successRate > 80 ? 'success' : 'warn',
    );
    this.loadSendSms();
  }

  onMonitoringError(error: string): void {
    console.error('Erreur monitoring:', error);
    this.toast.showToast('Erreur lors du suivi de progression', 'error');
  }

  onProgressUpdate(progress: BulkProgressResponse): void {
    console.log('Progression:', `${progress.stats.sent}/${progress.totalRecipients}`);
  }

  onRefresh(): void {
    this.dataService.updateSendSmsStatus(this.sendSmsInput.id).subscribe({
      next: () => this.toast.showToast('Mise à jour des totaux réussie.', 'success'),
      error: () => this.toast.showToast('Erreur lors de la mise à jour.', 'success'),
    });
  }

  loadSmsPage(): void {
    const id = this.sendSmsInput.id;
    if (!id) return;

    this.isLoadingSms = true;
    /* this.sendSmsService.getSmsByBulkId(id, this.currentPage, this.pageSize, this.searchTerm).subscribe({
      next: (page: PageSms) => {
        this.smsList = page.content;
        this.totalPages = page.totalPages;
        this.isLoadingSms = false;
      },
      error: (error: any) => {
        this.isLoadingSms = false;
        console.error('Erreur lors du chargement des SMS', error);
      },
    });*/
  }

  openTestGroupModal() {
    this.testGroupModalVisible = true;
    this.selectedTestGroupId = null;
    this.loadTestGroups();
  }

  closeTestGroupModal() {
    this.testGroupModalVisible = false;
  }

  loadTestGroups() {
    this.groupeService.query({ groupType: 'test' }).subscribe({
      next: res => {
        this.testGroups = res.body ?? [];
      },
      error: err => {
        console.error('Erreur chargement groupes test', err);
        this.testGroups = [];
      },
    });
  }

  sendToTestGroup() {
    if (!this.selectedTestGroupId) return;
    this.sendingTest = true;

    this.sendSmsService.sendToGroupTest(this.sendSmsInput?.id!, this.selectedTestGroupId).subscribe({
      next: () => {
        this.sendingTest = false;
        this.closeTestGroupModal();
        alert('Message envoyé au groupe test avec succès');
      },
      error: err => {
        this.sendingTest = false;
        console.error('Erreur envoi au groupe test', err);
        alert("Erreur lors de l'envoi au groupe test");
      },
    });
  }

  parseReactionCounters(rc?: string): { [key: string]: number } {
    try {
      return rc ? JSON.parse(rc) : {};
    } catch {
      return {};
    }
  }

  sortByValueDesc = (a: any, b: any) => b.value - a.value;

  totalVotes(): number {
    const rc = this.sendSmsInput.reactionCounters;
    try {
      const obj = rc ? JSON.parse(rc) : {};
      return Object.values(obj).reduce((acc: number, val: any) => acc + Number(val), 0);
    } catch {
      return 0;
    }
  }

  getDeliveryLabel(): string {
    const status = this.sendSmsInput.deliveryStatus?.toLowerCase();
    switch (status) {
      case 'read':
        return 'Lu';
      case 'delivered':
        return 'Distribué';
      case 'sent':
        return 'Envoyé';
      case 'pending':
        return 'En attente';
      case 'failed':
        return 'Échec';
      default:
        return 'Inconnu';
    }
  }

  previousState(): void {
    window.history.back();
  }

  refreshBatch() {
    const id = this.sendSmsInput.id;
    this.loading = true;
    this.sendSmsService.refresh(this.id).subscribe({
      next: batch => {
        this.loading = false;
        this.sendSmsInput = batch;
        this.toast.showToast('Rafraîchissement réussi.', 'success');
      },
      error: err => {
        this.toast.showToast('Erreur rafraîchissement : ' + err.message, 'error');
        this.loading = false;
      },
    });
  }

  openInNewTab(entityType: 'groupe' | 'contact', entityId: number | null): void {
    const token = sessionStorage.getItem('jhi-authenticationToken');
    if (token && entityId) {
      const url = `/${entityType}/${entityId}/view`;
      const newWindow = window.open(url, '_blank');
      if (newWindow) {
        newWindow.addEventListener('load', () => {
          try {
            newWindow.sessionStorage.setItem('authenticationToken', token);
          } catch (error) {
            console.error("Impossible d'écrire dans le sessionStorage du nouvel onglet", error);
          }
        });
      }
    }
  }

  protected readonly Math = Math;
  protected readonly Object = Object;
}
