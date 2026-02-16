import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { ContactService } from '../service/contact.service';
import { ImportHistory, Page, ProgressStatus } from '../contact.model';
import { ToastComponent } from '../../toast/toast.component';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe, NgClass, NgForOf, NgIf, NgSwitch, NgSwitchCase, NgSwitchDefault } from '@angular/common';

@Component({
  selector: 'jhi-import-result',
  templateUrl: './import-result.component.html',
  standalone: true,
  imports: [ToastComponent, FormsModule, NgIf, NgClass, DatePipe, NgForOf, NgSwitch, NgSwitchCase, DecimalPipe, NgSwitchDefault],
})
export class ImportResultComponent implements OnInit, OnDestroy {
  progressStatus: { [bulkId: string]: ProgressStatus } = {};
  progressSubscriptions: { [bulkId: string]: Subscription } = {};
  importHistory: ImportHistory[] = [];
  completedImports: Set<string> = new Set();

  isLoading = false;
  progressId = '';
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  searchQuery = '';
  @ViewChild('toast', { static: true }) toast!: ToastComponent;

  constructor(private contactService: ContactService) {}

  ngOnInit(): void {
    this.loadImportHistory();
  }

  goToDetail(bulkId: string): void {
    const token = sessionStorage.getItem('jhi-authenticationToken');
    if (token && bulkId) {
      const url = `/detail-import/${bulkId}`;
      const newWindow = window.open(url, '_blank');

      if (newWindow) {
        newWindow.addEventListener('load', () => {
          try {
            newWindow.sessionStorage.setItem('jhi-authenticationToken', token);
          } catch (error) {
            console.error("Impossible d'écrire dans le sessionStorage du nouvel onglet", error);
          }
        });
      } else {
        console.error("Impossible d'ouvrir le nouvel onglet");
        this.toast.showToast("Impossible d'ouvrir les détails dans un nouvel onglet.", 'error');
      }
    } else {
      console.error('Token ou bulkId manquant');
      this.toast.showToast('Erreur : informations manquantes pour ouvrir les détails.', 'error');
    }
  }

  loadImportHistory(page: number = this.currentPage, size: number = this.pageSize, search: string = this.searchQuery): void {
    this.isLoading = true;
    this.contactService.getImportHistory(page, size, search).subscribe({
      next: (response: Page<ImportHistory>) => {
        console.log('Import history response:', response);
        this.importHistory = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.isLoading = false;

        //  Démarrer le polling seulement pour les imports vraiment en cours
        this.importHistory.forEach(importItem => {
          if (this.shouldStartPolling(importItem)) {
            console.log('Starting polling for:', importItem.bulkId, 'Status:', importItem.status);
            this.startProgressPolling(importItem.bulkId);
          }
        });
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error('Error loading import history:', err);
        this.toast.showToast("Erreur lors de la récupération de l'historique.", 'error');
      },
    });
  }

  private shouldStartPolling(importItem: ImportHistory): boolean {
    const shouldPoll =
      (importItem.status === 'PENDING' || importItem.status === 'PROCESSING') &&
      !this.completedImports.has(importItem.bulkId) &&
      !this.progressSubscriptions[importItem.bulkId];

    console.log(`Should start polling for ${importItem.bulkId}?`, {
      status: importItem.status,
      alreadyCompleted: this.completedImports.has(importItem.bulkId),
      alreadyPolling: !!this.progressSubscriptions[importItem.bulkId],
      result: shouldPoll,
    });

    return shouldPoll;
  }

  onSearchChange(search: string): void {
    this.searchQuery = search;
    this.currentPage = 0;
    this.loadImportHistory();
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadImportHistory();
    }
  }

  startProgressPolling(bulkId: string): void {
    if (!bulkId || this.progressSubscriptions[bulkId] || this.completedImports.has(bulkId)) {
      console.log('Skipping polling for:', bulkId, {
        hasSubscription: !!this.progressSubscriptions[bulkId],
        isCompleted: this.completedImports.has(bulkId),
      });
      return;
    }

    this.progressSubscriptions[bulkId] = interval(2000) // 🚀 CORRECTION : Augmenter l'intervalle
      .pipe(switchMap(() => this.contactService.getProgress(bulkId)))
      .subscribe({
        next: (status: ProgressStatus) => {
          console.log('Progress status for', bulkId, ':', status);
          this.progressStatus[bulkId] = status;

          if (this.isProgressCompleted(status)) {
            console.log(' Progress completed for:', bulkId);
            this.handleProgressCompletion(bulkId, status);
          }
        },
        error: (err: any) => {
          console.error('Error getting progress for', bulkId, ':', err);
          this.stopProgressPolling(bulkId);
          this.toast.showToast('Erreur lors de la récupération de la progression.', 'error');
        },
      });
  }

  private isProgressCompleted(status: ProgressStatus): boolean {
    return status.completed || (status.inserted >= status.total && status.total > 0) || status.percentage >= 100;
  }

  private handleProgressCompletion(bulkId: string, status: ProgressStatus): void {
    this.stopProgressPolling(bulkId);
    this.completedImports.add(bulkId);

    if (!this.completedImports.has(bulkId + '_toast_shown')) {
      this.completedImports.add(bulkId + '_toast_shown');
      this.toast.showToast('Insertion terminée avec succès !', 'success');
    }

    setTimeout(() => {
      this.loadImportHistory();
    }, 1000);
  }

  stopProgressPolling(bulkId: string): void {
    if (this.progressSubscriptions[bulkId]) {
      console.log(' Stopping progress polling for:', bulkId);
      this.progressSubscriptions[bulkId].unsubscribe();
      delete this.progressSubscriptions[bulkId];
    }
  }

  formatTime(seconds: number): string {
    if (seconds <= 0) return '0s';
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = Math.round(seconds % 60);
    return minutes > 0 ? `${minutes}m ${remainingSeconds}s` : `${remainingSeconds}s`;
  }

  getDisplayStatus(importItem: ImportHistory): string {
    if (this.progressStatus[importItem.bulkId]) {
      const progress = this.progressStatus[importItem.bulkId];
      if (progress.completed) {
        return 'COMPLETED';
      } else if (progress.current > 0) {
        return 'PROCESSING';
      }
    }
    return importItem.status;
  }

  load(): void {
    this.loadImportHistory();
  }

  trackByBulkId(index: number, item: ImportHistory): string {
    return item.bulkId;
  }

  getVisiblePages(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  }

  ngOnDestroy(): void {
    Object.keys(this.progressSubscriptions).forEach(bulkId => {
      this.stopProgressPolling(bulkId);
    });
    this.completedImports.clear();
  }
}
