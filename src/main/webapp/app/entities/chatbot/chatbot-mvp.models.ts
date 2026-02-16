// chatbot-mvp.models.ts - Modèles cohérents avec le backend

// ================================
// TYPES DE BASE
// ================================
export type NodeType =
  | 'start'
  | 'message'
  | 'buttons'
  | 'list'
  | 'condition'
  | 'variable_set'
  | 'image'
  | 'file'
  | 'webhook'
  | 'input'
  | 'wait_response'
  | 'whatsapp_form'
  | 'api_connector'
  | 'end';

export type ConditionType = 'user_response' | 'variable_value' | 'button_clicked' | 'list_selected';

export type ConditionOperator = 'equals' | 'not_equals' | 'contains' | 'starts_with' | 'custom_expression';
export type VariableOperation = 'set' | 'increment' | 'decrement';

// ================================
// CONFIGURATION FLOW PRINCIPALE
// ================================
export interface FlowConfig {
  partnerId?: number;
  flowId: string;
  name: string;
  description?: string;
  active: boolean;
  nodes: FlowNode[];
  variables: FlowVariable[];
  language: string;
  createdAt?: string;
  updatedAt?: string;
}

// ================================
// NŒUDS DU FLOW
// ================================
export interface FlowNode {
  id: string;
  type: NodeType;
  x: number;
  y: number;
  data: NodeData;
  order: number;
  nextNodeId?: string;
  label?: string;
}

// Dans chatbot-mvp.models.ts - REMPLACER l'interface NodeData par ceci

export interface NodeData {
  // Texte et médias
  text?: string;
  imageUrl?: string;
  fileUrl?: string;
  fileName?: string;
  caption?: string;
  customExpression?: string; // Expression comme "result contains 'x' and result is text"
  expressionDescription?: string;
  waitForUserResponse?: boolean;

  minLength?: number;
  maxLength?: number;

  // Validation de valeur pour nombres
  minValue?: number;
  maxValue?: number;

  // Validation de fichiers
  maxFileSize?: number;
  allowedExtensions?: string;

  // ================================
  // NOUVELLES PROPRIÉTÉS POUR META UPLOAD
  // ================================

  // Meta WhatsApp Business API
  mediaId?: string; // ID du média chez Meta
  originalFilename?: string; // Nom de fichier original
  mimeType?: string; // Type MIME du fichier
  fileSize?: number; // Taille du fichier en bytes

  // États d'upload
  isUploading?: boolean; // Upload en cours
  uploadProgress?: string; // Message de progression
  imagePreview?: string; // Prévisualisation base64 (temporaire)

  // ================================
  // PROPRIÉTÉS EXISTANTES
  // ================================

  // Gestion des réponses utilisateur
  waitForResponse?: boolean;
  storeUserResponse?: boolean;
  storeInVariable?: string;
  responseType?: 'text' | 'number' | 'email' | 'phone' | 'any';

  // Variables dynamiques dans le texte
  useVariables?: boolean;

  // Boutons et listes
  buttons?: NodeButton[];
  items?: NodeListItem[];

  // Conditions
  conditionType?: ConditionType;
  variable?: string;
  operator?: ConditionOperator;
  value?: string;
  conditionalConnections?: ConditionalConnection[];
  defaultNextNodeId?: string;

  // Variables
  variableName?: string;
  variableValue?: string;
  variableOperation?: VariableOperation;

  // Connexions conditionnelles anciennes (compatibilité)
  trueNextNodeId?: string;
  falseNextNodeId?: string;

  // Webhook
  webhookUrl?: string;
  method?: 'POST' | 'GET';

  // Validation
  required?: boolean;
  validationMessage?: string;

  // Timeout et retry
  timeoutSeconds?: number;
  maxRetries?: number;

  apiUrl?: string;
  apiMethod?: ApiMethod;
  apiHeaders?: ApiHeader[];
  apiParameters?: ApiParameter[];

  // Authentification
  authType?: 'none' | 'bearer' | 'basic' | 'api_key';
  authToken?: string;
  authUsername?: string;
  authPassword?: string;
  authApiKey?: string;
  authApiKeyHeader?: string;

  // Corps de la requête
  requestBody?: string;
  requestBodyType?: 'json' | 'form' | 'text' | 'xml';

  // Gestion des réponses
  responseMapping?: ApiResponseMapping[];
  successCondition?: string; // Ex: "status >= 200 && status < 300"
  errorHandling?: 'continue' | 'stop' | 'retry';
  retryCount?: number;
  timeout?: number;

  // Variables de test
  lastTestResult?: ApiTestResult;
  isApiValid?: boolean;

  // Nœuds conditionnels selon le résultat
  successNextNodeId?: string;
  errorNextNodeId?: string;
  formTitle?: string;
  formSubtitle?: string;
  formHeaderImage?: string;
  formFields?: WhatsAppFormField[];
  formLayout?: WhatsAppFormLayout;

  // Configuration du Flow WhatsApp
  whatsappFlowId?: string;
  whatsappFlowConfig?: WhatsAppFlowConfig;

  // Bouton d'action pour ouvrir le formulaire
  ctaText?: string; // "Remplir le formulaire"
  ctaType?: 'flow' | 'url';

  // Gestion des réponses
  submitMessage?: string;
  successMessage?: string;
  errorMessage?: string;

  // Mapping des réponses vers variables
  formResponseMapping?: Array<{
    id: string;
    fieldName: string;
    variableName: string;
    enabled: boolean;
  }>;

  // Validation avancée
  customValidation?: string;
  requiredFields?: string[];

  // Test et debug
  lastFormSubmission?: WhatsAppFormSubmission;
  isFormPublished?: boolean;
  formPreviewUrl?: string;

  // Nœuds conditionnels selon soumission
  formSuccessNextNodeId?: string;
  formErrorNextNodeId?: string;
}
export interface WhatsAppFormField {
  id: string;
  type: 'text' | 'number' | 'email' | 'phone' | 'date' | 'time' | 'dropdown' | 'radio' | 'checkbox' | 'textarea';
  name: string;
  label: string;
  placeholder?: string;
  required: boolean;
  validation?: {
    minLength?: number;
    maxLength?: number;
    pattern?: string;
    errorMessage?: string;
  };
  options?: Array<{
    id: string;
    title: string;
    value: string;
  }>;
  defaultValue?: string;
  enabled: boolean;
}

export interface WhatsAppFormLayout {
  type: 'single_column' | 'two_column';
  header?: {
    title: string;
    subtitle?: string;
    image?: string;
  };
  footer?: {
    text: string;
    links?: Array<{
      text: string;
      url: string;
    }>;
  };
}

export interface WhatsAppFormSubmission {
  flowToken: string;
  responseJson: any;
  userResponses: { [fieldName: string]: any };
  submissionTime: string;
  isValid: boolean;
  validationErrors?: string[];
}

export interface WhatsAppFlowConfig {
  flowId?: string;
  name: string;
  status: 'draft' | 'published' | 'deprecated';
  categories: string[];
  flowJson: any;
  preview?: {
    previewUrl?: string;
    body?: string;
    footer?: string;
    ctaText?: string;
  };
  webhook_url?: string;
}
export interface NodeButton {
  id: string;
  text: string;
  value?: string;
  nextNodeId?: string;
  storeInVariable?: string;
}

export type ApiMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export interface ApiHeader {
  id: string;
  key: string;
  value: string;
  enabled: boolean;
}

export interface ApiParameter {
  id: string;
  key: string;
  value: string;
  type: 'query' | 'body' | 'path';
  enabled: boolean;
}

export interface ApiResponseMapping {
  id: string;
  jsonPath: string; // Ex: "data.user.name" ou "status"
  variableName: string;
  enabled: boolean;
}

export interface ApiTestResult {
  success: boolean;
  status: number;
  statusText: string;
  responseTime: number;
  responseData: any;
  error?: string;
  headers?: { [key: string]: string };
}
export interface NodeListItem {
  id: string;
  title: string;
  description?: string;
  value?: string;
  nextNodeId?: string;
  storeInVariable?: string;
}

export interface ConditionalConnection {
  id: string;
  condition: string;
  nextNodeId: string;
  label?: string;
  operator?: ConditionOperator;
}

// ================================
// VARIABLES
// ================================
export interface FlowVariable {
  name: string;
  value: any;
  type: string;
  description?: string;
  isSystem?: boolean;
}

// ================================
// RÉPONSES API
// ================================
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errors?: string[];
}

export interface SaveFlowResponse {
  flowId: string;
  id: number;
  success: boolean;
  message: string;
}

export interface SaveFlowRequest {
  partnerId?: number;
  flowConfig: FlowConfig;
}

// ================================
// DTOs BACKEND
// ================================
export interface ChatbotFlowDTO {
  id?: number;
  userId?: number;
  name: string;
  description?: string;
  active?: boolean;
  language?: string;
  flowData?: string; // JSON du FlowConfig
  status?: string;
  version?: number;
  createdDate?: string;
  lastModifiedDate?: string;
}

// ================================
// TEST FLOW
// ================================
export interface FlowTestSession {
  id: string;
  currentNodeId: string;
  variables: { [key: string]: any };
  messages: TestMessage[];
  isCompleted: boolean;
}

export interface TestMessage {
  id: string;
  type: 'user' | 'bot';
  content: string;
  timestamp: Date;
  nodeId?: string;
}

// ================================
// DÉFINITIONS TYPES DE NŒUDS
// ================================
export interface NodeTypeDefinition {
  type: NodeType;
  icon: string;
  label: string;
  color: string;
  category: 'message' | 'interaction' | 'logic';
}

// ================================
// CONSTANTES
// ================================
export const NODE_TYPES: NodeTypeDefinition[] = [
  // Messages & Médias
  { type: 'start', icon: 'fas fa-play', label: 'Démarrage', color: 'bg-green-600', category: 'message' },
  { type: 'message', icon: 'fas fa-comment', label: 'Message texte', color: 'bg-blue-500', category: 'message' },
  { type: 'image', icon: 'fas fa-image', label: 'Image', color: 'bg-pink-500', category: 'message' },
  { type: 'file', icon: 'fas fa-file', label: 'Fichier', color: 'bg-orange-500', category: 'message' },

  // Interactions
  { type: 'buttons', icon: 'fas fa-list-ul', label: 'Boutons', color: 'bg-green-500', category: 'interaction' },
  { type: 'list', icon: 'fas fa-bars', label: 'Liste déroulante', color: 'bg-purple-500', category: 'interaction' },
  { type: 'input', icon: 'fas fa-edit', label: 'Saisie utilisateur', color: 'bg-indigo-500', category: 'interaction' },
  { type: 'wait_response', icon: 'fas fa-clock', label: 'Attendre réponse', color: 'bg-teal-500', category: 'interaction' },
  { type: 'whatsapp_form', icon: 'fas fa-wpforms', label: 'Formulaire WhatsApp', color: 'bg-emerald-600', category: 'interaction' }, // ← NOUVEAU

  // Logique
  { type: 'condition', icon: 'fas fa-code-branch', label: 'Condition', color: 'bg-yellow-500', category: 'logic' },
  { type: 'variable_set', icon: 'fas fa-database', label: 'Variable', color: 'bg-cyan-500', category: 'logic' },
  { type: 'webhook', icon: 'fas fa-link', label: 'Webhook (3CX)', color: 'bg-red-500', category: 'logic' },
  { type: 'api_connector', icon: 'fas fa-cloud', label: 'API Connector', color: 'bg-violet-600', category: 'logic' },
  { type: 'end', icon: 'fas fa-stop', label: 'Fin', color: 'bg-red-600', category: 'logic' },
];

export const CONDITION_OPERATORS = [
  { value: 'equals', label: 'Est égal à' },
  { value: 'not_equals', label: "N'est pas égal à" },
  { value: 'contains', label: 'Contient' },
  { value: 'custom_expression', label: 'Expression custom' }, // ← NOUVEAU
];

export const VARIABLE_OPERATIONS = [
  { value: 'set', label: 'Définir valeur' },
  { value: 'increment', label: 'Incrémenter (+1)' },
  { value: 'decrement', label: 'Décrémenter (-1)' },
];

// ================================
// CLASSE UTILITAIRES
// ================================
export class ImprovedFlowUtils {
  static createDefaultNodeData(type: NodeType): NodeData {
    switch (type) {
      case 'start':
        return { text: 'Bienvenue ! Comment puis-je vous aider ?', waitForUserResponse: false };

      case 'message':
        return {
          text: 'Votre message ici...',
          useVariables: false,
        };
      case 'whatsapp_form':
        return {
          text: 'Veuillez remplir ce formulaire:',
          formTitle: 'Informations requises',
          formSubtitle: 'Merci de compléter les champs suivants',
          ctaText: 'Ouvrir le formulaire',
          ctaType: 'flow',
          formFields: [
            {
              id: 'field_1',
              type: 'text',
              name: 'full_name',
              label: 'Nom complet',
              placeholder: 'Votre nom et prénom',
              required: true,
              enabled: true,
            },
            {
              id: 'field_2',
              type: 'email',
              name: 'email',
              label: 'Adresse email',
              placeholder: 'votre@email.com',
              required: true,
              enabled: true,
            },
            {
              id: 'field_3',
              type: 'phone',
              name: 'phone',
              label: 'Numéro de téléphone',
              placeholder: '+33123456789',
              required: false,
              enabled: true,
            },
          ],
          formResponseMapping: [
            {
              id: 'mapping_1',
              fieldName: 'full_name',
              variableName: 'user_name',
              enabled: true,
            },
            {
              id: 'mapping_2',
              fieldName: 'email',
              variableName: 'user_email',
              enabled: true,
            },
            {
              id: 'mapping_3',
              fieldName: 'phone',
              variableName: 'user_phone',
              enabled: true,
            },
          ],
          submitMessage: 'Envoi en cours...',
          successMessage: '✅ Formulaire envoyé avec succès !',
          errorMessage: "❌ Erreur lors de l'envoi. Veuillez réessayer.",
          waitForUserResponse: true,
        };
      case 'api_connector':
        return {
          text: 'Appel API en cours...',
          apiUrl: 'https://api.example.com/endpoint',
          apiMethod: 'GET',
          apiHeaders: [
            {
              id: '1',
              key: 'Content-Type',
              value: 'application/json',
              enabled: true,
            },
          ],
          apiParameters: [],
          authType: 'none',
          requestBodyType: 'json',
          responseMapping: [
            {
              id: '1',
              jsonPath: 'data',
              variableName: 'api_response',
              enabled: true,
            },
          ],
          successCondition: 'status >= 200 && status < 300',
          errorHandling: 'continue',
          timeout: 10000,
          retryCount: 0,
          waitForUserResponse: false,
        };
      case 'buttons':
        return {
          text: 'Choisissez une option:',
          buttons: [
            {
              id: '1',
              text: 'Option 1',
              value: 'option1',
              storeInVariable: 'user_choice',
            },
            {
              id: '2',
              text: 'Option 2',
              value: 'option2',
              storeInVariable: 'user_choice',
            },
          ],
        };

      case 'list':
        return {
          text: 'Sélectionnez dans la liste:',
          waitForUserResponse: true,
          items: [
            {
              id: '1',
              title: 'Option 1',
              value: 'option1',
              storeInVariable: 'user_selection',
            },
            {
              id: '2',
              title: 'Option 2',
              value: 'option2',
              storeInVariable: 'user_selection',
            },
          ],
        };

      case 'input':
        return {
          text: 'Veuillez saisir votre réponse:',
          waitForResponse: true,
          storeUserResponse: true,
          waitForUserResponse: true,
          storeInVariable: 'user_input',
          responseType: 'text',
          required: true,
        };

      case 'wait_response':
        return {
          text: "J'attends votre réponse...",
          waitForResponse: true,
          storeUserResponse: true,
          storeInVariable: 'user_response',
          timeoutSeconds: 300,
        };

      case 'condition':
        return {
          conditionType: 'variable_value',
          variable: '',
          operator: 'equals',
          value: '',
          conditionalConnections: [],
          defaultNextNodeId: '',
        };

      case 'variable_set':
        return {
          variableName: '',
          variableOperation: 'set',
          variableValue: '',
        };

      case 'webhook':
        return {
          text: 'Transfert vers un agent...',
          webhookUrl: '',
          method: 'POST',
        };

      case 'end':
        return {
          text: 'Merci pour votre visite ! À bientôt.',
        };

      default:
        return {};
    }
  }

  // Créer une condition simple
  static createSimpleCondition(sourceNodeId: string, targetNodeId: string, conditionValue: string): ConditionalConnection {
    return {
      id: `conn_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
      condition: conditionValue,
      nextNodeId: targetNodeId,
      label: conditionValue,
      operator: 'equals',
    };
  }

  // Valider un flow
  static validateImprovedFlow(flowConfig: FlowConfig): { isValid: boolean; errors: string[]; warnings: string[] } {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Vérifications de base
    const startNode = flowConfig.nodes.find(n => n.type === 'start');
    if (!startNode) {
      errors.push('Le flow doit avoir un nœud de démarrage');
    }

    // Vérifier les variables utilisées
    const definedVariables = new Set(flowConfig.variables.map(v => v.name));

    flowConfig.nodes.forEach(node => {
      // Vérifier les variables dans les conditions
      if (node.data.variable && !definedVariables.has(node.data.variable)) {
        errors.push(`Nœud ${node.order}: Variable "${node.data.variable}" non définie`);
      }

      // Vérifier les variables de stockage
      if (node.data.storeInVariable && !definedVariables.has(node.data.storeInVariable)) {
        warnings.push(`Nœud ${node.order}: Variable de stockage "${node.data.storeInVariable}" sera créée automatiquement`);
      }

      // Vérifications spécifiques par type
      if (node.type === 'condition' && (!node.data.conditionalConnections || node.data.conditionalConnections.length === 0)) {
        if (!node.data.trueNextNodeId && !node.data.falseNextNodeId) {
          errors.push(`Nœud condition ${node.order}: Aucune connexion conditionnelle définie`);
        }
      }

      if (node.type === 'input' && !node.data.storeInVariable) {
        warnings.push(`Nœud input ${node.order}: Aucune variable de stockage définie`);
      }

      if (node.type === 'webhook' && !node.data.webhookUrl) {
        errors.push(`Nœud webhook ${node.order}: URL du webhook manquante`);
      }
    });

    return {
      isValid: errors.length === 0,
      errors,
      warnings,
    };
  }
}
