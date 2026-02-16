import { Component, computed, inject, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ITemplate, Template } from '../template.model';
import { PageTemplate, TemplateService } from '../service/template.service';
import { DatePipe, NgClass, NgForOf, NgIf } from '@angular/common';
import { AccountService } from '../../../core/auth/account.service';
import { FormsModule } from '@angular/forms';
import { ToastComponent } from '../../toast/toast.component';
import { DynamicMenuService } from '../../../Subscription/service/dynamicMenuService.service';

@Component({
  selector: 'app-template-list',
  templateUrl: './template-list.component.html',
  imports: [NgForOf, NgIf, NgClass, FormsModule, DatePipe, ToastComponent],
  standalone: true,
})
export class TemplateListComponent implements OnInit {
  permissions = {
    canApprove: false,
    canView: false,
    canEdit: false,
    canDelete: false,
    canCreate: false,
    canCreateSms: false,
    canCreateWhatsapp: false,
  };
  templates: ITemplate[] = [];
  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  activeTab: 'APPROVED' | 'PENDING' | 'REJECTED' = 'APPROVED';
  showModal: boolean = false;
  public dynamicMenuService = inject(DynamicMenuService);
  action: 'delete' | 'approve' | null = null;
  showImportModal: boolean = false;
  importTemplateName: string = '';
  searchTerm: string = '';
  selectedTemplateId: number | null = null;
  currentPage: number = 1;
  itemsPerPage: number = 10;
  totalPages: number = 1;
  itemsPerPageOptions: number[] = [10, 20, 25, 30, 50];
  totalElements = 0;
  loading = false;
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  constructor(
    private templateService: TemplateService,
    protected router: Router,
    private accountService: AccountService,
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
    this.initialPermsiiions();
  }
  initialPermsiiions() {
    const isAdmin = this.isAdmin();
    const hasCreate = this.dynamicMenuService.hasPermission('templates.create');
    const hasApprove = this.dynamicMenuService.hasPermission('templates.approved');
    const hasView = this.dynamicMenuService.hasPermission('templates');
    const hasEdit = this.dynamicMenuService.hasPermission('templates.edit');
    const hasDelete = this.dynamicMenuService.hasPermission('templates.delete');
    const hasSendSms = this.dynamicMenuService.hasPermission('templates.send.sms');
    const hasSendWhatsapp = this.dynamicMenuService.hasPermission('templates.send.whatsapp');

    this.permissions = {
      canApprove: isAdmin && hasApprove,
      canView: hasView,
      canEdit: hasEdit,
      canDelete: hasDelete,
      canCreate: hasCreate,
      canCreateSms: hasCreate && hasSendSms,
      canCreateWhatsapp: hasCreate && hasSendWhatsapp,
    };
  }

  openImportModal(): void {
    this.showImportModal = true;
    this.importTemplateName = '';
  }

  cancelImport(): void {
    this.showImportModal = false;
    this.importTemplateName = '';
  }

  confirmImport(): void {
    if (!this.importTemplateName || this.importTemplateName.trim() === '') {
      this.toast.showToast('Veuillez entrer un nom de template', 'error');
      return;
    }

    this.loading = true;

    this.templateService.importTemplateFromMeta(this.importTemplateName.trim()).subscribe(
      importedTemplate => {
        this.loading = false;
        this.toast.showToast('Template importé avec succès !', 'success');
        this.showImportModal = false;
        this.importTemplateName = '';
        this.loadTemplates(); // Recharger la liste
      },
      error => {
        this.loading = false;
        console.error("Erreur lors de l'importation :", error);

        if (error.status === 404) {
          this.toast.showToast('Template non trouvé dans Meta', 'error');
        } else if (error.status === 401) {
          this.toast.showToast('Configuration WhatsApp manquante', 'error');
        } else {
          this.toast.showToast("Erreur lors de l'importation du template", 'error');
        }
      },
    );
  }
  loadTemplates(): void {
    // Le back-end attend un index de page commençant à 0
    const backendPage = this.currentPage - 1;
    this.loading = true;
    this.templateService.getTemplates(this.activeTab, backendPage, this.itemsPerPage, this.searchTerm).subscribe(
      (pageResponse: PageTemplate) => {
        this.loading = false;
        this.templates = pageResponse.content;
        this.templates = pageResponse.content.sort((a, b) => {
          const dateA = a.created_at ? new Date(a.created_at).getTime() : 0;
          const dateB = b.created_at ? new Date(b.created_at).getTime() : 0;
          return dateB - dateA;
        });

        console.log('PageTemplate', pageResponse);
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
      },
      error => {
        this.loading = false;
        console.error('Erreur lors du chargement des templates :', error);
      },
    );
  }
  onRefresh() {
    //this.templateService.refreshStatuses().subscribe(() => this.loadTemplates());
  }
  hasPermission(isSms: boolean): boolean {
    return isSms
      ? this.dynamicMenuService.hasPermission('templates.send.sms')
      : this.dynamicMenuService.hasPermission('templates.send.whatsapp');
  }
  permission(permission: string): boolean {
    return this.dynamicMenuService.hasPermission(permission);
  }
  changeItemsPerPage(): void {
    this.currentPage = 1; // remise à zéro ou réinitialisation de la pagination
    this.loadTemplates();
  }
  onSearch(): void {
    this.currentPage = 1;
    this.loadTemplates();
  }

  onTabChange(tab: 'APPROVED' | 'PENDING' | 'REJECTED'): void {
    this.activeTab = tab;
    this.searchTerm = '';
    this.currentPage = 1;
    this.loadTemplates();
    // this.loadWhatsappTemplates();
  }

  deleteTemplate(id: number): void {
    this.selectedTemplateId = id;
    this.action = 'delete';
    this.showModal = true;
  }

  // Navigation vers la page de visualisation du template
  viewTemplate(id: number) {
    console.log(`Visualiser le template ${id}`);
    this.router.navigate(['/template', id, 'view']);
  }

  // Navigation vers la page d'édition du template
  editTemplate(id: number) {
    console.log(`Modifier le template ${id}`);
    this.router.navigate(['/template', id, 'edit']);
  }
  approveTemplate(template: ITemplate) {
    this.selectedTemplateId = template.id;
    this.action = 'approve';
    this.showModal = true;
  }

  confirmAction() {
    if (this.action === 'delete' && this.selectedTemplateId !== null) {
      this.templateService.deleteTemplate(this.selectedTemplateId).subscribe(() => {
        this.loadTemplates();
      });
    } else if (this.action === 'approve' && this.selectedTemplateId !== null) {
      this.templateService.approveTemplate(this.selectedTemplateId).subscribe(
        updatedTemplate => {
          this.loadTemplates();
        },
        error => {
          this.toast.showToast('Le template n’a pas encore été validé par Meta.', 'error');

          console.error("Erreur lors de l'approbation : ", error);
        },
      );
    }
    this.resetModal();
  }

  // Annulation de l'action
  cancelAction() {
    this.resetModal();
  }

  // Réinitialisation de la modale
  private resetModal() {
    this.showModal = false;
    this.action = null;
    this.selectedTemplateId = null;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadTemplates();
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadTemplates();
    }
  }
}
