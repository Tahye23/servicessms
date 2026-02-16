// meta-config.component.ts
import { Component, computed, inject, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';

import { DecimalPipe, NgClass, NgForOf, NgIf, NgSwitch, NgSwitchCase, PercentPipe } from '@angular/common';
import { ToastComponent } from '../toast/toast.component';
import { AccountService } from '../../core/auth/account.service';
import { ConfigService } from '../config/config.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-config-admin',
  templateUrl: './config-admin.component.html',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, NgIf, FormsModule, NgForOf, NgSwitch, NgSwitchCase, PercentPipe, DecimalPipe, ToastComponent],
})
export class ConfigAdminComponent implements OnInit {
  configForm!: FormGroup;
  persistenceForm!: FormGroup;
  threadingForm!: FormGroup;
  monitoringForm!: FormGroup;
  codeSecret = '';
  accessGranted = false;
  private router = inject(Router);
  readonly correctSecretCode = 'Richatt2025!';
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  private accountService = inject(AccountService);
  // États UI
  loading = false;

  // Données de performance
  cpuUsageData = [30, 45, 60, 75, 65, 80, 55];
  memoryUsageData = [40, 50, 65, 70, 60, 75, 80];
  messagesSentToday = 1245;
  messagesSentTrend = 12;
  deliveryRate = 0.98;
  readRate = 0.85;

  // Options de configuration
  persistenceOptions = [
    {
      value: 'jpa',
      label: 'JPA (Base relationnelle)',
      description: 'Persistance transactionnelle avec cache de second niveau',
    },
    {
      value: 'jdbc',
      label: 'JDBC Direct',
      description: 'Accès direct SQL pour haute performance',
    },
  ];
  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  constructor(
    private fb: FormBuilder,
    private configService: ConfigService,
  ) {}

  ngOnInit(): void {
    if (!this.isAdmin) {
      this.router.navigate(['404']);
    }
    this.initForms();
    // this.loadConfig();
  }

  get cpuAverageUsage(): number {
    return Math.round(this.cpuUsageData.reduce((a, b) => a + b, 0) / this.cpuUsageData.length);
  }

  get memoryAverageUsage(): number {
    return Math.round(this.memoryUsageData.reduce((a, b) => a + b, 0) / this.memoryUsageData.length);
  }
  verifySecretCode() {
    if (this.codeSecret === this.correctSecretCode) {
      this.accessGranted = true;
    } else {
      alert('Code secret incorrect.');
    }
  }

  private initForms(): void {
    // Configuration principale
    this.configForm = this.fb.group({
      WHATSAPP_API_URL_envoi: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      ACCESS_TOKEN: ['', Validators.required],
      WHATSAPP_API_URL_Templates: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
    });

    // Configuration de persistance
    this.persistenceForm = this.fb.group({
      persistenceType: ['jpa', Validators.required],
      // Options JPA
      connectionPoolSize: [10, [Validators.min(5), Validators.max(100)]],
      jpaCache: ['read-only'],
      batchStrategy: ['batch-50'],
      // Options JDBC
      jdbcBatchSize: [100, [Validators.min(10), Validators.max(1000)]],
      queryTimeout: [30, [Validators.min(5), Validators.max(120)]],
      preparedStatementMode: ['pooled'],
      // Options MongoDB
      mongoConnectionPoolSize: [20, [Validators.min(5), Validators.max(100)]],
      mongoConnectionTimeout: [5000, [Validators.min(1000), Validators.max(30000)]],
      mongoBulkMode: ['unordered'],
    });

    // Configuration des threads
    this.threadingForm = this.fb.group({
      sendingThreads: [5, [Validators.min(1), Validators.max(20)]],
      webhookThreads: [3, [Validators.min(1), Validators.max(10)]],
      queueSize: ['500'],
      executionStrategy: ['fixed'],
    });

    // Configuration monitoring
    this.monitoringForm = this.fb.group({
      logLevel: ['info'],
      logRetention: [7],
      enableMetrics: [true],
      enableHealthChecks: [true],
      maxCpuUsage: [85],
      maxMemoryUsage: [80],
      throttleOnHighUsage: [true],
      alertOnHighUsage: [true],
      pauseOnCritical: [false],
    });
  }
}
