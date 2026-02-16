import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContactService } from '../service/contact.service';
import { FormsModule } from '@angular/forms';
import { NgClass, NgForOf, NgIf, DatePipe, CommonModule } from '@angular/common';
import { ToastComponent } from '../../toast/toast.component';
import { IContact, ImportHistory, Page } from '../contact.model';
import { AccountService } from '../../../core/auth/account.service';

@Component({
  selector: 'jhi-import-detail',
  templateUrl: './import-detail.component.html',
  standalone: true,
  imports: [FormsModule, NgClass, NgForOf, NgIf, DatePipe, ToastComponent, CommonModule],
})
export class ImportDetailComponent implements OnInit {
  importHistory: ImportHistory | null = null;
  contacts: IContact[] = [];
  isLoading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  loadingSpinner = false;
  totalPages = 0;
  bulkId: string | null = null;

  detailsLoading = false;
  contactsLoading = false;

  public router = inject(Router);
  @ViewChild('toast', { static: true }) toast!: ToastComponent;

  constructor(
    private contactService: ContactService,
    private accountService: AccountService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.bulkId = this.route.snapshot.paramMap.get('bulkId');
    this.accountService.identity(true).subscribe();

    if (this.bulkId) {
      this.loadImportDetails();
      this.loadContacts();
    } else {
      this.showToast("ID d'import manquant dans l'URL", 'error');
      this.goBackToImports();
    }
  }

  loadImportDetails(): void {
    if (!this.bulkId) return;

    this.detailsLoading = true;
    this.isLoading = true;

    this.contactService.getImportHistoryByBulkId(this.bulkId).subscribe({
      next: (importHistory: ImportHistory) => {
        this.importHistory = importHistory;
        this.detailsLoading = false;
        this.updateLoadingState();
        console.log(" Détails d'import chargés:", importHistory);
      },
      error: (err: any) => {
        this.detailsLoading = false;
        this.updateLoadingState();
        console.error(' Erreur lors du chargement des détails:', err);

        if (err.status === 404) {
          this.showToast('Import introuvable. Il a peut-être été supprimé.', 'error');
          setTimeout(() => this.goBackToImports(), 2000);
        } else if (err.status === 403) {
          this.showToast("Vous n'avez pas l'autorisation d'accéder à cet import.", 'error');
          setTimeout(() => this.goBackToImports(), 2000);
        } else {
          this.showToast("Erreur lors de la récupération des détails de l'import.", 'error');
        }
      },
    });
  }

  loadContacts(page: number = this.currentPage, size: number = this.pageSize): void {
    if (!this.bulkId) return;

    this.contactsLoading = true;
    this.isLoading = true;

    this.contactService.getContactsByBulkId(this.bulkId, page, size).subscribe({
      next: (response: Page<IContact>) => {
        this.contacts = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.currentPage = response.number || 0;

        this.contactsLoading = false;
        this.updateLoadingState();

        console.log(' Contacts chargés:', {
          total: this.totalElements,
          page: this.currentPage + 1,
          pages: this.totalPages,
          contacts: this.contacts.length,
        });
      },
      error: (err: any) => {
        this.contactsLoading = false;
        this.updateLoadingState();
        console.error(' Erreur lors du chargement des contacts:', err);
        this.showToast('Erreur lors de la récupération des contacts.', 'error');
      },
    });
  }

  private updateLoadingState(): void {
    this.isLoading = this.detailsLoading || this.contactsLoading;
  }

  refresh(): void {
    if (this.isLoading) {
      console.log('⚠ Actualisation déjà en cours...');
      return;
    }

    console.log(' Actualisation des données...');
    this.loadImportDetails();
    this.loadContacts();
    this.showToast('Données actualisées', 'success');
  }

  trackByContactId(index: number, contact: IContact): number {
    return contact.id;
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

  getPercentage(value: number, total: number): number {
    if (total === 0) return 0;
    return (value / total) * 100;
  }

  goBackToImports(): void {
    this.router.navigate(['/imports']);
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages && this.bulkId && page !== this.currentPage) {
      console.log(` Changement de page: ${this.currentPage + 1} → ${page + 1}`);
      this.currentPage = page;
      this.loadContacts();
    }
  }

  view(contact: IContact): void {
    console.log('👁 Visualisation du contact:', contact.id);
    this.router.navigate(['/contact', contact.id, 'view']);
  }

  exportContacts(): void {
    if (!this.bulkId) {
      this.showToast('Aucun import sélectionné.', 'error');
      return;
    }

    if (this.loadingSpinner) {
      console.log('Export déjà en cours...');
      return;
    }

    console.log(" Début de l'export pour bulkId:", this.bulkId);
    this.loadingSpinner = true;

    this.contactService.exportContactsByBulkId(this.bulkId).subscribe({
      next: (blob: Blob) => {
        try {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          const fileName = `contacts_${this.bulkId}_${new Date().getTime()}.csv`;

          link.href = url;
          link.download = fileName;
          link.style.display = 'none';

          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);

          setTimeout(() => window.URL.revokeObjectURL(url), 1000);

          this.loadingSpinner = false;
          this.showToast(`Export réussi: ${fileName}`, 'success');
        } catch (error) {
          this.loadingSpinner = false;
          console.error(' Erreur lors de la création du fichier:', error);
          this.showToast("Erreur lors de la création du fichier d'export.", 'error');
        }
      },
      error: (err: any) => {
        this.loadingSpinner = false;
        console.error(" Erreur lors de l'export:", err);

        if (err.status === 404) {
          this.showToast('Aucun contact trouvé pour cet import.', 'error');
        } else if (err.status === 403) {
          this.showToast("Vous n'avez pas l'autorisation d'exporter ces contacts.", 'error');
        } else {
          this.showToast("Erreur lors de l'exportation des contacts.", 'error');
        }
      },
    });
  }

  downloadErrorFile(): void {
    if (!this.importHistory?.bulkId) {
      this.showToast("Aucun fichier d'erreur disponible.", 'error');
      return;
    }

    console.log(" Téléchargement du fichier d'erreurs pour:", this.importHistory.bulkId);

    this.contactService.downloadErrorFile(this.importHistory.bulkId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        const fileName = `errors_${this.importHistory!.bulkId}.csv`;

        link.href = url;
        link.download = fileName;
        link.style.display = 'none';

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        this.showToast("Fichier d'erreurs téléchargé avec succès", 'success');
      },
      error: (error: any) => {
        console.error(" Erreur lors du téléchargement du fichier d'erreurs:", error);
        this.showToast("Erreur lors du téléchargement du fichier d'erreurs", 'error');
      },
    });
  }

  showToast(message: string, type: 'success' | 'error'): void {
    try {
      if (this.toast) {
        this.toast.showToast(message, type);
        console.log(`🍞 Toast affiché: [${type.toUpperCase()}] ${message}`);
      } else {
        console.warn(' Toast component non disponible');
        if (type === 'error') {
          alert(`Erreur: ${message}`);
        }
      }
    } catch (error) {
      console.error(" Erreur lors de l'affichage du toast:", error);
    }
  }
}
