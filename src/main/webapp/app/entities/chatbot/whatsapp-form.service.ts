import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { NodeData, WhatsAppFlowConfig, WhatsAppFormField, WhatsAppFormSubmission } from './chatbot-mvp.models';

@Injectable({
  providedIn: 'root',
})
export class WhatsAppFormService {
  private readonly WHATSAPP_API_BASE = 'https://graph.facebook.com/v18.0';
  private readonly ACCESS_TOKEN = 'YOUR_WHATSAPP_ACCESS_TOKEN'; // À remplacer

  constructor(private http: HttpClient) {}

  /**
   * Créer un Flow WhatsApp
   */
  createWhatsAppFlow(formConfig: WhatsAppFlowConfig): Observable<any> {
    const url = `${this.WHATSAPP_API_BASE}/YOUR_BUSINESS_ID/flows`;

    const headers = new HttpHeaders({
      Authorization: `Bearer ${this.ACCESS_TOKEN}`,
      'Content-Type': 'application/json',
    });

    const payload = {
      name: formConfig.name,
      categories: formConfig.categories || ['OTHER'],
      flow_json: formConfig.flowJson,
    };

    return this.http.post(url, payload, { headers }).pipe(
      map((response: any) => {
        console.log('✅ Flow WhatsApp créé:', response);
        return response;
      }),
      catchError(error => {
        console.error('❌ Erreur création Flow:', error);
        return throwError(error);
      }),
    );
  }

  /**
   * Publier un Flow WhatsApp
   */
  publishWhatsAppFlow(flowId: string): Observable<any> {
    const url = `${this.WHATSAPP_API_BASE}/${flowId}/publish`;

    const headers = new HttpHeaders({
      Authorization: `Bearer ${this.ACCESS_TOKEN}`,
      'Content-Type': 'application/json',
    });

    return this.http.post(url, {}, { headers }).pipe(
      map((response: any) => {
        console.log('✅ Flow publié:', response);
        return response;
      }),
      catchError(error => {
        console.error('❌ Erreur publication Flow:', error);
        return throwError(error);
      }),
    );
  }

  /**
   * Générer le JSON du Flow WhatsApp à partir des champs du formulaire
   */
  generateFlowJson(nodeData: NodeData): any {
    const formFields = nodeData.formFields || [];

    // Structure de base du Flow WhatsApp
    const flowJson = {
      version: '5.0',
      screens: [
        {
          id: 'FORM_SCREEN',
          title: nodeData.formTitle || 'Formulaire',
          terminal: true,
          data: {},
          layout: {
            type: 'SingleColumnLayout',
            children: this.generateFormChildren(formFields),
          },
        },
      ],
    };

    return flowJson;
  }

  /**
   * Générer les composants du formulaire
   */
  private generateFormChildren(formFields: WhatsAppFormField[]): any[] {
    const children: any[] = [];

    formFields
      .filter(field => field.enabled)
      .forEach(field => {
        const component = this.generateFieldComponent(field);
        if (component) {
          children.push(component);
        }
      });

    // Ajouter le bouton de soumission
    children.push({
      type: 'Footer',
      label: 'Envoyer',
      'on-click-action': {
        name: 'complete',
        payload: {},
      },
    });

    return children;
  }
  hasValidAccessToken(): boolean {
    return this.ACCESS_TOKEN && this.ACCESS_TOKEN !== 'YOUR_WHATSAPP_ACCESS_TOKEN';
  }

  /**
   * Vérifier si un compte Business est configuré
   */
  hasBusinessAccount(): boolean {
    // Logique à implémenter selon votre configuration
    return true; // Par défaut true pour les tests
  }

  /**
   * Tester la connexion à l'API WhatsApp
   */
  testConnection(): Observable<{ success: boolean; message: string }> {
    const url = `${this.WHATSAPP_API_BASE}/me`;

    const headers = new HttpHeaders({
      Authorization: `Bearer ${this.ACCESS_TOKEN}`,
    });

    return this.http.get(url, { headers }).pipe(
      map(() => ({
        success: true,
        message: 'Connexion WhatsApp Business réussie',
      })),
      catchError(error => {
        console.error('❌ Erreur test connexion:', error);
        return throwError({
          success: false,
          message: `Échec connexion: ${error.status} ${error.statusText}`,
        });
      }),
    );
  }
  /**
   * Générer un composant de champ selon son type
   */
  private generateFieldComponent(field: WhatsAppFormField): any {
    const baseComponent = {
      name: field.name,
      label: field.label,
      required: field.required,
    };

    switch (field.type) {
      case 'text':
      case 'email':
      case 'phone':
        return {
          type: 'TextInput',
          ...baseComponent,
          'input-type': field.type === 'email' ? 'email' : field.type === 'phone' ? 'phone' : 'text',
          'helper-text': field.placeholder || '',
          validation: this.generateValidation(field),
        };

      case 'number':
        return {
          type: 'TextInput',
          ...baseComponent,
          'input-type': 'number',
          'helper-text': field.placeholder || '',
        };

      case 'textarea':
        return {
          type: 'TextArea',
          ...baseComponent,
          'helper-text': field.placeholder || '',
        };

      case 'dropdown':
        return {
          type: 'Dropdown',
          ...baseComponent,
          'data-source':
            field.options?.map(option => ({
              id: option.id,
              title: option.title,
            })) || [],
        };

      case 'radio':
        return {
          type: 'RadioButtonsGroup',
          ...baseComponent,
          'data-source':
            field.options?.map(option => ({
              id: option.id,
              title: option.title,
            })) || [],
        };

      case 'checkbox':
        return {
          type: 'CheckboxGroup',
          ...baseComponent,
          'data-source':
            field.options?.map(option => ({
              id: option.id,
              title: option.title,
            })) || [],
        };

      case 'date':
        return {
          type: 'DatePicker',
          ...baseComponent,
        };

      default:
        return null;
    }
  }

  /**
   * Générer les règles de validation
   */
  private generateValidation(field: WhatsAppFormField): any {
    if (!field.validation) return undefined;

    const validation: any = {};

    if (field.validation.minLength) {
      validation['min-chars'] = field.validation.minLength;
    }

    if (field.validation.maxLength) {
      validation['max-chars'] = field.validation.maxLength;
    }

    if (field.validation.pattern) {
      validation.regex = field.validation.pattern;
    }

    if (field.validation.errorMessage) {
      validation['error-message'] = field.validation.errorMessage;
    }

    return Object.keys(validation).length > 0 ? validation : undefined;
  }

  /**
   * Envoyer un message avec bouton Flow
   */
  sendFlowMessage(phoneNumber: string, nodeData: NodeData): Observable<any> {
    const url = `${this.WHATSAPP_API_BASE}/YOUR_PHONE_NUMBER_ID/messages`;

    const headers = new HttpHeaders({
      Authorization: `Bearer ${this.ACCESS_TOKEN}`,
      'Content-Type': 'application/json',
    });

    const payload = {
      messaging_product: 'whatsapp',
      to: phoneNumber,
      type: 'interactive',
      interactive: {
        type: 'flow',
        header: nodeData.formHeaderImage
          ? {
              type: 'image',
              image: {
                link: nodeData.formHeaderImage,
              },
            }
          : undefined,
        body: {
          text: nodeData.text || 'Veuillez remplir ce formulaire',
        },
        footer: nodeData.formSubtitle
          ? {
              text: nodeData.formSubtitle,
            }
          : undefined,
        action: {
          name: 'flow',
          parameters: {
            flow_message_version: '3',
            flow_id: nodeData.whatsappFlowId,
            flow_cta: nodeData.ctaText || 'Ouvrir le formulaire',
            flow_action: 'navigate',
            flow_action_payload: {
              screen: 'FORM_SCREEN',
              data: {},
            },
          },
        },
      },
    };

    return this.http.post(url, payload, { headers }).pipe(
      map((response: any) => {
        console.log('✅ Message Flow envoyé:', response);
        return response;
      }),
      catchError(error => {
        console.error('❌ Erreur envoi Flow:', error);
        return throwError(error);
      }),
    );
  }

  /**
   * Traiter une réponse de formulaire reçue
   */
  processFormSubmission(webhookData: any): WhatsAppFormSubmission {
    try {
      const flowData = webhookData.entry?.[0]?.changes?.[0]?.value?.messages?.[0]?.interactive?.nfm_reply;

      if (!flowData) {
        throw new Error('Données de formulaire non trouvées');
      }

      const submission: WhatsAppFormSubmission = {
        flowToken: flowData.flow_token || '',
        responseJson: flowData.response_json || {},
        userResponses: this.extractUserResponses(flowData.response_json),
        submissionTime: new Date().toISOString(),
        isValid: true,
        validationErrors: [],
      };

      console.log('📋 Soumission formulaire traitée:', submission);
      return submission;
    } catch (error: any) {
      console.error('❌ Erreur traitement soumission:', error);
      return {
        flowToken: '',
        responseJson: {},
        userResponses: {},
        submissionTime: new Date().toISOString(),
        isValid: false,
        validationErrors: [error.message],
      };
    }
  }

  /**
   * Extraire les réponses utilisateur du JSON
   */
  private extractUserResponses(responseJson: any): { [fieldName: string]: any } {
    const responses: { [fieldName: string]: any } = {};

    try {
      if (responseJson && typeof responseJson === 'object') {
        Object.keys(responseJson).forEach(key => {
          responses[key] = responseJson[key];
        });
      }
    } catch (error) {
      console.warn('⚠️ Erreur extraction réponses:', error);
    }

    return responses;
  }

  /**
   * Valider une configuration de formulaire
   */
  validateFormConfiguration(nodeData: NodeData): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    // Titre obligatoire
    if (!nodeData.formTitle || !nodeData.formTitle.trim()) {
      errors.push('Titre du formulaire obligatoire');
    }

    // Au moins un champ
    const enabledFields = (nodeData.formFields || []).filter(f => f.enabled);
    if (enabledFields.length === 0) {
      errors.push('Au moins un champ doit être activé');
    }

    // Validation des champs
    enabledFields.forEach((field, index) => {
      if (!field.name || !field.name.trim()) {
        errors.push(`Champ ${index + 1}: Nom obligatoire`);
      }
      if (!field.label || !field.label.trim()) {
        errors.push(`Champ ${index + 1}: Label obligatoire`);
      }
      if (
        (field.type === 'dropdown' || field.type === 'radio' || field.type === 'checkbox') &&
        (!field.options || field.options.length === 0)
      ) {
        errors.push(`Champ ${index + 1}: Options obligatoires pour le type ${field.type}`);
      }
    });

    // Validation du mapping
    if (nodeData.formResponseMapping) {
      nodeData.formResponseMapping
        .filter(mapping => mapping.enabled)
        .forEach((mapping, index) => {
          if (!mapping.fieldName) {
            errors.push(`Mapping ${index + 1}: Nom de champ manquant`);
          }
          if (!mapping.variableName) {
            errors.push(`Mapping ${index + 1}: Nom de variable manquant`);
          }
        });
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  /**
   * Générer un aperçu du formulaire
   */
  generateFormPreview(nodeData: NodeData): string {
    const fields = (nodeData.formFields || []).filter(f => f.enabled);

    let preview = `📋 **${nodeData.formTitle}**\n`;
    if (nodeData.formSubtitle) {
      preview += `${nodeData.formSubtitle}\n\n`;
    }

    fields.forEach((field, index) => {
      const required = field.required ? '*' : '';
      preview += `${index + 1}. ${field.label}${required}\n`;
      preview += `   Type: ${this.getFieldTypeLabel(field.type)}\n`;
      if (field.placeholder) {
        preview += `   Placeholder: ${field.placeholder}\n`;
      }
      if (field.options && field.options.length > 0) {
        preview += `   Options: ${field.options.map(o => o.title).join(', ')}\n`;
      }
      preview += '\n';
    });

    return preview;
  }

  /**
   * Obtenir le label d'un type de champ
   */
  private getFieldTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      text: 'Texte',
      number: 'Nombre',
      email: 'Email',
      phone: 'Téléphone',
      date: 'Date',
      time: 'Heure',
      textarea: 'Zone de texte',
      dropdown: 'Liste déroulante',
      radio: 'Boutons radio',
      checkbox: 'Cases à cocher',
    };
    return labels[type] || type;
  }
}
