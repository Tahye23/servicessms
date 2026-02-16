import { CommonModule } from '@angular/common';
import { Component, inject, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { QuillEditorComponent } from 'ngx-quill';
import { WhatsappService } from '../service/whatsapp.service';

// Interfaces
interface ButtonPayload {
  type: string;
  text: string;
  url?: string;
  phoneNumber?: string;
  copyCode?: string;
}

interface ComponentPayload {
  type: string;
  format?: string;
  text?: string;
  safeText?: SafeHtml | string;
  mediaUrl?: string;
  fileName?: string;
  fileSize?: number;
  mimeType?: string;
  documentName?: string;
  buttons?: ButtonPayload[];
}

interface TemplatePayload {
  name: string;
  language: string;
  category: string;
  components: ComponentPayload[];
}

interface CategoryConfig {
  value: string;
  label: string;
  description: string;
  requiresOTP?: boolean;
  maxButtons?: number;
  allowedButtonTypes?: string[];
}

interface CustomField {
  maxLength: number;
  example?: string;
}

@Component({
  selector: 'jhi-create-template',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, QuillEditorComponent],
  templateUrl: './create-template.component.html',
  styleUrl: './create-template.component.scss',
})
export class CreateTemplateComponent {
  @ViewChildren('editor') quillEditors!: QueryList<QuillEditorComponent>;
  private whatsappService = inject(WhatsappService);
  // Configuration des catégories
  categories: CategoryConfig[] = [
    {
      value: 'MARKETING',
      label: 'Marketing',
      description: 'Templates promotionnels et marketing',
      maxButtons: 10,
      allowedButtonTypes: ['QUICK_REPLY', 'URL', 'PHONE_NUMBER'],
    },
    {
      value: 'AUTHENTICATION',
      label: 'Authentification',
      description: 'Templates pour codes OTP et authentification',
      requiresOTP: true,
      maxButtons: 2,
      allowedButtonTypes: ['QUICK_REPLY', 'URL'],
    },
    {
      value: 'UTILITY',
      label: 'Utilitaire',
      description: 'Templates transactionnels et informatifs',
      maxButtons: 5,
      allowedButtonTypes: ['QUICK_REPLY', 'URL', 'PHONE_NUMBER'],
    },
  ];

  template: TemplatePayload = {
    name: '',
    language: 'en_US',
    category: 'MARKETING',
    components: [
      {
        type: 'HEADER',
        format: 'TEXT',
        text: '',
        safeText: '',
        mediaUrl: '',
        fileName: '',
        fileSize: 0,
        mimeType: '',
        documentName: '',
      },
      { type: 'BODY', text: '', safeText: '' },
      { type: 'FOOTER', text: '', safeText: '' },
      { type: 'BUTTONS', buttons: [], safeText: '' },
    ],
  };

  // Propriétés pour la gestion des champs
  showFieldDialog: boolean = false;
  selectedField: string = '';
  selectedCustomField: CustomField | null = null;
  fieldMaxLength: number = 50;
  fieldExample: string = '';
  loading = false;
  activeFieldTab: 'standard' | 'custom' = 'standard';
  errorMessage: string = '';
  currentEditorIndex = 1;
  customFields: Record<string, CustomField> = {};
  isNameValid = false;

  // Propriétés pour l'authentification OTP
  otpLength: number = 6;
  otpType: 'NUMERIC' | 'ALPHANUMERIC' = 'NUMERIC';

  quillModules = {
    toolbar: [['bold', 'italic', 'strike'], ['clean']],
  };

  constructor(
    private router: Router,
    private sanitizer: DomSanitizer,
  ) {}

  // Getters pour la catégorie actuelle
  get currentCategory(): CategoryConfig {
    return this.categories.find(c => c.value === this.template.category) || this.categories[0];
  }

  get isAuthenticationCategory(): boolean {
    return this.template.category === 'AUTHENTICATION';
  }

  get isUtilityCategory(): boolean {
    return this.template.category === 'UTILITY';
  }

  get maxButtonsAllowed(): number {
    return this.currentCategory.maxButtons || 10;
  }

  get allowedButtonTypes(): string[] {
    return this.currentCategory.allowedButtonTypes || ['QUICK_REPLY', 'URL', 'PHONE_NUMBER'];
  }

  // Méthodes pour la gestion des catégories
  onCategoryChange(category: string): void {
    this.template.category = category;
    this.errorMessage = '';

    // Ajuster les boutons selon la catégorie
    const buttonComponent = this.template.components.find(c => c.type === 'BUTTONS');
    if (buttonComponent && buttonComponent.buttons) {
      // Limiter le nombre de boutons
      if (buttonComponent.buttons.length > this.maxButtonsAllowed) {
        buttonComponent.buttons = buttonComponent.buttons.slice(0, this.maxButtonsAllowed);
      }

      // Filtrer les types de boutons non autorisés
      buttonComponent.buttons = buttonComponent.buttons.filter(btn => this.allowedButtonTypes.includes(btn.type));
    }

    // Pour la catégorie AUTHENTICATION, ajouter automatiquement le placeholder OTP
    if (this.isAuthenticationCategory) {
      this.addOTPPlaceholder();
    }
  }

  // Ajouter automatiquement le placeholder OTP pour l'authentification
  addOTPPlaceholder(): void {
    const bodyComponent = this.template.components.find(c => c.type === 'BODY');
    if (bodyComponent && bodyComponent.text && !bodyComponent.text.includes('{{otp}}')) {
      bodyComponent.text += '\n\nVotre code de vérification: {{otp}}';
      bodyComponent.safeText = this.sanitizer.bypassSecurityTrustHtml(bodyComponent.text);
    }
  }

  getCustomFieldsArray(): { key: string; value: CustomField }[] {
    return Object.entries(this.customFields).map(([key, value]) => ({ key, value }));
  }

  openFieldDialog(field: string, customField: CustomField | null): void {
    this.selectedField = field;
    this.selectedCustomField = customField;
    this.fieldMaxLength = customField ? customField.maxLength : 50;
    this.fieldExample = this.getDefaultExample(field);
    this.showFieldDialog = true;
  }

  onNameChange(value: string): void {
    const pattern = /^[a-z0-9_]+$/;
    this.isNameValid = pattern.test(value);
  }

  getDefaultExample(field: string): string {
    const examples: Record<string, string> = {
      nom: 'Dupont',
      prenom: 'Jean',
      telephone: '41956236',
      otp: '123456',
      order_id: 'ORD-12345',
      amount: '150.00',
      date: '15/01/2025',
      status: 'confirmée',
    };

    return examples[field] || 'Exemple';
  }

  getPlaceholderExample(field: string): string {
    return `Ex: ${this.getDefaultExample(field)}`;
  }

  insertConfiguredField(): void {
    const idx = this.currentEditorIndex;
    let placeholder: string;

    // Pour l'authentification, utiliser un format spécial pour OTP
    if (this.isAuthenticationCategory && this.selectedField === 'otp') {
      placeholder = `{{otp}}`;
    } else {
      placeholder = `{{${this.selectedField}:${this.fieldExample}}}`;
    }

    const editorComp = this.quillEditors.toArray()[idx];
    if (!editorComp) return;

    const quill = editorComp.quillEditor;
    const sel = quill.getSelection() || { index: quill.getLength(), length: 0 };
    quill.insertText(sel.index, placeholder);
    quill.setSelection(sel.index + placeholder.length);

    this.template.components[idx].text = quill.root.innerHTML;
    this.showFieldDialog = false;
  }

  addButton(component: any): void {
    if (component.buttons.length >= this.maxButtonsAllowed) {
      this.errorMessage = `Maximum ${this.maxButtonsAllowed} boutons autorisés pour cette catégorie.`;
      return;
    }

    const defaultButtonType = this.allowedButtonTypes[0] || 'QUICK_REPLY';
    component.buttons.push({
      type: defaultButtonType,
      text: '',
    });
  }

  removeButton(component: any, buttonIndex: number): void {
    if (component.buttons && component.buttons.length > buttonIndex) {
      component.buttons.splice(buttonIndex, 1);
    }
  }

  onchangeHeader(index: number): void {
    this.currentEditorIndex = index;
    this.errorMessage = '';
    const raw = this.template.components[index].text;
    this.template.components[index].safeText = this.sanitizer.bypassSecurityTrustHtml(raw || '');
  }

  goBack(): void {
    this.router.navigate(['/template']);
  }

  // Méthodes utilitaires
  getFileAcceptType(format: string): string {
    switch (format) {
      case 'IMAGE':
        return 'image/*';
      case 'VIDEO':
        return 'video/mp4,video/3gpp,video/quicktime';
      case 'DOCUMENT':
        // ✅ MISE À JOUR: Inclure les types audio dans DOCUMENT
        return '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.mp3,.aac,.m4a,.amr,.ogg,.opus';
      default:
        return '*/*';
    }
  }

  formatFileSize(bytes: number): string {
    if (!bytes) return '0 B';

    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getButtonTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      QUICK_REPLY: 'Réponse rapide',
      URL: 'Lien URL',
      PHONE_NUMBER: 'Numéro de téléphone',
      COPY_CODE: 'Copier code',
    };

    return labels[type] || type;
  }

  saveTemplate(): void {
    if (!this.isNameValid) {
      this.errorMessage = '⛔ Nom du template requis et valide.';
      return;
    }

    const body = this.template.components.find(c => c.type === 'BODY');
    if (!body?.text?.trim()) {
      this.errorMessage = 'Le corps (BODY) est requis.';
      return;
    }

    // Validation métier
    if (!this.validateMetaCompliance()) {
      return;
    }

    this.errorMessage = '';
    const payload = this.buildTemplatePayload();

    this.loading = true;

    // ✅ APPEL RÉEL AU BACKEND
    this.whatsappService.createTemplate(payload).subscribe({
      next: response => {
        this.loading = false;
        console.log('Template créé avec succès:', response);

        // Afficher un message de succès
        this.showSuccessMessage('Template créé avec succès!');

        // Rediriger après un court délai
        setTimeout(() => {
          this.router.navigate(['/template']);
        }, 1500);
      },
      error: error => {
        this.loading = false;
        console.error('Erreur lors de la création du template:', error);

        // Gérer les différents types d'erreurs
        this.handleApiError(error);
      },
    });
  }

  // ✅ NOUVELLE MÉTHODE: Gestion des erreurs API
  private handleApiError(error: any): void {
    if (error.status === 400) {
      // Erreur de validation
      if (error.error?.error?.error_user_msg) {
        this.errorMessage = error.error.error.error_user_msg;
      } else if (error.error?.message) {
        this.errorMessage = error.error.message;
      } else {
        this.errorMessage = 'Erreur de validation des données.';
      }
    } else if (error.status === 401) {
      this.errorMessage = 'Session expirée. Veuillez vous reconnecter.';
      // Optionnel: rediriger vers la page de login
      // this.router.navigate(['/login']);
    } else if (error.status === 403) {
      this.errorMessage = 'Accès non autorisé.';
    } else if (error.status === 500) {
      this.errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
    } else if (error.status === 0) {
      this.errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
    } else {
      this.errorMessage = `Erreur inattendue: ${error.message || 'Veuillez réessayer.'}`;
    }
  }

  // ✅ NOUVELLE MÉTHODE: Afficher message de succès
  private showSuccessMessage(message: string): void {
    // Créer un message de succès temporaire
    const successElement = document.createElement('div');
    successElement.className = 'fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
    successElement.innerHTML = `
    <div class="flex items-center">
      <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
      </svg>
      ${message}
    </div>
  `;

    document.body.appendChild(successElement);

    // Supprimer après 3 secondes
    setTimeout(() => {
      if (successElement.parentNode) {
        successElement.parentNode.removeChild(successElement);
      }
    }, 3000);
  }

  // ✅ AMÉLIORATION: Validation de taille par type de média
  onHeaderMediaSelect(event: Event, comp: any): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];

    // ✅ DÉTECTION AUTOMATIQUE: Si c'est un fichier audio, forcer le format DOCUMENT
    if (file.type.startsWith('audio/')) {
      console.log('🎵 Fichier audio détecté - conversion automatique en DOCUMENT');
      comp.format = 'DOCUMENT';
      comp.audioFile = true; // Flag pour l'affichage spécial
    }

    // Validation de taille selon le format (après conversion éventuelle)
    const maxSizes: Record<string, number> = {
      IMAGE: 5 * 1024 * 1024, // 5MB
      VIDEO: 16 * 1024 * 1024, // 16MB
      AUDIO: 16 * 1024 * 1024, // 16MB (pour compatibilité)
      DOCUMENT: 100 * 1024 * 1024, // 100MB
    };

    const maxSize = maxSizes[comp.format] || 16 * 1024 * 1024;

    if (file.size > maxSize) {
      this.errorMessage = `Le fichier est trop volumineux. Taille maximale: ${this.formatFileSize(maxSize)}`;
      return;
    }

    // Validation du type de fichier (mise à jour pour supporter audio comme document)
    if (!this.isValidFileTypeUpdated(file, comp.format, comp.audioFile)) {
      this.errorMessage = `Type de fichier non supporté pour ${comp.format.toLowerCase()}.`;
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      comp.mediaUrl = reader.result as string;
      comp.fileName = file.name;
      comp.fileSize = file.size;
      comp.mimeType = file.type;

      // ✅ AFFICHAGE: Message informatif pour les fichiers audio
      if (file.type.startsWith('audio/')) {
        this.showAudioConversionInfo();
      }

      // Effacer les erreurs
      this.errorMessage = '';
    };

    reader.onerror = () => {
      this.errorMessage = 'Erreur lors de la lecture du fichier.';
    };

    reader.readAsDataURL(file);
  }
  private isValidFileTypeUpdated(file: File, format: string, isAudioFile?: boolean): boolean {
    const allowedTypes: Record<string, string[]> = {
      IMAGE: ['image/jpeg', 'image/png', 'image/webp'],
      VIDEO: ['video/mp4', 'video/3gpp', 'video/quicktime'],
      DOCUMENT: [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain',
        // ✅ AJOUT: Types audio supportés comme documents
        'audio/aac',
        'audio/mp4',
        'audio/amr',
        'audio/mpeg',
        'audio/mp3',
        'audio/ogg',
        'audio/opus',
      ],
    };

    const allowed = allowedTypes[format] || [];
    return allowed.includes(file.type);
  }
  private showAudioConversionInfo(): void {
    // Afficher un message temporaire
    const infoElement = document.createElement('div');
    infoElement.className = 'fixed top-4 right-4 bg-blue-500 text-white px-6 py-3 rounded-lg shadow-lg z-50';
    infoElement.innerHTML = `
    <div class="flex items-center">
      <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
      </svg>
      🎵 Fichier audio converti en document téléchargeable
    </div>
  `;

    document.body.appendChild(infoElement);

    setTimeout(() => {
      if (infoElement.parentNode) {
        infoElement.parentNode.removeChild(infoElement);
      }
    }, 4000);
  }
  isAudioFile(component: any): boolean {
    // Vérifier le flag audioFile ou le type MIME
    if (component.audioFile) return true;

    if (!component.mediaUrl) return false;

    const mimeType = component.mediaUrl.split(',')[0].split(':')[1].split(';')[0];
    return mimeType.startsWith('audio/');
  }
  // ✅ NOUVELLE MÉTHODE: Validation du type de fichier
  private isValidFileType(file: File, format: string): boolean {
    const allowedTypes: Record<string, string[]> = {
      IMAGE: ['image/jpeg', 'image/png', 'image/webp'],
      VIDEO: ['video/mp4', 'video/3gpp', 'video/quicktime'],
      AUDIO: ['audio/aac', 'audio/mp4', 'audio/amr', 'audio/mpeg', 'audio/mp3', 'audio/ogg'],
      DOCUMENT: [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain',
      ],
    };

    const allowed = allowedTypes[format] || [];
    return allowed.includes(file.type);
  }

  validateMetaCompliance(): boolean {
    const bodyText = this.template.components[1].text ?? '';

    // Validation spécifique par catégorie
    if (!this.validateCategoryRequirements()) {
      return false;
    }

    this.errorMessage = '';
    return true;
  }

  validateCategoryRequirements(): boolean {
    const bodyText = this.template.components[1].text || '';

    switch (this.template.category) {
      case 'AUTHENTICATION':
        // Vérifier que le template contient un placeholder OTP
        if (!bodyText.includes('{{otp}}') && !bodyText.includes('{{1}}')) {
          this.errorMessage = "Les templates d'authentification doivent contenir un placeholder pour le code OTP.";
          return false;
        }

        // Vérifier que le message est suffisamment court pour l'authentification
        if (bodyText.length > 500) {
          this.errorMessage = "Les templates d'authentification doivent être concis (max 500 caractères).";
          return false;
        }
        break;

      case 'UTILITY':
        // Vérifier que le template a un but informatif clair
        if (bodyText.length < 20) {
          this.errorMessage = "Les templates utilitaires doivent contenir suffisamment d'informations.";
          return false;
        }
        break;

      /*case 'MARKETING':
        // Validation spécifique au marketing
        const buttonComponent = this.template.components.find(c => c.type === 'BUTTONS');
        if (!buttonComponent || !buttonComponent.buttons || buttonComponent.buttons.length === 0) {
          this.errorMessage = 'Les templates marketing doivent inclure au moins un bouton d\'action.';
          return false;
        }
        break;*/
    }

    return true;
  }

  private buildTemplatePayload(): any {
    const payload: any = {
      name: this.template.name,
      language: this.template.language,
      category: this.template.category,
      components: this.template.components.filter(this.isValidComponent).map(this.formatComponent),
    };

    // Ajouter des métadonnées spécifiques pour l'authentification
    if (this.isAuthenticationCategory) {
      payload.authentication = {
        otpLength: this.otpLength,
        otpType: this.otpType,
      };
    }

    return payload;
  }

  private isValidComponent(c: any): boolean {
    switch (c.type) {
      case 'HEADER':
        return c.format === 'TEXT' ? !!c.text?.trim() : !!c.mediaUrl;
      case 'FOOTER':
        return !!c.text?.trim();
      case 'BUTTONS':
        return Array.isArray(c.buttons) && c.buttons.length > 0;
      default:
        return true;
    }
  }

  private formatComponent(c: any): any {
    const out: any = { type: c.type };

    if (c.type === 'HEADER') {
      out.format = c.format;
      if (c.format === 'TEXT') {
        out.text = c.text!.trim();
      } else if (['IMAGE', 'VIDEO', 'AUDIO'].includes(c.format)) {
        out.mediaUrl = c.mediaUrl;
        if (c.fileName) out.fileName = c.fileName;
        if (c.fileSize) out.fileSize = c.fileSize;
      } else if (c.format === 'DOCUMENT') {
        out.mediaUrl = c.mediaUrl;
        out.documentName = c.documentName;
      }
    }

    if (c.type === 'BODY' || c.type === 'FOOTER') {
      out.text = c.text!.trim();
    }

    if (c.type === 'BUTTONS') {
      out.buttons = c.buttons!.map((b: ButtonPayload) => ({
        type: b.type,
        text: b.text,
        ...(b.url ? { url: b.url } : {}),
        ...(b.phoneNumber ? { phone_number: b.phoneNumber } : {}),
      }));
    }

    return out;
  }
}
