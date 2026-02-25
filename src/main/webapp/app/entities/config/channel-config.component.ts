import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ChannelConfigService, ChannelConfigurationDTO } from './channel-config.service';
import { ToastComponent } from '../toast/toast.component';
import { NgIf } from '@angular/common';
import { ChannelType } from './channel.types';

@Component({
  selector: 'app-channel-config',
  templateUrl: './channel-config.component.html',
  styleUrls: ['./channel-config.component.scss'],
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, NgIf, ToastComponent],
})
export class ChannelConfigComponent implements OnInit {
  @Input() channel!: ChannelType;
  @Input() toast!: ToastComponent;

  /**
   * Quand une config est passée (mode édition), on pré-remplit le formulaire.
   * Le setter est appelé AVANT ou APRÈS ngOnInit selon l'ordre Angular,
   * donc on stocke la valeur et on patch dans ngOnInit si besoin.
   */
  @Input() set config(cfg: ChannelConfigurationDTO | null) {
    this._config = cfg;
    // Si le formulaire est déjà initialisé (Input arrive après ngOnInit)
    if (cfg && this.smsForm && this.emailForm) {
      this.patchForm(cfg);
    }
  }

  private _config: ChannelConfigurationDTO | null = null;

  whatsappForm!: FormGroup;
  smsForm!: FormGroup;
  emailForm!: FormGroup;

  loading = false;
  testResult: string | null = null;

  duplicateOperatorError: string | null = null;

  constructor(
    private fb: FormBuilder,
    private service: ChannelConfigService,
  ) {}

  ngOnInit(): void {
    this.initForms();

    // Si config déjà reçue avant ngOnInit (cas @Input set avant ngOnInit)
    if (this._config) {
      this.patchForm(this._config);
    }
  }

  // -----------------------------
  // INIT
  // -----------------------------
  private initForms(): void {
    this.whatsappForm = this.fb.group({
      id: [null],
      host: [''],
      port: [''],
      username: [''],
      password: [''],
    });

    this.smsForm = this.fb.group({
      id: [null], // ← CRUCIAL pour détecter update
      smsOperator: ['Mattel', Validators.required],
      host: ['', Validators.required],
      port: [0, Validators.required],
      username: ['', Validators.required],
      password: [''], // pas obligatoire en update
    });

    this.emailForm = this.fb.group({
      id: [null], // ← CRUCIAL pour détecter update
      host: ['', Validators.required],
      port: [587, Validators.required],
      username: ['', Validators.required],
      password: [''], // pas obligatoire en update
    });
  }

  /**
   * Pré-remplit le formulaire du bon channel avec les données existantes.
   */
  private patchForm(cfg: ChannelConfigurationDTO): void {
    const form = this.getFormByChannel();
    if (!form) return;

    form.patchValue({
      id: cfg.id ?? null,
      host: cfg.host ?? '',
      port: cfg.port ?? '',
      username: cfg.username ?? '',
      smsOperator: cfg.smsOperator ?? 'Mattel',
      password: '', // Ne jamais pré-remplir le mot de passe pour la sécurité
    });
  }

  // -----------------------------
  // HELPERS
  // -----------------------------
  public getFormByChannel(): FormGroup | null {
    switch (this.channel) {
      case 'WHATSAPP':
        return this.whatsappForm;
      case 'SMS':
        return this.smsForm;
      case 'EMAIL':
        return this.emailForm;
      default:
        return null;
    }
  }

  get isEditMode(): boolean {
    const form = this.getFormByChannel();
    return !!form?.get('id')?.value;
  }

  // -----------------------------
  // SAVE ou UPDATE
  // -----------------------------
  saveCurrentTab(): void {
    const form = this.getFormByChannel();
    if (!form) return;

    // En mode update, le password n'est pas obligatoire
    if (!this.isEditMode && form.invalid) {
      form.markAllAsTouched();
      return;
    }

    const formValue = form.value;
    const cfg: ChannelConfigurationDTO = {
      ...formValue,
      channelType: this.channel,
      verified: true,
    };
    const password = formValue.password || '';
    this.loading = true;

    if (cfg.id) {
      // ── UPDATE ──
      this.service.updateConfig(cfg.id, cfg, password).subscribe({
        next: updated => {
          this.loading = false;
          // Mettre à jour le formulaire avec les données retournées (id conservé)
          form.patchValue({ id: updated.id });
          this.toast.showToast(`${this.channel} mis à jour `, 'success');
        },
        error: err => {
          this.loading = false;
          this.toast.showToast(typeof err.error === 'string' ? err.error : 'Erreur mise à jour', 'error');
        },
      });
    } else {
      // ── CREATE ──
      this.service.saveConfig(cfg, password).subscribe({
        next: created => {
          this.loading = false;
          // Stocker l'id retourné pour les futures modifications
          form.patchValue({ id: created.id });
          this.toast.showToast(`${this.channel} configuré `, 'success');
        },
        error: err => {
          this.loading = false;
          const message = typeof err.error === 'string' ? err.error : 'Erreur création';
          // Afficher l'erreur doublon dans le champ opérateur si SMS
          if (this.channel === 'SMS' && message.includes('opérateur')) {
            this.duplicateOperatorError = message;
          }
          this.toast.showToast(message, 'error');
        },
      });
    }
  }

  // -----------------------------
  // SUPPRIMER
  // -----------------------------
  deleteCurrentTab(): void {
    const form = this.getFormByChannel();
    if (!form || !form.value.id) return;

    if (!confirm(`Voulez-vous vraiment supprimer la configuration ${this.channel} ?`)) return;

    this.loading = true;
    this.service.deleteConfig(form.value.id).subscribe({
      next: () => {
        this.loading = false;
        this.toast.showToast(`${this.channel} supprimé `, 'success');
        form.reset();
        // Remettre les valeurs par défaut après reset
        if (this.channel === 'SMS') {
          form.patchValue({ smsOperator: 'Mattel', port: 0 });
        } else if (this.channel === 'EMAIL') {
          form.patchValue({ port: 587 });
        }
      },
      error: err => {
        this.loading = false;
        this.toast.showToast(typeof err.error === 'string' ? err.error : 'Erreur suppression', 'error');
      },
    });
  }

  // -----------------------------
  // TEST
  // -----------------------------
  testCurrentTab(): void {
    const form = this.getFormByChannel();
    if (!form || form.invalid) {
      form?.markAllAsTouched();
      return;
    }

    const password = form.get('password')?.value;
    const cfg: ChannelConfigurationDTO = {
      ...form.value,
      channelType: this.channel,
      verified: true,
    };

    this.loading = true;

    this.service.testConfig(cfg, password).subscribe({
      next: (res: string) => {
        this.loading = false;
        this.testResult = res;
        this.toast.showToast(`${this.channel} testé avec succès `, 'success');
      },
      error: err => {
        this.loading = false;
        const message = typeof err.error === 'string' ? err.error : 'Erreur test';
        this.testResult = message;
        this.toast.showToast(message, 'error');
      },
    });
  }
}
