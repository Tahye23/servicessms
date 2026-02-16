import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { TemplateService } from '../service/template.service';
import { NgClass, NgForOf, NgIf } from '@angular/common';

export interface ComponentData {
  type: string;
  format?: string;
  text?: string;
  mediaUrl?: string;
  buttons?: Array<{ text: string; url?: string }>;
}

export interface VariableDTO {
  ordre: number;
  valeur: string;
  type: string;
}

@Component({
  selector: 'app-template-renderer',
  templateUrl: './template-renderer.component.html',
  standalone: true,
  imports: [NgClass, NgIf, NgForOf],
})
export class TemplateRendererComponent implements OnInit, OnChanges {
  /**
   * ID du template. Peut être string, number ou null.
   */
  @Input() templateId?: number | null;
  /**
   * Variables optionnelles sous forme JSON ou tableau.
   */
  @Input() variables: string | VariableDTO[] | null | undefined;

  contentData: { components: ComponentData[] } = { components: [] };

  constructor(private templateService: TemplateService) {}

  ngOnInit(): void {
    this.loadAndRender();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.templateId || changes.variables) {
      this.loadAndRender();
    }
  }

  private loadAndRender(): void {
    if (this.templateId == null) {
      // aucune donnée, on vide
      this.contentData = { components: [] };
      return;
    }
    this.templateService.getTemplate(this.templateId).subscribe(
      template => {
        try {
          this.contentData = JSON.parse(template.content);
          this.applyVariables();
        } catch (e) {
          console.error('Invalid JSON content:', e);
          this.contentData = { components: [] };
        }
      },
      err => {
        console.error('Error loading template:', err);
        this.contentData = { components: [] };
      },
    );
  }

  private applyVariables(): void {
    // Récupérer tableau de variables, ou vide si none
    let varsArray: VariableDTO[] = [];
    if (this.variables) {
      if (typeof this.variables === 'string') {
        try {
          varsArray = JSON.parse(this.variables) as VariableDTO[];
        } catch (e) {
          console.error('Invalid variables JSON:', e);
        }
      } else {
        varsArray = this.variables;
      }
    }

    // Appliquer suivant ordre et type
    varsArray
      .sort((a, b) => a.ordre - b.ordre)
      .forEach(v => {
        // Filtrer composants par type
        const comps = this.contentData.components.filter(c => c.type.toUpperCase() === v.type.toUpperCase());
        const comp = comps.length > v.ordre - 1 ? comps[v.ordre - 1] : null;
        if (comp && comp.text) {
          comp.text = comp.text.replace(/\{\{\s*[^:}]+(?:[^}]*)?\s*\}\}/, v.valeur);
        }
      });
  }
}
