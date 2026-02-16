// config.component.ts
import { Component, computed, inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ConfigService, PartnerConfigurationDTO } from './config.service';
import { ChannelConfigService, ChannelConfigurationDTO } from './channel-config.service';
import { ChannelType } from './channel.types';
import { DatePipe, DecimalPipe, NgClass, NgForOf, NgIf, NgSwitch, NgSwitchCase, PercentPipe } from '@angular/common';
import { ToastComponent } from '../toast/toast.component';
import { AccountService } from '../../core/auth/account.service';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { ChannelConfigComponent } from './channel-config.component';
import { TableModule } from 'primeng/table';

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
  readonly FB: any;

  // Liste et filtres
  configs: any[] = [];
  channels: ChannelConfigurationDTO[] = [];
  filteredChannels: ChannelConfigurationDTO[] = [];
  searchTerm: string = '';
  filterChannelType: ChannelType | null = null;
  page: number = 1;
  itemsPerPage: number = 10;
  viewMode: 'LIST' | 'FORM' = 'LIST';

  private popupListener = this.handleSignupSuccess.bind(this);

  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));

  @ViewChild('toast', { static: true }) toast!: ToastComponent;

  private accountService = inject(AccountService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private configService = inject(ConfigService);
  private channelService = inject(ChannelConfigService);

  ngOnInit(): void {
    this.initForms();
    this.loadConfiguration();
    this.loadAllConfigs();
    window.addEventListener('message', this.popupListener, false);
  }

  ngOnDestroy(): void {
    window.removeEventListener('message', this.popupListener, false);
  }

  // INITIALISATION FORMULAIRES
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

  // CHARGEMENT CONFIGURATION
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

  loadAllConfigs() {
    this.configService.getAllConfigurations().subscribe(res => (this.configs = res));
  }

  // SAUVEGARDE
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
        this.toast.showToast('Configuration sauvegardée ✅', 'success');
      },
      error: () => {
        this.loading = false;
        this.toast.showToast('Erreur de sauvegarde ❌', 'error');
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

  // TEST CONFIGURATION
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

  // COEXISTENCE
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
        this.toast.showToast('Coexistence activée ✅', 'success');
      },
      error: () => {
        this.coexistenceLoading = false;
        this.toast.showToast('Échec activation Coexistence ❌', 'error');
      },
    });
  }

  // WEBHOOK
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

  private onCopySuccess() {
    this.webhookCopied = true;
    this.toast.showToast('Webhook copié 🔗', 'success');
    setTimeout(() => (this.webhookCopied = false), 3000);
  }

  private onCopyError() {
    this.toast.showToast('Échec de la copie', 'error');
  }

  private fallbackCopy(text: string) {
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

  // META SIGNUP
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
      this.toast.showToast('Impossible d’ouvrir la fenêtre de signup ❌', 'error');
    }
  }

  private handleSignupSuccess(event: MessageEvent) {
    if (event.origin !== window.location.origin) return;
    if (event.data?.type === 'META_SIGNUP_SUCCESS') {
      this.loadConfiguration();
      this.toast.showToast('WhatsApp Business configuré avec succès ✅', 'success');
    }
  }
  paginatedChannels(): any[] {
    let filtered = this.channels;

    // Filtre par searchTerm
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => c.username?.toLowerCase().includes(term) || c.host?.toLowerCase().includes(term));
    }

    // Filtre par type de channel
    if (this.filterChannelType) {
      filtered = filtered.filter(c => c.channelType === this.filterChannelType);
    }

    // Pagination
    const start = (this.page - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return filtered.slice(start, end);
  }

  // ADMIN
  goToConfigAdmin() {
    this.router.navigate(['config-admin']);
  }
  togglePanel(): void {
    this.showAdminOptions = !this.showAdminOptions;
  }

  // FILTRES CANAUX
  applyFilters() {
    this.filteredChannels = this.channels.filter(c => {
      const matchesSearch = this.searchTerm
        ? c.username?.toLowerCase().includes(this.searchTerm.toLowerCase()) || c.host?.toLowerCase().includes(this.searchTerm.toLowerCase())
        : true;
      const matchesType = this.filterChannelType ? c.channelType === this.filterChannelType : true;
      return matchesSearch && matchesType;
    });
  }

  onSearchChange() {
    this.page = 1;
    this.applyFilters();
  }
  onTypeFilterChange() {
    this.page = 1;
    this.applyFilters();
  }

  // ONGLET / FORMULAIRE CANAUX
  switchTab(tab: ChannelType) {
    this.activeTab = tab;
  }
  openCreate(channel: ChannelType) {
    this.activeTab = channel;
    this.viewMode = 'FORM';
  }
}
