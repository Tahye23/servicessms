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

  whatsappForm!: FormGroup;
  smsForm!: FormGroup;
  emailForm!: FormGroup;

  loading = false;
  testResult: string | null = null;

  constructor(
    private fb: FormBuilder,
    private service: ChannelConfigService,
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadConfigForChannel();
  }

  // -----------------------------
  // INIT
  // -----------------------------
  private initForms(): void {
    this.whatsappForm = this.fb.group({
      host: [''],
      port: [''],
      username: [''],
      password: [''],
    });

    this.smsForm = this.fb.group({
      smsOperator: ['Mattel', Validators.required],
      host: ['', Validators.required],
      port: [0, Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
    });

    this.emailForm = this.fb.group({
      host: ['', Validators.required],
      port: [587, Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  private loadConfigForChannel(): void {
    // Optionnel : chargement backend si nécessaire
  }

  // -----------------------------
  // HELPERS
  // -----------------------------
  private getFormByChannel(): FormGroup | null {
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

  // -----------------------------
  // SAVE
  // -----------------------------
  saveCurrentTab(): void {
    const form = this.getFormByChannel();
    if (!form || form.invalid) {
      return;
    }

    const password = form.get('password')?.value;

    const cfg: ChannelConfigurationDTO = {
      ...form.value,
      channelType: this.channel,
    };

    this.loading = true;

    this.service.saveConfig(cfg, password).subscribe({
      next: () => {
        this.loading = false;
        this.toast.showToast(`${this.channel} configuré ✅`, 'success');
      },
      error: err => {
        this.loading = false;
        this.toast.showToast(`Erreur de sauvegarde : ${err.error || err.message}`, 'error');
      },
    });
  }

  // -----------------------------
  // TEST
  // -----------------------------
  testCurrentTab(): void {
    const form = this.getFormByChannel();
    if (!form || form.invalid) {
      return;
    }

    const password = form.get('password')?.value;

    const cfg: ChannelConfigurationDTO = {
      ...form.value,
      channelType: this.channel,
    };

    this.loading = true;

    this.service.testConfig(cfg, password).subscribe({
      next: res => {
        this.loading = false;
        this.testResult = res;
        this.toast.showToast(`${this.channel} testé avec succès ✅`, 'success');
      },
      error: err => {
        this.loading = false;
        this.testResult = `Erreur : ${err.error || err.message}`;
        this.toast.showToast(`${this.channel} test échoué ❌`, 'error');
      },
    });
  }
}
