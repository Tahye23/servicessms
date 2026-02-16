import { Component, computed, inject, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ITemplate, Template } from '../template.model';
import { ContactFieldInfo, TemplateService, VariableInfo } from '../service/template.service';
import { NgClass, NgForOf, NgIf, NgSwitch, NgSwitchCase } from '@angular/common';
import { AccountService } from '../../../core/auth/account.service';
import { TemplateRendererComponent } from '../detailMessage/template-renderer.component';
import { DynamicMenuService } from '../../../Subscription/service/dynamicMenuService.service';
import { ToastComponent } from '../../toast/toast.component';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-template-detail',
  templateUrl: './template-detail.component.html',
  imports: [NgIf, NgClass, NgForOf, NgSwitchCase, NgSwitch, TemplateRendererComponent, ToastComponent, FormsModule],
  standalone: true,
})
export class TemplateDetailComponent implements OnInit {
  template?: ITemplate;
  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  showModal: boolean = false;
  selectedTemplateId: number | null = null;
  action: 'delete' | 'approve' | 'rejected' | null = null;
  maxCharactersPerMessage = 160;
  showMediaUploadModal = false;
  selectedMediaType: 'IMAGE' | 'VIDEO' | 'DOCUMENT' = 'IMAGE';
  selectedFile: File | null = null;
  uploadingMedia = false;
  mediaPreviewUrl: string | null = null;
  loading = false;
  showActions: boolean = false;
  // Ajouter ces propriétés dans la classe
  showEditModal = false;
  savingTemplate = false;
  editForm = {
    name: '',
    content: '',
  };
  public dynamicMenuService = inject(DynamicMenuService);
  isAdmin = computed(() => this.accountService.hasAnyAuthority('ROLE_ADMIN'));
  contentData!: {
    components: Array<{
      type: string;
      format?: string;
      text?: string;
      mediaUrl?: string;
      buttons?: any[];
      /* … */
    }>;
  };
  showMappingModal = false;
  variables: VariableInfo[] = [];
  variableMappings: Map<string, string> = new Map();
  contactFields: ContactFieldInfo[] = [];
  loadingFields = false;
  constructor(
    private route: ActivatedRoute,
    private templateService: TemplateService,
    private accountService: AccountService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.selectedTemplateId = id;
    this.templateService.getTemplate(id).subscribe(data => {
      this.template = data;
      console.log('this.template', this.template);
      if (this.template.templateId) {
        if (this.template.content) {
          try {
            this.contentData = JSON.parse(this.template.content);
            console.log('this.contentData', this.contentData);
          } catch (e) {
            console.error('contenu JSON invalide', e);
          }
        }
      }
    });
  }
  permission(permission: string): boolean {
    return this.dynamicMenuService.hasPermission(permission);
  }
  get messageCount(): number {
    if (!this.template || this.template.characterCount === undefined) {
      return 0;
    }
    return Math.max(1, Math.ceil(this.template.characterCount / this.maxCharactersPerMessage));
  }

  goBack(): void {
    this.router.navigate(['/template']);
  }
  getTotalTextLength(): number {
    return (
      this.contentData?.components?.reduce((acc, component) => {
        return acc + (component?.text?.length || 0);
      }, 0) || 0
    );
  }

  deleteTemplate(id: number): void {
    this.selectedTemplateId = id;
    this.action = 'delete';
    this.showModal = true;
  }
  approveTemplate(template: ITemplate) {
    this.selectedTemplateId = template.id;
    this.action = 'approve';
    this.showModal = true;
  }
  rejectedTemplate(template: ITemplate) {
    this.selectedTemplateId = template.id;
    this.action = 'rejected';
    this.showModal = true;
  }
  confirmAction() {
    if (this.action === 'delete' && this.selectedTemplateId !== null) {
      this.templateService.deleteTemplate(this.selectedTemplateId).subscribe(() => {
        this.goBack();
      });
    } else if (this.action === 'approve' && this.selectedTemplateId !== null) {
      this.templateService.approveTemplate(this.selectedTemplateId).subscribe(
        updatedTemplate => {
          this.goBack();
        },
        error => {
          console.error("Erreur lors de l'approbation : ", error);
        },
      );
    } else if (this.action === 'rejected' && this.selectedTemplateId !== null) {
      this.templateService.rejectedTemplate(this.selectedTemplateId).subscribe(
        updatedTemplate => {
          this.goBack();
        },
        error => {
          console.error("Erreur lors de l'approbation : ", error);
        },
      );
    }
    this.resetModal();
  }

  cancelAction() {
    this.resetModal();
  }

  // Réinitialisation de la modale
  private resetModal() {
    this.showModal = false;
    this.action = null;
    this.selectedTemplateId = null;
  }
  editTemplate(): void {
    if (this.template) {
      this.editForm = {
        name: this.template.name,
        content: this.template.content,
      };
      this.showEditModal = true;
      this.showActions = false;
    }
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editForm = { name: '', content: '' };
  }

  getEditMessageCount(): number {
    if (!this.editForm.content) return 0;
    return Math.max(1, Math.ceil(this.editForm.content.length / this.maxCharactersPerMessage));
  }

  saveTemplate(): void {
    if (!this.template || !this.editForm.name || !this.editForm.content) return;

    this.savingTemplate = true;

    const updatedTemplate: ITemplate = {
      ...this.template,
      name: this.editForm.name,
      content: this.editForm.content,
    };

    this.templateService.updateTemplate(updatedTemplate).subscribe(
      result => {
        this.savingTemplate = false;
        this.template = result;
        this.toast.showToast('Template mis à jour avec succès !', 'success');
        this.closeEditModal();
      },
      error => {
        this.savingTemplate = false;
        console.error('Erreur lors de la mise à jour:', error);
        this.toast.showToast('Erreur lors de la mise à jour du template', 'error');
      },
    );
  }
  exportTemplate(): void {
    if (!this.template) {
      return;
    }

    // Préparation des données à exporter
    const templateData = {
      name: this.template.name,
      content: this.template.content,
      expediteur: this.template.expediteur,
      status: this.template.approved ? 'Approuvé' : 'En attente',
      created_at: this.template.created_at || new Date().toLocaleDateString('fr-FR'),
      approved_at: this.template.approved ? this.template.approved_at || new Date().toLocaleDateString('fr-FR') : 'Non approuvé',
    };

    // Conversion en JSON
    const jsonData = JSON.stringify(templateData, null, 2);

    // Création d'un blob pour le téléchargement
    const blob = new Blob([jsonData], { type: 'application/json' });

    // Création d'un URL pour le blob
    const url = window.URL.createObjectURL(blob);

    // Création d'un lien temporaire pour le téléchargement
    const a = document.createElement('a');
    a.href = url;
    a.download = `template-${this.template.name.toLowerCase().replace(/\s+/g, '-')}.json`;

    // Simulation du clic pour déclencher le téléchargement
    document.body.appendChild(a);
    a.click();

    // Nettoyage
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);

    // Affichage d'un message de succès (si vous avez un service de notification)
    // this.notificationService.success('Export réussi', 'Le template a été exporté avec succès.');
  }
  openMappingModal(): void {
    this.loadingFields = true;

    // Charger les champs de contact disponibles
    this.templateService.getAvailableFields().subscribe(
      fields => {
        this.contactFields = fields;
        this.loadingFields = false;

        // Extraire les variables du template
        this.templateService.extractTemplateVariables(this.template!.id).subscribe(
          variables => {
            this.variables = variables;

            // Initialiser les mappings avec les noms actuels
            this.variableMappings = new Map();
            variables.forEach(v => {
              // Essayer de trouver une correspondance automatique
              const matchingField = this.findBestMatch(v.name, this.contactFields);
              this.variableMappings.set(v.name, matchingField || v.name);
            });

            this.showMappingModal = true;
          },
          error => {
            console.error("Erreur lors de l'extraction des variables:", error);
            this.toast.showToast('Impossible de charger les variables', 'error');
          },
        );
      },
      error => {
        this.loadingFields = false;
        console.error('Erreur lors du chargement des champs:', error);
        this.toast.showToast('Impossible de charger les champs de contact', 'error');
      },
    );
  }

  /**
   * Trouve la meilleure correspondance automatique entre un nom de variable et les champs disponibles
   */
  findBestMatch(variableName: string, fields: ContactFieldInfo[]): string | null {
    const lowerName = variableName.toLowerCase();

    // Correspondances exactes
    const exactMatch = fields.find(f => f.fieldName.toLowerCase() === lowerName || f.displayName.toLowerCase() === lowerName);

    if (exactMatch) {
      return exactMatch.fieldName;
    }

    // Correspondances partielles
    const partialMatches = {
      nom: 'connom',
      name: 'connom',
      prenom: 'conprenom',
      firstname: 'conprenom',
      tel: 'contelephone',
      telephone: 'contelephone',
      phone: 'contelephone',
      email: 'conemail',
      mail: 'conemail',
      adresse: 'conadresse',
      address: 'conadresse',
      ville: 'conville',
      city: 'conville',
      pays: 'conpays',
      country: 'conpays',
      code: 'concodpostal',
      postal: 'concodpostal',
      zip: 'concodpostal',
      organisation: 'conorganisation',
      organization: 'conorganisation',
      company: 'conorganisation',
      entreprise: 'conorganisation',
      poste: 'conposte',
      position: 'conposte',
      job: 'conposte',
    };

    for (const [key, value] of Object.entries(partialMatches)) {
      if (lowerName.includes(key)) {
        return value;
      }
    }

    return null;
  }

  closeMappingModal(): void {
    this.showMappingModal = false;
    this.variables = [];
    this.variableMappings.clear();
    this.contactFields = [];
  }

  saveMappings(): void {
    if (!this.template) return;

    this.loading = true;

    this.templateService.updateVariableMapping(this.template.id, this.variableMappings).subscribe(
      updatedTemplate => {
        this.loading = false;
        this.template = updatedTemplate;
        this.toast.showToast('Mapping des variables mis à jour avec succès !', 'success');
        this.closeMappingModal();
      },
      error => {
        this.loading = false;
        console.error('Erreur lors de la mise à jour du mapping:', error);
        this.toast.showToast('Erreur lors de la mise à jour du mapping', 'error');
      },
    );
  }

  updateMapping(oldName: string, newName: string): void {
    this.variableMappings.set(oldName, newName);
  }

  getFieldLabel(fieldName: string): string {
    const field = this.contactFields.find(f => f.fieldName === fieldName);
    return field ? field.displayName : fieldName;
  }
  get standardFields(): ContactFieldInfo[] {
    return this.contactFields.filter(f => f.type === 'standard');
  }

  get customFields(): ContactFieldInfo[] {
    return this.contactFields.filter(f => f.type === 'custom');
  }

  get hasCustomFields(): boolean {
    return this.customFields.length > 0;
  }
  formatVariable(name: string, defaultValue: string): string {
    const defaultPart = defaultValue ? ':' + defaultValue : '';
    return `{{${name}${defaultPart}}}`;
  }
  formatVariableDisplay(variableName: string, defaultValue: string): string {
    const mappedName = variableName || '';
    const defaultPart = defaultValue ? ':' + defaultValue : '';
    return `{{${mappedName}${defaultPart}}}`;
  }

  /**
   * Ouvre le modal d'upload de média
   */
  openMediaUploadModal(): void {
    this.showMediaUploadModal = true;
    this.selectedFile = null;
    this.mediaPreviewUrl = null;
    this.selectedMediaType = 'IMAGE';
  }

  /**
   * Ferme le modal d'upload
   */
  closeMediaUploadModal(): void {
    this.showMediaUploadModal = false;
    this.selectedFile = null;
    this.mediaPreviewUrl = null;
    if (this.mediaPreviewUrl) {
      URL.revokeObjectURL(this.mediaPreviewUrl);
    }
  }

  /**
   * Gère la sélection d'un fichier
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      // Valider le type de fichier
      if (!this.isValidFileType(file, this.selectedMediaType)) {
        this.toast.showToast('Type de fichier non valide pour ' + this.selectedMediaType, 'error');
        return;
      }

      // Valider la taille
      const maxSize = this.getMaxFileSize(this.selectedMediaType);
      if (file.size > maxSize) {
        this.toast.showToast(`Fichier trop volumineux. Taille maximale: ${this.formatFileSize(maxSize)}`, 'error');
        return;
      }

      this.selectedFile = file;

      // Créer un aperçu si c'est une image ou vidéo
      if (this.selectedMediaType === 'IMAGE' || this.selectedMediaType === 'VIDEO') {
        if (this.mediaPreviewUrl) {
          URL.revokeObjectURL(this.mediaPreviewUrl);
        }
        this.mediaPreviewUrl = URL.createObjectURL(file);
      }
    }
  }

  /**
   * Upload le média
   */
  uploadMedia(): void {
    if (!this.selectedFile || !this.template) {
      return;
    }

    this.uploadingMedia = true;

    this.templateService.uploadMediaForTemplate(this.template.id, this.selectedFile, this.selectedMediaType).subscribe(
      response => {
        this.uploadingMedia = false;

        if (response.success) {
          // Mettre à jour le template avec le nouveau media_id
          this.template!.code = response.mediaId;

          this.toast.showToast('Média uploadé avec succès !', 'success');
          this.closeMediaUploadModal();
        } else {
          this.toast.showToast("Erreur lors de l'upload du média", 'error');
        }
      },
      error => {
        this.uploadingMedia = false;
        console.error('Erreur upload média:', error);
        this.toast.showToast("Erreur lors de l'upload du média", 'error');
      },
    );
  }

  /**
   * Valide le type de fichier
   */
  private isValidFileType(file: File, mediaType: string): boolean {
    const validTypes: { [key: string]: string[] } = {
      IMAGE: ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
      VIDEO: ['video/mp4', 'video/3gpp'],
      DOCUMENT: [
        'application/pdf',
        'text/plain',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      ],
    };

    return validTypes[mediaType]?.includes(file.type) || false;
  }

  /**
   * Retourne la taille maximale selon le type
   */
  private getMaxFileSize(mediaType: string): number {
    const sizes: { [key: string]: number } = {
      IMAGE: 5 * 1024 * 1024, // 5MB
      VIDEO: 16 * 1024 * 1024, // 16MB
      DOCUMENT: 100 * 1024 * 1024, // 100MB
    };
    return sizes[mediaType] || 5 * 1024 * 1024;
  }

  /**
   * Formate la taille de fichier
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
