// contact-import.component.ts
import {
  Component,
  EventEmitter,
  Output,
  Input,
  ViewChild,
  OnDestroy,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  inject,
  HostListener,
  OnInit,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { interval, Observable, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ContactService } from '../service/contact.service';
import { GroupeService } from '../../groupe/service/groupe.service';
import { ToastComponent } from '../../toast/toast.component';
import { IGroupe } from '../../groupe/groupe.model';
import { ProgressStatus, IContact } from '../contact.model';
import { DuplicateContactsResponse } from '../list/contact.component';

@Component({
  selector: 'app-contact-import',
  templateUrl: './contact-import.component.html',
  standalone: true,
  imports: [FormsModule, CommonModule, ToastComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContactImportComponent implements OnInit, OnDestroy {
  @Input() isVisible = false;
  @Input() selectedGroupInput!: IGroupe | null;

  @Output() closeImport = new EventEmitter<void>();
  @Output() importCompleted = new EventEmitter<DuplicateContactsResponse>();

  @ViewChild('toast') toast!: ToastComponent;

  private contactService = inject(ContactService);
  private groupeService = inject(GroupeService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  uploadedFiles: File[] = [];
  selectedGroup: IGroupe | null = null;
  searchTerm = '';
  groups: IGroupe[] = [];
  dropdownOpen = false;
  isLoading = false;

  importResult: DuplicateContactsResponse | null = null;
  progressStatus: ProgressStatus | null = null;
  progressSubscription?: Subscription;

  addedContacts: IContact[] = [];
  showAddedContacts = false;
  groupActionMode: 'add' | 'replace' = 'add';

  private searchTimeout?: number;
  private readonly DEBOUNCE_TIME = 300;
  private readonly PROGRESS_INTERVAL = 1500;

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.dropdownOpen && !(event.target as HTMLElement).closest('.dropdown-container')) {
      this.dropdownOpen = false;
      this.cdr.markForCheck();
    }
  }
  ngOnInit(): void {
    console.log('selectedGroupInput', this.selectedGroupInput);
    if (this.selectedGroupInput) {
      this.selectedGroup = this.selectedGroupInput;
    }
  }
  ngOnDestroy(): void {
    this.stopProgressPolling();
    this.clearSearchTimeout();
  }

  onFileSelect(event: any): void {
    const files = event.target.files;
    if (!files?.length) return;

    const file = files[0];
    if (!this.validateFile(file)) return;

    this.uploadedFiles = [file];
    this.resetImportState();
    this.cdr.markForCheck();

    this.showToast(` Fichier "${file.name}" sélectionné (${this.formatFileSize(file.size)})`, 'success');
  }

  private validateFile(file: File): boolean {
    if (!file.type.includes('csv') && !file.name.endsWith('.csv')) {
      this.showToast(' Format invalide. Seuls les fichiers CSV sont acceptés.', 'error');
      return false;
    }

    const MAX_SIZE = 100 * 1024 * 1024;
    if (file.size > MAX_SIZE) {
      this.showToast(` Fichier trop volumineux (maximum ${MAX_SIZE / (1024 * 1024)}MB)`, 'error');
      return false;
    }

    if (file.size === 0) {
      this.showToast(' Le fichier est vide', 'error');
      return false;
    }

    return true;
  }
  removeFile(): void {
    this.uploadedFiles = [];
    this.resetImportState();
    this.cdr.markForCheck();
    this.showToast('🗑 Fichier supprimé', 'info');
  }

  private resetImportState(): void {
    this.importResult = null;
    this.addedContacts = [];
    this.showAddedContacts = false;
    this.progressStatus = null;
    this.stopProgressPolling();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
    if (this.dropdownOpen) {
      this.loadGroups();
      setTimeout(() => {
        const searchInput = document.querySelector('input[placeholder="Rechercher un groupe..."]') as HTMLInputElement;
        searchInput?.focus();
      }, 100);
    }
    this.cdr.markForCheck();
  }

  onSearchChange(): void {
    this.clearSearchTimeout();
    this.searchTimeout = setTimeout(() => {
      this.loadGroups();
    }, this.DEBOUNCE_TIME) as any;
  }

  private clearSearchTimeout(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
      this.searchTimeout = undefined;
    }
  }

  private loadGroups(): void {
    const params = { page: 0, size: 50, search: this.searchTerm.trim() };
    this.groupeService.query(params).subscribe({
      next: response => {
        this.groups = response.body || [];
        this.groups.sort((a, b) => {
          if (!a.grotitre) return 1;
          if (!b.grotitre) return -1;
          return a.grotitre.localeCompare(b.grotitre, 'fr', { sensitivity: 'base' });
        });
        this.cdr.markForCheck();
      },
      error: error => {
        console.error('Erreur chargement groupes:', error);
        this.showToast(' Erreur lors du chargement des groupes', 'error');
      },
    });
  }

  selectGroup(group: IGroupe): void {
    this.selectedGroup = group;
    this.searchTerm = group.grotitre || '';
    this.dropdownOpen = false;
    this.cdr.markForCheck();
    this.showToast(` Groupe "${group.grotitre}" sélectionné`, 'info');
  }

  async importContacts(insert: boolean): Promise<void> {
    if (!this.validateImportConditions()) return;

    this.isLoading = true;
    this.resetImportState();
    this.cdr.markForCheck();

    const startTime = Date.now();

    try {
      console.log(" Démarrage de l'import:", {
        file: this.uploadedFiles[0].name,
        group: this.selectedGroup!.grotitre,
        mode: this.groupActionMode,
        insert,
      });

      const result = await this.contactService.cleanContacts(this.uploadedFiles[0], this.selectedGroup!.id!, insert).toPromise();

      this.handleImportSuccess(result!, insert, Date.now() - startTime);
    } catch (error) {
      this.handleImportError(error);
    } finally {
      this.isLoading = false;
      this.cdr.markForCheck();
    }
  }

  private validateImportConditions(): boolean {
    if (!this.selectedGroup?.id) {
      this.showToast(' Veuillez sélectionner un groupe de destination', 'error');
      return false;
    }

    if (!this.uploadedFiles.length) {
      this.showToast(' Veuillez sélectionner un fichier CSV', 'error');
      return false;
    }

    return true;
  }

  private handleImportSuccess(result: DuplicateContactsResponse, insert: boolean, duration: number): void {
    this.importResult = result;

    console.log(" Résultat de l'import:", {
      totalLines: result.totalFileLines,
      inserted: result.totalInserted,
      duplicates: result.totalDuplicates,
      errors: result.totalErrors,
      progressId: result.progressId,
      uniqueContacts: result.uniqueContacts?.length || 0,
      allContactsToInsert: result.allContactsToInsert?.length || 0,
    });

    if (insert && result.progressId && result.totalInserted > 0) {
      this.startProgressPolling(result.progressId);
      this.showToast(` Import lancé! ${result.totalInserted} contacts à traiter`, 'success');
    } else if (result.totalInserted == 0) {
      this.showToast(` Analyse terminée en ${Math.round(duration / 1000)}s - ${result.totalInserted} contacts valides`, 'success');
      this.progressStatus = {
        total: this.totalAddedToGroup,
        inserted: this.totalAddedToGroup,
        current: this.totalAddedToGroup,
        percentage: 100,
        completed: true,
        insertionRate: 0,
        estimatedTimeRemaining: 0,
      };
    } else {
      this.showToast(` Analyse terminée en ${Math.round(duration / 1000)}s - ${result.totalInserted} contacts valides`, 'success');

      // Si analyse seulement, charger un aperçu des contacts
      if (result.totalInserted > 0) {
        this.loadPreviewContacts(result);
      }
    }

    this.importCompleted.emit(result);
  }

  private handleImportError(error: any): void {
    console.error(' Erreur import:', error);

    const messages: Record<number, string> = {
      400: 'Format de fichier ou données invalides',
      401: "Vous n'êtes pas autorisé à effectuer cette action",
      413: 'Fichier trop volumineux - Maximum 100MB',
      422: 'Données invalides dans le fichier CSV',
      500: 'Erreur serveur - Veuillez réessayer',
    };

    const message = messages[error.status] || `Erreur lors de l'import (${error.status || 'inconnue'})`;
    this.showToast(` ${message}`, 'error');

    if (error.status === 0 || error.name === 'TimeoutError') {
      this.showToast(' Le fichier est très volumineux - Le traitement peut prendre plusieurs minutes', 'info');
    }
  }

  private startProgressPolling(progressId: string): void {
    this.stopProgressPolling();

    console.log(' Démarrage du polling de progression:', progressId);

    this.progressSubscription = interval(this.PROGRESS_INTERVAL)
      .pipe(
        switchMap(() => {
          console.log(' Polling progression...');
          return this.contactService.getProgress(progressId);
        }),
      )
      .subscribe({
        next: (status: ProgressStatus) => {
          console.log(' Status reçu:', status);
          this.progressStatus = status;
          this.cdr.markForCheck();

          if (status.completed) {
            console.log(' Import terminé!');
            this.onProgressCompleted();
          }
        },
        error: error => {
          console.error(' Erreur polling progression:', error);
          this.stopProgressPolling();
          this.showToast(' Erreur de suivi de progression', 'error');
        },
      });
  }

  private stopProgressPolling(): void {
    if (this.progressSubscription) {
      this.progressSubscription.unsubscribe();
      this.progressSubscription = undefined;
      console.log('Polling progression arrêté');
    }
  }

  private onProgressCompleted(): void {
    this.stopProgressPolling();

    if (this.importResult?.progressId) {
      this.loadFinalStatistics(this.importResult.progressId);
    }

    this.showToast(' Import terminé avec succès!', 'success');
  }

  private loadFinalStatistics(progressId: string): void {
    console.log(' Chargement des statistiques finales...');

    this.contactService.getImportHistoryByBulkId(progressId).subscribe({
      next: importHistory => {
        console.log(' Statistiques finales:', importHistory);

        if (this.importResult) {
          this.importResult.totalInserted = importHistory.insertedCount || this.importResult.totalInserted;
          this.importResult.totalDuplicates = importHistory.duplicateCount || this.importResult.totalDuplicates;
          this.importResult.totalErrors = importHistory.rejectedCount || this.importResult.totalErrors;
        }

        this.loadAddedContacts();
      },
      error: error => {
        console.error(' Erreur chargement statistiques:', error);
        this.loadAddedContacts();
      },
    });
  }

  private loadAddedContacts(): void {
    if (!this.importResult?.progressId) return;

    console.log(' Chargement des contacts ajoutés...');

    this.contactService.getContactsByBulkId(this.importResult.progressId, 0, 20).subscribe({
      next: response => {
        this.addedContacts = response.content || [];
        this.showAddedContacts = this.addedContacts.length > 0;
        this.cdr.markForCheck();

        console.log(` ${this.addedContacts.length} contacts chargés`);
      },
      error: error => {
        console.error(' Erreur chargement contacts:', error);
        this.showToast(' Impossible de charger les contacts ajoutés', 'error');
      },
    });
  }

  private loadPreviewContacts(result: DuplicateContactsResponse): void {
    const contactsToShow = result.allContactsToInsert || result.uniqueContacts || [];
    this.addedContacts = contactsToShow.slice(0, 10);
    this.showAddedContacts = this.addedContacts.length > 0;
    this.cdr.markForCheck();

    console.log(` Aperçu chargé: ${this.addedContacts.length} contacts sur ${contactsToShow.length} disponibles`);
  }

  downloadFile(fileName: string | undefined, type: 'error' | 'duplicate' | 'database-duplicate' | 'file-duplicate'): void {
    if (!fileName) {
      this.showToast(' Nom de fichier manquant', 'error');
      return;
    }

    let download$: Observable<Blob>;
    let fileLabel: string;

    switch (type) {
      case 'error':
        download$ = this.contactService.downloadErrorFile(fileName);
        fileLabel = "d'erreurs";
        break;
      case 'database-duplicate':
        download$ = this.contactService.downloadDatabaseDuplicateFile(fileName);
        fileLabel = 'de doublons base de données';
        break;
      case 'file-duplicate':
        download$ = this.contactService.downloadFileDuplicateFile(fileName);
        fileLabel = 'de doublons fichier';
        break;
      default:
        download$ = this.contactService.downloadDuplicateFile(fileName);
        fileLabel = 'de doublons';
    }

    download$.subscribe({
      next: (blob: Blob) => {
        this.downloadBlob(blob, this.generateFileName(type));
        this.showToast(` Fichier ${fileLabel} téléchargé`, 'success');
      },
      error: () => {
        this.showToast('Erreur lors du téléchargement', 'error');
      },
    });
  }
  private downloadBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
  private generateFileName(type: 'error' | 'duplicate' | 'database-duplicate' | 'file-duplicate'): string {
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:]/g, '-');
    const groupName = this.selectedGroup?.grotitre?.replace(/[^a-zA-Z0-9]/g, '_') || 'groupe';

    let prefix: string;
    switch (type) {
      case 'error':
        prefix = 'erreurs';
        break;
      case 'database-duplicate':
        prefix = 'doublons_db';
        break;
      case 'file-duplicate':
        prefix = 'doublons_fichier';
        break;
      default:
        prefix = 'doublons';
    }

    return `${prefix}_${groupName}_${timestamp}.csv`;
  }

  close(): void {
    this.stopProgressPolling();
    this.clearSearchTimeout();
    this.closeImport.emit();
  }

  formatTime(seconds: number): string {
    if (!seconds || seconds <= 0) return '0s';

    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.round(seconds % 60);

    if (hours > 0) return `${hours}h ${minutes}m`;
    if (minutes > 0) return `${minutes}m ${secs}s`;
    return `${secs}s`;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  trackByGroupId = (index: number, group: IGroupe): number => group.id!;

  private showToast(message: string, type: 'success' | 'error' | 'info'): void {
    if (this.toast) {
      this.toast.showToast(message, type);
    }
    console.log(` Toast (${type}):`, message);
  }

  get progressPercentage(): number {
    if (!this.progressStatus) return 0;
    return Math.round((this.progressStatus.current / this.progressStatus.total) * 100);
  }

  get hasResults(): boolean {
    return !!this.importResult;
  }

  get hasProgress(): boolean {
    return !!this.progressStatus && !this.progressStatus.completed;
  }

  get isCompleted(): boolean {
    return !!this.progressStatus?.completed;
  }

  get totalAddedToGroup(): number {
    if (!this.importResult) return 0;
    return this.importResult.totalInserted;
  }

  get newContactsCreated(): number {
    return this.importResult?.totalInserted || 0;
  }

  get existingContactsAddedToGroup(): number {
    if (!this.importResult) return 0;

    return this.totalAddedToGroup - this.newContactsCreated;
  }

  get fileDuplicates(): number {
    return this.importResult?.totalDuplicates || 0;
  }

  get errorLines(): number {
    return this.importResult?.totalErrors || 0;
  }

  get successPercentage(): number {
    if (!this.importResult?.totalFileLines) return 0;
    return Math.round((this.totalAddedToGroup / this.importResult.totalFileLines) * 100);
  }

  get summaryMessage(): string {
    if (!this.importResult) return '';

    const total = this.importResult.totalFileLines;
    const added = this.totalAddedToGroup;
    const newContacts = this.newContactsCreated;
    const existing = this.existingContactsAddedToGroup;

    if (newContacts === 0 && existing > 0) {
      return `Tous les ${existing} contacts existaient déjà et ont été ajoutés au groupe "${this.selectedGroup?.grotitre}".`;
    } else if (newContacts > 0 && existing === 0) {
      return `${newContacts} nouveaux contacts ont été créés et ajoutés au groupe "${this.selectedGroup?.grotitre}".`;
    } else if (newContacts > 0 && existing > 0) {
      return `${newContacts} nouveaux contacts créés et ${existing} contacts existants ajoutés au groupe "${this.selectedGroup?.grotitre}".`;
    } else {
      return `Traitement terminé sur ${total} lignes.`;
    }
  }
}
