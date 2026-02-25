import { Component, computed, inject, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfigService, PartnerConfigurationDTO, UnifiedConfigurationDTO } from './config.service';
import { ChannelConfigService, ChannelConfigurationDTO } from './channel-config.service';
import { ChannelType } from './channel.types';
import { DatePipe, DecimalPipe, NgClass, NgForOf, NgIf, NgSwitch, NgSwitchCase, PercentPipe } from '@angular/common';
import { ToastComponent } from '../toast/toast.component';
import { AccountService } from '../../core/auth/account.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ChannelConfigComponent } from './channel-config.component';
import { TableModule } from 'primeng/table';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-config',
  templateUrl: './config.component.html',
  standalone: true,
  imports: [
    NgClass,
    ReactiveFormsModule,
    NgIf,
    FormsModule,
    NgForOf,
    NgSwitch,
    NgSwitchCase,
    PercentPipe,
    DecimalPipe,
    ToastComponent,
    DatePipe,
    ChannelConfigComponent,
    TableModule,
  ],
  styleUrls: ['./config.component.scss'],
})
export class ConfigComponent implements OnInit, OnDestroy {
  // Services
  private accountService = inject(AccountService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private configService = inject(ConfigService);
  private channelService = inject(ChannelConfigService);
  private destroy$ = new Subject<void>();

  // Formulaire principale
  configForm!: FormGroup;
  webhookCopied = false;
  testError: string | null = null;
  testLoading = false;
  testSuccess: boolean | null = null;
  verified = false;
  loading = false;
  coexistenceLoading = false;
  isConfigured = false;
  showAdminOptions = false;
  activeTab: ChannelType = 'WHATSAPP';

  // État des configurations
  configurations: UnifiedConfigurationDTO[] = [];
  filteredConfigurations: UnifiedConfigurationDTO[] = [];
  searchTerm: string = '';
  filterChannelType: ChannelType | null = null;

  viewMode: 'LIST' | 'FORM' = 'LIST';
  selectedChannelConfig: ChannelConfigurationDTO | null = null;

  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));

  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  @ViewChild(ChannelConfigComponent) channelConfig!: ChannelConfigComponent;

  @Input() config!: ChannelConfigurationDTO | null;

  ngOnInit(): void {
    this.initForms();
    this.loadConfiguration();
    this.subscribeToConfigurations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ========== INITIALISATION ==========

  private initForms(): void {
    this.configForm = this.fb.group({
      businessId: ['', Validators.required],
      accessToken: ['', Validators.required],
      phoneNumberId: ['', Validators.required],
      appId: ['', Validators.required],
      valid: [false],
      application: [''],
      updatedAt: [null],
      coexistenceEnabled: [false],
    });
  }

  /**
   * S'abonne aux changements des configurations
   */
  private subscribeToConfigurations(): void {
    this.configService.configurations$.pipe(takeUntil(this.destroy$)).subscribe(configs => {
      this.configurations = configs;
      this.applyFilters();
    });
  }

  /**
   * Charge la configuration WhatsApp
   */
  private loadConfiguration(): void {
    this.configService.getMetaConfig().subscribe({
      next: dto => {
        if (dto) {
          this.configForm.patchValue(dto);
          this.verified = !!dto.verified;
          this.isConfigured = true;
        }
      },
      error: () => {
        // pas de config existante
      },
    });
  }

  // ========== GESTION META WHATSAPP ==========

  saveConfig(): void {
    if (this.configForm.invalid) {
      this.markFormTouched();
      return;
    }
    this.loading = true;
    const cfg: PartnerConfigurationDTO = this.configForm.value;
    this.configService.saveMetaConfig(cfg).subscribe({
      next: () => {
        this.loading = false;
        this.isConfigured = true;
        this.toast.showToast('Configuration sauvegardée ', 'success');
      },
      error: () => {
        this.loading = false;
        this.toast.showToast('Erreur de sauvegarde ', 'error');
      },
    });
  }

  testConfig(): void {
    if (this.configForm.invalid) {
      this.markFormTouched();
      return;
    }
    this.testLoading = true;
    this.testError = null;
    const cfg: PartnerConfigurationDTO = this.configForm.value;
    this.configService.testMetaConfig(cfg).subscribe({
      next: () => {
        this.testLoading = false;
        this.testSuccess = true;
      },
      error: (err: HttpErrorResponse) => {
        this.testLoading = false;
        this.testSuccess = false;
        this.testError = err.error?.detail || 'Test échoué';
      },
    });
  }

  enableCoexistence(): void {
    if (this.configForm.invalid) {
      this.markFormTouched();
      return;
    }
    this.coexistenceLoading = true;
    const { phoneNumberId, accessToken } = this.configForm.value;
    this.configService.enableCoexistence(phoneNumberId, accessToken).subscribe({
      next: () => {
        this.coexistenceLoading = false;
        this.configForm.patchValue({ coexistenceEnabled: true });
        this.toast.showToast('Coexistence activée ', 'success');
      },
      error: () => {
        this.coexistenceLoading = false;
        this.toast.showToast('Échec activation Coexistence ', 'error');
      },
    });
  }

  private markFormTouched(): void {
    Object.values(this.configForm.controls).forEach(c => c.markAsTouched());
  }

  onFieldChange(field: string): void {
    this.isConfigured = false;
    this.testSuccess = null;
    this.verified = false;
  }

  get webhookUrl(): string {
    return `${window.location.origin}/api/webhook`;
  }

  copyWebhook(): void {
    const text = this.webhookUrl;
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(
        () => this.onCopySuccess(),
        () => this.onCopyError(),
      );
    } else {
      this.fallbackCopy(text);
    }
  }

  private onCopySuccess(): void {
    this.webhookCopied = true;
    this.toast.showToast('Webhook copié ', 'success');
    setTimeout(() => (this.webhookCopied = false), 3000);
  }

  private onCopyError(): void {
    this.toast.showToast('Échec de la copie', 'error');
  }

  private fallbackCopy(text: string): void {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
      document.execCommand('copy') ? this.onCopySuccess() : this.onCopyError();
    } catch {
      this.onCopyError();
    }
    document.body.removeChild(textarea);
  }

  launchMetaSignup(): void {
    const url = this.buildSignupUrl(['whatsapp_business_messaging', 'whatsapp_business_management'], 'signup');
    this.openSignupPopup(url);
  }

  launchCoexistenceSignup(): void {
    const url = this.buildSignupUrl(
      ['whatsapp_business_messaging', 'whatsapp_business_management', 'whatsapp_business_coexistence'],
      'coexistence',
    );
    this.openSignupPopup(url);
  }

  private buildSignupUrl(scopes: string[], state: string): string {
    const appId = '2645300142336839';
    const redirect = `${window.location.origin}/meta-signup-callback`;
    return [
      `https://www.facebook.com/dialog/oauth`,
      `?client_id=${appId}`,
      `&redirect_uri=${encodeURIComponent(redirect)}`,
      `&scope=${encodeURIComponent(scopes.join(','))}`,
      `&response_type=token`,
      `&state=${encodeURIComponent(state)}`,
    ].join('');
  }

  private openSignupPopup(url: string): void {
    const popup = window.open(url, 'MetaSignup', 'width=600,height=800');
    if (popup) {
      const jwt = sessionStorage.getItem('jhi-authenticationToken')!;
      popup.sessionStorage.setItem('jhi-authenticationToken', jwt);
    } else {
      this.toast.showToast("Impossible d'ouvrir la fenêtre de signup ", 'error');
    }
  }

  // ========== GESTION DES CANAUX (SMS/EMAIL) ==========

  /**
   * Applique les filtres de recherche et type
   */
  applyFilters(): void {
    let filtered = this.configurations;

    // Filtre par searchTerm
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(
        c => c.username?.toLowerCase().includes(term) || c.host?.toLowerCase().includes(term) || c.channel.toLowerCase().includes(term),
      );
    }

    // Filtre par type de channel
    if (this.filterChannelType) {
      filtered = filtered.filter(c => c.channel === this.filterChannelType);
    }

    this.filteredConfigurations = filtered;
  }

  countByChannel(channel: ChannelType): number {
    return this.configurations.filter(c => c.channel === channel).length;
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onTypeFilterChange(): void {
    this.applyFilters();
  }

  // ========== NAVIGATION ONGLETS ==========

  switchTab(tab: ChannelType): void {
    this.activeTab = tab;
  }

  goToConfigAdmin(): void {
    this.router.navigate(['config-admin']);
  }

  togglePanel(): void {
    this.showAdminOptions = !this.showAdminOptions;
  }

  openCreate(channel: ChannelType, cfg?: UnifiedConfigurationDTO): void {
    this.activeTab = channel;
    this.selectedChannelConfig = cfg ? { ...cfg, channelType: channel as 'SMS' | 'EMAIL' } : null;
    this.viewMode = 'FORM';
  }

  // ========== CRUD DES CANAUX ==========

  /**
   * Supprime une configuration avec mise à jour en temps réel
   */
  deleteChannel(cfg: UnifiedConfigurationDTO): void {
    if (!confirm(`Voulez-vous vraiment supprimer la configuration ${cfg.channel} ?`)) {
      return;
    }

    this.channelService.deleteConfig(cfg.id).subscribe({
      next: () => {
        this.toast.showToast(`${cfg.channel} supprimé `, 'success');
        // Mise à jour immédiate du state
        this.configService.removeConfigFromMemory(cfg.id, cfg.channel);
      },
      error: err => {
        const message = typeof err.error === 'string' ? err.error : 'Erreur suppression';
        this.toast.showToast(message, 'error');
      },
    });
  }

  /**
   * Sauvegarde une configuration avec mise à jour en temps réel
   */
  saveChannel(cfg: ChannelConfigurationDTO): void {
    const password = cfg.password || '';

    if (cfg.id) {
      // UPDATE
      this.channelService.updateConfig(cfg.id, cfg, password).subscribe({
        next: updated => {
          this.toast.showToast(`${cfg.channelType} mis à jour `, 'success');
          // Mise à jour immédiate du state
          const unifiedCfg: UnifiedConfigurationDTO = {
            channel: cfg.channelType,
            id: updated.id!,
            username: updated.username,
            host: updated.host,
            port: updated.port,
            extraInfo: cfg.channelType === 'SMS' ? `Opérateur: ${cfg.smsOperator}` : undefined,
          };
          this.configService.updateConfigInMemory(unifiedCfg);
          this.viewMode = 'LIST';
        },
        error: err => {
          const message = typeof err.error === 'string' ? err.error : 'Erreur mise à jour';
          this.toast.showToast(message, 'error');
        },
      });
    } else {
      // CREATE
      this.channelService.saveConfig(cfg, password).subscribe({
        next: created => {
          this.toast.showToast(`${cfg.channelType} configuré `, 'success');
          // Ajout immédiat du state
          const unifiedCfg: UnifiedConfigurationDTO = {
            channel: cfg.channelType,
            id: created.id!,
            username: created.username,
            host: created.host,
            port: created.port,
            extraInfo: cfg.channelType === 'SMS' ? `Opérateur: ${cfg.smsOperator}` : undefined,
          };
          this.configService.updateConfigInMemory(unifiedCfg);
          this.viewMode = 'LIST';
        },
        error: err => {
          const message = typeof err.error === 'string' ? err.error : 'Erreur création';
          this.toast.showToast(message, 'error');
        },
      });
    }
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toast?.showToast(message, type);
  }
  trackById(index: number, item: UnifiedConfigurationDTO) {
    return item.channel + '-' + item.id;
  }
}
