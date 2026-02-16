import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Template } from '../template.model';
import { TemplateService } from '../service/template.service';
import { QuillEditorComponent, QuillModule } from 'ngx-quill';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../../core/auth/account.service';
import { ContactService, CustomField } from '../../contact/service/contact.service';
import { ToastComponent } from '../../toast/toast.component';

@Component({
  selector: 'app-template-create',
  templateUrl: './template-create.component.html',
  imports: [CommonModule, ReactiveFormsModule, QuillEditorComponent, FormsModule],
  standalone: true,
})
export class TemplateCreateComponent implements OnInit {
  templateForm: FormGroup;
  isEditMode = false;
  showPreview: boolean = false;
  previewContent: string = '';
  customFields: Record<string, CustomField> = {};

  templateId?: number;
  maxCharactersPerMessage = 160;
  insertedFields: Set<string> = new Set();

  get characterCount(): number {
    const content = this.templateForm.get('content')?.value || '';
    return this.stripHtml(content).length;
  }
  @ViewChild('toast', { static: true }) toast!: ToastComponent;
  protected contactService = inject(ContactService);
  private account = inject(AccountService).trackCurrentAccount();
  @ViewChild('quillEditor', { static: true }) quillEditorComp!: QuillEditorComponent;
  defaultFields = ['nom', 'prenom', 'telephone'];
  // Configuration de l'éditeur Quill
  quillModules = {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ header: 1 }, { header: 2 }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ script: 'sub' }, { script: 'super' }],
      [{ indent: '-1' }, { indent: '+1' }],
      [{ direction: 'rtl' }],
      [{ size: ['small', false, 'large', 'huge'] }],
      [{ color: [] }, { background: [] }],
      [{ font: [] }],
      [{ align: [] }],
      ['clean'],
      ['link', 'image'],
    ],
  };

  // Liste des champs disponibles dans l'objet contact
  availableContactFields: string[] = ['nom', 'prenom', 'telephone'];
  defaultFieldMaxLengths: Record<string, number> = {
    nom: 10,
    prenom: 10,
    telephone: 8,
  };
  get contentHtml(): string {
    return this.templateForm.get('content')?.value || '';
  }

  /** Calcule la longueur brute sans balises HTML */
  stripHtml(html: string): string {
    const div = document.createElement('div');
    div.innerHTML = html;
    return div.textContent || '';
  }

  /** Compte pondéré en remplaçant chaque placeholder par son maxLength */
  get weightedCharacterCount(): number {
    const text = this.stripHtml(this.contentHtml);
    let count = 0;
    let lastIndex = 0;
    const placeholderRe = /\{\{\s*(\w+)\s*\}\}/g;
    let m: RegExpExecArray | null;
    while ((m = placeholderRe.exec(text)) !== null) {
      const [full, key] = m;
      const idx = m.index;
      // ajoute la partie avant le placeholder
      count += idx - lastIndex;
      // détermine la longueur pour ce champ
      const def = this.customFields[key];
      const maxLen = def ? def.maxLength : this.defaultFieldMaxLengths[key] ?? full.length;
      count += maxLen;
      lastIndex = idx + full.length;
    }
    // la partie finale après le dernier placeholder
    count += text.length - lastIndex;
    return count;
  }

  /** Nombre de parties SMS nécessaires */
  get messageCount(): number {
    return Math.max(1, Math.ceil(this.weightedCharacterCount / this.maxCharactersPerMessage));
  }
  constructor(
    private fb: FormBuilder,
    private templateService: TemplateService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.templateForm = this.fb.group({
      name: ['', Validators.required],
      content: ['', Validators.required],
      expediteur: [{ value: this.account()?.expediteur, disabled: true }, Validators.required],
      characterCount: [0],
    });
  }
  loadCustomFields(): void {
    this.contactService.getCustomFields().subscribe({
      next: (fields: Record<string, CustomField>) => {
        console.log('fields', fields);
        this.customFields = fields;

        // Fusion sans doublons
        const customKeys = Object.keys(fields);
        this.availableContactFields = Array.from(new Set([...this.availableContactFields, ...customKeys]));
      },
      error: err => {
        console.error('Erreur lors du chargement des custom fields', err);
      },
    });
  }

  ngOnInit(): void {
    this.loadCustomFields();
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.templateId = +params['id'];
        this.templateService.getTemplate(this.templateId).subscribe(template => {
          this.templateForm.patchValue(template);
        });
      }
    });
  }

  insertContactField(field: string): void {
    const editor = this.quillEditorComp.quillEditor;
    const placeholder = `{{${field}}}`;
    const sel = editor.getSelection();
    const idx = sel ? sel.index : editor.getLength();

    editor.insertText(idx, placeholder);
    editor.setSelection(idx + placeholder.length);

    this.insertedFields.add(field); // afficher l’input pour modifier la taille

    // 🔁 Mettre à jour le champ du formulaire manuellement
    const updatedContent = editor.root.innerHTML;
    this.templateForm.get('content')?.setValue(updatedContent);
  }

  get variables(): FormArray {
    return this.templateForm.get('variables') as FormArray;
  }

  onSubmit(): void {
    if (this.templateForm.invalid) {
      return;
    }

    // Met à jour manuellement le champ characterCount avec le dernier comptage
    const weightedCount = this.weightedCharacterCount;
    this.templateForm.get('characterCount')?.setValue(weightedCount, { emitEvent: false });

    // Prépare les données
    const formData = this.templateForm.getRawValue();

    // Récupère le contenu brut (texte uniquement, sans HTML)
    let content: string = '';
    if (this.quillEditorComp) {
      const editor = this.quillEditorComp.quillEditor;
      content = editor.getText().trim();
    } else {
      content = formData.content;
    }

    formData.content = content;

    console.log('Données du template nettoyées : ', formData);

    const template: Template = formData;
    if (this.isEditMode && this.templateId) {
      template.id = this.templateId;
      this.templateService.updateTemplate(template).subscribe(() => {
        this.router.navigate(['/template']);
      });
    } else {
      this.templateService.createTemplate(template).subscribe(() => {
        this.router.navigate(['/template']);
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/template']);
  }

  togglePreview(): void {
    this.showPreview = !this.showPreview;
    if (this.showPreview) {
      // Récupérer le contenu actuel de l'éditeur
      const content = this.templateForm.get('content')?.value || '';

      // Remplacer les variables par des exemples
      let previewContent = content;
      this.availableContactFields.forEach(field => {
        const placeholder = `{{${field}}}`;
        const exampleValue = this.getExampleValueForField(field);
        previewContent = previewContent.replace(new RegExp(placeholder, 'g'), exampleValue);
      });

      this.previewContent = previewContent;
    }
  }

  // Méthode pour obtenir des exemples de valeurs pour l'aperçu
  getExampleValueForField(field: string): string {
    const examples: { [key: string]: string } = {
      Nom: 'Dupont',
      Prénom: 'Marie',
      Email: 'marie.dupont@exemple.com',
      Téléphone: '06 12 34 56 78',
      Adresse: '123 rue de Paris, 75001 Paris',
      // Ajoutez d'autres exemples selon vos champs disponibles
    };

    return examples[field] || `[Exemple ${field}]`;
  }

  // Méthode pour réinitialiser le formulaire
  resetForm(): void {
    if (this.isEditMode && this.templateId) {
      // Recharger les données originales du template depuis le serveur
      this.templateService.getTemplate(this.templateId).subscribe(template => {
        this.templateForm.patchValue({
          name: template.name,
          content: template.content,
          expediteur: template.expediteur,
        });
      });
    } else {
      // Réinitialiser à vide pour un nouveau template
      this.templateForm.reset();
    }

    // Masquer l'aperçu si affiché
    this.showPreview = false;
  }
}
