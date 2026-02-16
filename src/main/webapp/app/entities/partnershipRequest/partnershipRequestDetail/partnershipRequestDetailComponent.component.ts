import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { IPartnershipRequest, RequestStatus } from '../partnership-request.model';
import { PartnershipRequestService } from '../service/partnershipRequestService.service';

// Interfaces et services

// Interface pour les notes admin
interface AdminNote {
  id: number;
  note: string;
  adminName: string;
  createdDate: Date;
  action: 'approve' | 'reject' | 'comment';
}

// Interface pour l'historique des actions
interface ActionHistory {
  id: number;
  action: string;
  adminName: string;
  timestamp: Date;
  notes?: string;
  previousStatus?: RequestStatus;
  newStatus?: RequestStatus;
}

@Component({
  selector: 'app-partnership-request-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './partnership-request-detail.component.html',
  styleUrls: ['./partnership-request-detail.component.scss'],
})
export class PartnershipRequestDetailComponent implements OnInit {
  // Signaux pour la gestion d'état
  loading = signal(false);
  processing = signal(false);
  partnershipRequest = signal<IPartnershipRequest | null>(null);
  showAdminPanel = signal(false);
  showHistory = signal(false);
  showNotes = signal(false);

  // Formulaires
  adminNotesForm!: FormGroup;
  statusUpdateForm!: FormGroup;

  // Données additionnelles
  adminNotes = signal<AdminNote[]>([]);
  actionHistory = signal<ActionHistory[]>([]);

  // ID de la demande
  private requestId!: number;

  // Configuration des statuts
  readonly statusOptions = [
    { value: RequestStatus.PENDING, label: 'En attente', color: 'orange' },
    { value: RequestStatus.IN_REVIEW, label: "En cours d'examen", color: 'blue' },
    { value: RequestStatus.APPROVED, label: 'Approuvée', color: 'green' },
    { value: RequestStatus.REJECTED, label: 'Rejetée', color: 'red' },
  ];

  readonly statusColors = {
    [RequestStatus.PENDING]: 'bg-orange-100 text-orange-800 border-orange-200',
    [RequestStatus.APPROVED]: 'bg-green-100 text-green-800 border-green-200',
    [RequestStatus.REJECTED]: 'bg-red-100 text-red-800 border-red-200',
    [RequestStatus.IN_REVIEW]: 'bg-blue-100 text-blue-800 border-blue-200',
  };

  readonly statusLabels = {
    [RequestStatus.PENDING]: 'En attente',
    [RequestStatus.APPROVED]: 'Approuvée',
    [RequestStatus.REJECTED]: 'Rejetée',
    [RequestStatus.IN_REVIEW]: "En cours d'examen",
  };

  readonly industryLabels: { [key: string]: string } = {
    ecommerce: 'E-commerce',
    retail: 'Commerce de détail',
    services: 'Services',
    technology: 'Technologie',
    healthcare: 'Santé',
    finance: 'Finance',
    education: 'Éducation',
    restaurant: 'Restauration',
    'real-estate': 'Immobilier',
    automotive: 'Automobile',
    travel: 'Voyage & Tourisme',
    consulting: 'Conseil',
    other: 'Autre',
  };

  // Computed properties
  canEdit = computed(() => {
    const request = this.partnershipRequest();
    return request && (request.status === RequestStatus.PENDING || request.status === RequestStatus.IN_REVIEW);
  });

  canApprove = computed(() => {
    const request = this.partnershipRequest();
    return request && (request.status === RequestStatus.PENDING || request.status === RequestStatus.IN_REVIEW);
  });

  canReject = computed(() => {
    const request = this.partnershipRequest();
    return request && (request.status === RequestStatus.PENDING || request.status === RequestStatus.IN_REVIEW);
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private fb: FormBuilder,
    private partnershipRequestService: PartnershipRequestService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.requestId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.requestId) {
      this.loadPartnershipRequest();
      this.loadAdminNotes();
      this.loadActionHistory();
    } else {
      this.router.navigate(['/admin/partnership-requests']);
    }
  }

  /**
   * Initialisation des formulaires
   */
  private initializeForms(): void {
    this.adminNotesForm = this.fb.group({
      note: ['', [Validators.required, Validators.minLength(10)]],
    });

    this.statusUpdateForm = this.fb.group({
      status: ['', Validators.required],
      adminNotes: [''],
    });
  }

  /**
   * Chargement de la demande de partenariat
   */
  loadPartnershipRequest(): void {
    this.loading.set(true);

    this.partnershipRequestService
      .find(this.requestId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          if (response.body) {
            this.partnershipRequest.set(response.body);
            this.statusUpdateForm.patchValue({
              status: response.body.status,
            });
          }
        },
        error: error => {
          console.error('Erreur lors du chargement de la demande:', error);
          this.showError('Erreur lors du chargement de la demande');
          this.router.navigate(['/admin/partnership-requests']);
        },
      });
  }

  /**
   * Chargement des notes administratives
   */
  loadAdminNotes(): void {
    // Simulation des notes - remplacez par un appel API réel
    const mockNotes: AdminNote[] = [
      {
        id: 1,
        note: "Demande initiale reçue. Besoin de vérifier les informations de l'entreprise.",
        adminName: 'John Doe',
        createdDate: new Date('2024-01-15T10:30:00'),
        action: 'comment',
      },
      {
        id: 2,
        note: "Informations vérifiées. L'entreprise semble légitime avec un bon potentiel.",
        adminName: 'Jane Smith',
        createdDate: new Date('2024-01-16T14:20:00'),
        action: 'comment',
      },
    ];
    this.adminNotes.set(mockNotes);
  }

  /**
   * Chargement de l'historique des actions
   */
  loadActionHistory(): void {
    // Simulation de l'historique - remplacez par un appel API réel
    const mockHistory: ActionHistory[] = [
      {
        id: 1,
        action: 'Demande créée',
        adminName: 'Système',
        timestamp: new Date('2024-01-15T09:00:00'),
        newStatus: RequestStatus.PENDING,
      },
      {
        id: 2,
        action: 'Mise en examen',
        adminName: 'John Doe',
        timestamp: new Date('2024-01-15T10:30:00'),
        previousStatus: RequestStatus.PENDING,
        newStatus: RequestStatus.IN_REVIEW,
        notes: "Début de l'examen de la demande",
      },
    ];
    this.actionHistory.set(mockHistory);
  }

  /**
   * Approbation de la demande
   */
  approveRequest(): void {
    const request = this.partnershipRequest();
    if (!request) return;

    const adminNotes = this.adminNotesForm.get('note')?.value || '';

    if (confirm(`Êtes-vous sûr de vouloir approuver cette demande ?`)) {
      this.processing.set(true);

      this.partnershipRequestService
        .approve(request.id, adminNotes)
        .pipe(finalize(() => this.processing.set(false)))
        .subscribe({
          next: response => {
            if (response.body) {
              this.partnershipRequest.set(response.body);
              this.showSuccess('Demande approuvée avec succès');
              this.loadActionHistory();
              this.adminNotesForm.reset();
            }
          },
          error: error => {
            console.error("Erreur lors de l'approbation:", error);
            this.showError("Erreur lors de l'approbation");
          },
        });
    }
  }

  /**
   * Rejet de la demande
   */
  rejectRequest(): void {
    const request = this.partnershipRequest();
    if (!request) return;

    const adminNotes = this.adminNotesForm.get('note')?.value || '';

    if (!adminNotes.trim()) {
      this.showError('Veuillez fournir une raison pour le rejet');
      return;
    }

    if (confirm(`Êtes-vous sûr de vouloir rejeter cette demande ?`)) {
      this.processing.set(true);

      this.partnershipRequestService
        .reject(request.id, adminNotes)
        .pipe(finalize(() => this.processing.set(false)))
        .subscribe({
          next: response => {
            if (response.body) {
              this.partnershipRequest.set(response.body);
              this.showSuccess('Demande rejetée');
              this.loadActionHistory();
              this.adminNotesForm.reset();
            }
          },
          error: error => {
            console.error('Erreur lors du rejet:', error);
            this.showError('Erreur lors du rejet');
          },
        });
    }
  }

  /**
   * Mise à jour du statut
   */
  updateStatus(): void {
    const request = this.partnershipRequest();
    if (!request) return;

    const formValue = this.statusUpdateForm.value;
    if (!formValue.status) return;

    this.processing.set(true);

    const updatedRequest = {
      ...request,
      status: formValue.status,
      adminNotes: formValue.adminNotes,
    };

    this.partnershipRequestService
      .update(updatedRequest)
      .pipe(finalize(() => this.processing.set(false)))
      .subscribe({
        next: response => {
          if (response.body) {
            this.partnershipRequest.set(response.body);
            this.showSuccess('Statut mis à jour avec succès');
            this.loadActionHistory();
          }
        },
        error: error => {
          console.error('Erreur lors de la mise à jour:', error);
          this.showError('Erreur lors de la mise à jour');
        },
      });
  }

  /**
   * Ajout d'une note administrative
   */
  addAdminNote(): void {
    const note = this.adminNotesForm.get('note')?.value;
    if (!note?.trim()) return;

    // Simulation d'ajout de note - remplacez par un appel API réel
    const newNote: AdminNote = {
      id: Date.now(),
      note: note,
      adminName: 'Admin Actuel', // Récupérer depuis le service d'authentification
      createdDate: new Date(),
      action: 'comment',
    };

    const currentNotes = this.adminNotes();
    this.adminNotes.set([...currentNotes, newNote]);
    this.adminNotesForm.reset();
    this.showSuccess('Note ajoutée avec succès');
  }

  /**
   * Envoi d'un email au demandeur
   */
  sendEmail(): void {
    const request = this.partnershipRequest();
    if (!request?.email) return;

    const subject = encodeURIComponent(`Concernant votre demande de partenariat #${request.id}`);
    const body = encodeURIComponent(
      `Bonjour ${this.getFullName(request)},\n\nConcernant votre demande de partenariat...\n\nCordialement,\nÉquipe Partenariats`,
    );

    window.open(`mailto:${request.email}?subject=${subject}&body=${body}`);
  }

  /**
   * Téléchargement des informations en PDF
   */
  downloadPDF(): void {
    const request = this.partnershipRequest();
    if (!request) return;

    // Implémentation du téléchargement PDF
    console.log('Téléchargement PDF de la demande:', request.id);
    this.showInfo('Fonctionnalité de téléchargement en cours de développement');
  }

  /**
   * Navigation
   */
  goBack(): void {
    this.location.back();
  }

  goToList(): void {
    this.router.navigate(['/admin/partnership-requests']);
  }

  /**
   * Utilitaires d'affichage
   */
  getFullName(request: IPartnershipRequest | null | undefined): string {
    if (!request) return '';
    return `${request.firstName || ''} ${request.lastName || ''}`.trim();
  }

  getInitials(request: IPartnershipRequest | null | undefined): string {
    if (!request) return '';
    const firstName = (request.firstName ?? '').trim();
    const lastName = (request.lastName ?? '').trim();
    const firstInitial = firstName.charAt(0).toUpperCase();
    const lastInitial = lastName.charAt(0).toUpperCase();
    return `${firstInitial}${lastInitial}`;
  }

  getStatusLabel(status: RequestStatus | string | null | undefined): string {
    if (!status) return 'Inconnu'; // ou '' selon ton besoin
    return this.statusLabels[status as RequestStatus] || status;
  }

  getStatusClasses(status: RequestStatus | string | null | undefined): string {
    if (!status) return 'bg-gray-100 text-gray-800 border-gray-200'; // couleur de fallback
    return this.statusColors[status as RequestStatus] || 'bg-gray-100 text-gray-800 border-gray-200';
  }

  getIndustryLabel(industry: string | null | undefined): string {
    if (!industry) return 'Inconnu'; // ou une chaîne vide, ou '--', selon ta préférence
    return this.industryLabels[industry] || industry;
  }

  formatDate(date: any): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  formatDateShort(date: any): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getRelativeTime(date: any): string {
    if (!date) return '';
    const now = new Date();
    const then = new Date(date);
    const diffInHours = Math.floor((now.getTime() - then.getTime()) / (1000 * 60 * 60));

    if (diffInHours < 1) {
      const diffInMinutes = Math.floor((now.getTime() - then.getTime()) / (1000 * 60));
      return `Il y a ${diffInMinutes} min`;
    } else if (diffInHours < 24) {
      return `Il y a ${diffInHours}h`;
    } else {
      const diffInDays = Math.floor(diffInHours / 24);
      return `Il y a ${diffInDays}j`;
    }
  }

  /**
   * Utilitaires pour les panneaux
   */
  toggleAdminPanel(): void {
    this.showAdminPanel.set(!this.showAdminPanel());
  }

  toggleHistory(): void {
    this.showHistory.set(!this.showHistory());
  }

  toggleNotes(): void {
    this.showNotes.set(!this.showNotes());
  }

  /**
   * Messages utilisateur
   */
  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 3000,
      panelClass: ['success-snackbar'],
    });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: ['error-snackbar'],
    });
  }

  private showInfo(message: string): void {
    this.snackBar.open(message, 'Fermer', {
      duration: 4000,
      panelClass: ['info-snackbar'],
    });
  }

  /**
   * TrackBy functions pour optimiser les performances
   */
  trackByActionId(index: number, action: ActionHistory): number {
    return action.id;
  }

  trackByNoteId(index: number, note: AdminNote): number {
    return note.id;
  }

  // Exposition des enums pour le template
  protected readonly RequestStatus = RequestStatus;
}
