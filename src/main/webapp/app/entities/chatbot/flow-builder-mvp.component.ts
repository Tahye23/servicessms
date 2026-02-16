// flow-builder-mvp.component.ts - VERSION FINALE CORRIGÉE
import { Component, ElementRef, ViewChild, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { retry, takeUntil, timeout } from 'rxjs/operators';
import {
  FlowConfig,
  FlowNode,
  FlowVariable,
  NODE_TYPES,
  NodeType,
  NodeButton,
  NodeListItem,
  ConditionalConnection,
  ImprovedFlowUtils,
  ApiTestResult,
  ApiResponseMapping,
  WhatsAppFormField,
  WhatsAppFlowConfig,
} from './chatbot-mvp.models';
import { ChatbotMvpService } from './chatbot-mvp.service';
import { ApiConnectorService } from './api-connector.service';
import { WhatsAppFormService } from './whatsapp-form.service';

@Component({
  selector: 'app-flow-builder-mvp',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './flow-builder-mvp.component.html',
  styleUrls: ['./flow-builder-mvp.component.scss'],
})
export class FlowBuilderMvpComponent implements OnInit, OnDestroy {
  @ViewChild('canvas', { static: true }) canvasRef!: ElementRef;

  // État du composant
  private destroy$ = new Subject<void>();
  private toastTimeout: any;

  private showToast(type: 'success' | 'error' | 'warning' | 'info', message: string, duration = 3000): void {
    // Supprimer l'ancien toast
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }

    // Afficher le nouveau toast
    this.currentToast = { type, message, visible: true };

    // Masquer après délai
    this.toastTimeout = setTimeout(() => {
      this.currentToast = null;
    }, duration);
  }

  // Dans les propriétés du composant, ajouter :
  currentToast: { type: string; message: string; visible: boolean } | null = null;
  // Configuration du flow
  flowConfig: FlowConfig = {
    partnerId: 1,
    flowId: '',
    name: 'Nouveau Flow',
    active: false,
    nodes: [],
    variables: [],
    language: 'fr',
  };
  selectedNode: FlowNode | null = null;

  // Dans flow-builder-mvp.component.ts - Variables déjà présentes, vérifiez qu'elles existent
  canvasZoom = 1;
  canvasMinZoom = 0.2;
  canvasMaxZoom = 3;
  canvasOffsetX = 0;
  canvasOffsetY = 0;
  isPanning = false;
  panStartX = 0;
  panStartY = 0;
  // État UI
  nodes: FlowNode[] = [];
  selectedNodeId: string | null = null;
  isDragging = false;
  isConnecting = false;
  dragStartX = 0;
  dragStartY = 0;
  dragNodeId: string | null = null;
  connectionStartNodeId: string | null = null;

  // Loading states
  isLoading = false;
  isSaving = false;

  // Types de nœuds disponibles
  nodeTypes = NODE_TYPES;

  constructor(
    private whatsappFormService: WhatsAppFormService,
    private chatbotService: ChatbotMvpService,
    private apiConnectorService: ApiConnectorService,
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
    this.setupEventListeners();

    // AJOUTER CES LIGNES
    this.loadCurrentUserFlow().then(() => {
      // S'assurer que les nœuds sont visibles après chargement
      if (this.nodes.length > 0) {
        this.centerViewOnNodes();
      }
    });
  }
  // NOUVELLE MÉTHODE À AJOUTER
  centerViewOnNodes(): void {
    if (this.nodes.length === 0) return;

    // Trouver le centre de tous les nœuds
    const avgX = this.nodes.reduce((sum, node) => sum + node.x, 0) / this.nodes.length;
    const avgY = this.nodes.reduce((sum, node) => sum + node.y, 0) / this.nodes.length;

    // Centrer la vue sur ce point (AVEC l'offset de 5000)
    const canvasRect = document.querySelector('.flex-1.relative.overflow-hidden');
    if (canvasRect) {
      const rect = canvasRect.getBoundingClientRect();
      this.canvasOffsetX = -((avgX + 5000) * this.canvasZoom) + rect.width / 2;
      this.canvasOffsetY = -((avgY + 5000) * this.canvasZoom) + rect.height / 2;
    }

    console.log('🎯 Vue centrée sur les nœuds:', {
      avgPosition: { x: avgX, y: avgY },
      canvasOffset: { x: this.canvasOffsetX, y: this.canvasOffsetY },
    });
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.cleanupEventListeners();
  }

  // ================================
  // INITIALISATION
  // ================================

  private initializeComponent(): void {
    console.log('🚀 Initialisation du composant Flow Builder');
  }

  private setupEventListeners(): void {
    document.addEventListener('mousemove', this.onMouseMove.bind(this));
    document.addEventListener('mouseup', this.onMouseUp.bind(this));
  }

  private cleanupEventListeners(): void {
    document.removeEventListener('mousemove', this.onMouseMove.bind(this));
    document.removeEventListener('mouseup', this.onMouseUp.bind(this));
  }

  // ================================
  // CHARGEMENT ET SAUVEGARDE
  // ================================
  // Dans flow-builder-mvp.component.ts - Ces méthodes semblent déjà présentes, vérifiez

  zoomIn(): void {
    this.canvasZoom = Math.min(this.canvasZoom * 1.2, this.canvasMaxZoom);
  }
  /**
   * ================================
   * MÉTHODES À AJOUTER DANS VOTRE COMPOSANT ANGULAR
   * ================================
   */

  getResponseTypeTitle(type: string): string {
    const titles: { [key: string]: string } = {
      text: 'Texte libre',
      number: 'Nombre uniquement',
      email: 'Adresse email',
      phone: 'Numéro de téléphone',
      url: 'URL/Lien web',
      date: 'Date',
      time: 'Heure',
      file: 'Fichier (tout type)',
      image: 'Image uniquement',
      document: 'Document',
      audio: 'Fichier audio',
      video: 'Fichier vidéo',
      location: 'Localisation GPS',
      contact: 'Contact',
    };
    return titles[type] || 'Type de réponse';
  }

  getResponseTypeDescription(type: string): string {
    const descriptions: { [key: string]: string } = {
      text: "L'utilisateur peut saisir n'importe quel texte",
      number: "L'utilisateur doit saisir uniquement des chiffres",
      email: "L'utilisateur doit saisir une adresse email valide (ex: nom@domaine.com)",
      phone: "L'utilisateur doit saisir un numéro de téléphone valide",
      url: "L'utilisateur doit saisir une URL valide commençant par http:// ou https://",
      date: "L'utilisateur doit saisir une date au format JJ/MM/AAAA",
      time: "L'utilisateur doit saisir une heure au format HH:MM",
      file: "L'utilisateur doit envoyer un fichier (tout type accepté)",
      image: "L'utilisateur doit envoyer uniquement une image (JPG, PNG, GIF, etc.)",
      document: "L'utilisateur doit envoyer un document (PDF, Word, Excel, etc.)",
      audio: "L'utilisateur doit envoyer un fichier audio ou un message vocal",
      video: "L'utilisateur doit envoyer une vidéo",
      location: "L'utilisateur doit partager sa localisation GPS",
      contact: "L'utilisateur doit partager un contact de son répertoire",
    };
    return descriptions[type] || '';
  }

  getResponseTypeExample(type: string): string {
    const examples: { [key: string]: string } = {
      text: 'Jean Dupont',
      number: '25',
      email: 'jean.dupont@email.com',
      phone: '+33123456789',
      url: 'https://www.example.com',
      date: '15/03/2024',
      time: '14:30',
      file: 'document.pdf',
      image: 'photo.jpg',
      document: 'rapport.pdf',
      audio: 'enregistrement.mp3',
      video: 'video.mp4',
      location: 'Coordonnées GPS',
      contact: 'Contact partagé',
    };
    return examples[type] || '';
  }

  getDefaultValidationMessage(type: string | undefined): string {
    const messages: { [key: string]: string } = {
      text: 'Veuillez saisir du texte',
      number: 'Veuillez saisir un nombre valide',
      email: 'Veuillez saisir une adresse email valide',
      phone: 'Veuillez saisir un numéro de téléphone valide',
      url: 'Veuillez saisir une URL valide',
      date: 'Veuillez saisir une date valide (JJ/MM/AAAA)',
      time: 'Veuillez saisir une heure valide (HH:MM)',
      file: 'Veuillez envoyer un fichier',
      image: 'Veuillez envoyer une image',
      document: 'Veuillez envoyer un document',
      audio: 'Veuillez envoyer un fichier audio',
      video: 'Veuillez envoyer une vidéo',
      location: 'Veuillez partager votre localisation',
      contact: 'Veuillez partager un contact',
    };
    return type ? messages[type] || 'Format de réponse incorrect' : 'Format de réponse incorrect';
  }

  needsAdvancedConfig(type: string | undefined): boolean {
    return !!type && ['text', 'number', 'file', 'image', 'document', 'audio', 'video'].includes(type);
  }

  isFileType(type: string | undefined): boolean {
    return !!type && ['file', 'image', 'document', 'audio', 'video'].includes(type);
  }

  onResponseTypeChange(): void {
    const node = this.getSelectedNode();
    if (!node) return;

    if (!node.data.validationMessage) {
      node.data.validationMessage = this.getDefaultValidationMessage(node.data.responseType);
    }

    if (node.data.responseType !== 'number') {
      node.data.minValue = undefined;
      node.data.maxValue = undefined;
    }

    if (node.data.responseType !== 'text') {
      node.data.minLength = undefined;
      node.data.maxLength = undefined;
    }

    if (!this.isFileType(node.data.responseType)) {
      node.data.maxFileSize = undefined;
      node.data.allowedExtensions = undefined;
    }

    this.onNodeChange();
  }

  zoomOut(): void {
    this.canvasZoom = Math.max(this.canvasZoom / 1.2, this.canvasMinZoom);
  }

  resetZoom(): void {
    this.canvasZoom = 1;
    this.canvasOffsetX = 0;
    this.canvasOffsetY = 0;
  }
  /**
   * Charger le flow actuel de l'utilisateur depuis la base de données
   */
  // MODIFIER VOTRE MÉTHODE EXISTANTE
  private loadCurrentUserFlow(): Promise<void> {
    return new Promise(resolve => {
      this.isLoading = true;
      console.log('📥 Chargement du flow utilisateur...');

      this.chatbotService
        .loadCurrentFlow()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: flowConfig => {
            this.isLoading = false;

            if (flowConfig) {
              this.flowConfig = flowConfig;
              this.nodes = flowConfig.nodes || [];

              console.log('✅ Flow chargé depuis la base:', {
                name: flowConfig.name,
                nodes: this.nodes.length,
                variables: flowConfig.variables?.length || 0,
                flowId: flowConfig.flowId,
              });
            } else {
              this.createDefaultFlow();
              console.log('🆕 Nouveau flow créé par défaut');
            }
            resolve();
          },
          error: error => {
            this.isLoading = false;
            console.error('❌ Erreur lors du chargement:', error);
            this.createDefaultFlow();
            this.showToast('error', 'Erreur lors du chargement du flow. Un nouveau flow a été créé.');
            resolve();
          },
        });
    });
  }

  /**
   * Créer un flow par défaut
   */
  private createDefaultFlow(): void {
    const partnerId = this.chatbotService.getCurrentPartnerId();
    this.flowConfig = this.chatbotService.createNewFlow(partnerId, 'Mon Premier Flow');
    this.nodes = this.flowConfig.nodes || [];

    console.log('🆕 Flow par défaut créé:', {
      name: this.flowConfig.name,
      nodes: this.nodes.length,
    });
  }

  /**
   * Sauvegarder le flow dans la base de données
   */
  saveFlow(): void {
    // Validation simple
    if (!this.flowConfig.name?.trim()) {
      this.showToast('error', 'Veuillez donner un nom à votre flow');
      return;
    }

    if (this.nodes.length === 0) {
      this.showToast('warning', 'Votre flow doit contenir au moins un nœud');
      return;
    }

    this.isSaving = true;
    this.showToast('info', 'Sauvegarde en cours...', 2000);

    // Mettre à jour la timestamp
    this.flowConfig.updatedAt = new Date().toISOString();

    this.chatbotService
      .saveFlow(this.flowConfig)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: response => {
          this.isSaving = false;
          this.flowConfig.flowId = response.id.toString();

          this.showToast('success', '✅ Flow sauvegardé');
          console.log('✅ Flow sauvegardé:', response.id);
        },
        error: error => {
          this.isSaving = false;
          this.showToast('error', 'Erreur lors de la sauvegarde. Réessayez.');
          console.error('❌ Erreur sauvegarde:', error);
        },
      });
  }
  formatDateTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString(); // ou format personnalisé
  }

  // ================================
  // GESTION NŒUDS - BASE
  // ================================

  getNodeById(nodeId: string | undefined | null): FlowNode | null {
    if (!nodeId) return null;
    return this.nodes.find(n => n.id === nodeId) ?? null;
  }

  // Dans flow-builder-mvp.component.ts - AJOUTER cette propriété
  showLeftSidebar = true;
  // Dans flow-builder-mvp.component.ts - AJOUTER ces méthodes

  // Méthodes pour contrôler la sidebar
  toggleLeftSidebar(): void {
    this.showLeftSidebar = !this.showLeftSidebar;
  }
  // Version avec scroll plus fluide et configurable

  onCanvasWheel(event: WheelEvent): void {
    event.preventDefault();

    const scrollSpeed = 2; // Ajustez cette valeur pour la vitesse

    if (event.ctrlKey) {
      // ZOOM avec Ctrl + molette
      const delta = event.deltaY > 0 ? 0.9 : 1.1;
      this.canvasZoom = Math.min(Math.max(this.canvasZoom * delta, this.canvasMinZoom), this.canvasMaxZoom);
    } else {
      // SCROLL avec différentes combinaisons
      const container = event.target as HTMLElement;
      const canvasContainer = container.closest('.overflow-auto') as HTMLElement;

      if (canvasContainer) {
        if (event.shiftKey) {
          // Shift + molette = scroll horizontal uniquement
          canvasContainer.scrollLeft += event.deltaY * scrollSpeed;
        } else if (event.altKey) {
          // Alt + molette = scroll horizontal uniquement (alternative)
          canvasContainer.scrollLeft += event.deltaY * scrollSpeed;
        } else {
          // Molette normale = scroll vertical + horizontal si deltaX existe
          if (Math.abs(event.deltaX) > 0) {
            canvasContainer.scrollLeft += event.deltaX * scrollSpeed;
          }
          canvasContainer.scrollTop += event.deltaY * scrollSpeed;
        }
      }
    }
  }

  private generateNodeId(): string {
    return `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private getNextNodeOrder(): number {
    const maxOrder = Math.max(...this.nodes.map(n => n.order), 0);
    return maxOrder + 1;
  }
  addNode(type: NodeType, x?: number, y?: number): void {
    let nodeX = x || 300;
    let nodeY = y || 200;

    // Si pas de position, placer au centre de la vue
    if (!x && !y) {
      const canvasRect = document.querySelector('.flex-1.relative.overflow-hidden');
      if (canvasRect) {
        const rect = canvasRect.getBoundingClientRect();
        // Position relative au centre de la vue SANS l'offset de 5000
        nodeX = (-this.canvasOffsetX + rect.width / 2) / this.canvasZoom;
        nodeY = (-this.canvasOffsetY + rect.height / 2) / this.canvasZoom;
      }
    }

    const newNode: FlowNode = {
      id: this.generateNodeId(),
      type,
      x: nodeX,
      y: nodeY,
      data: ImprovedFlowUtils.createDefaultNodeData(type),
      order: this.getNextNodeOrder(),
    };

    this.nodes.push(newNode);
    this.selectedNodeId = newNode.id;
    this.updateFlowConfig();

    console.log('➕ Nœud ajouté:', {
      type: type,
      id: newNode.id,
      position: { x: nodeX, y: nodeY },
    });
  }
  selectNode(nodeId: string, event: Event): void {
    event.stopPropagation();
    this.selectedNodeId = nodeId;

    console.log('🎯 Nœud sélectionné:', nodeId);

    // NOUVEAU : Initialiser waitForUserResponse à true si pas défini
    const selectedNode = this.getNodeById(nodeId);
    if (selectedNode && selectedNode.data.waitForUserResponse === undefined) {
      selectedNode.data.waitForUserResponse = true;
      this.updateFlowConfig();
      console.log('✅ waitForUserResponse initialisé à true pour le nœud:', nodeId);
    }

    // Vérifier que la sidebar va s'afficher
    setTimeout(() => {
      const sidebar = document.querySelector('.w-96');
      const isVisible = sidebar && !sidebar.classList.contains('hidden');
      console.log('📋 Sidebar visible:', isVisible);

      if (!isVisible) {
        console.error("❌ Sidebar ne s'affiche pas !");
        // Forcer l'affichage
        if (sidebar) {
          sidebar.classList.remove('hidden');
        }
      }
    }, 100);
  }

  getSelectedNode(): FlowNode | null {
    return this.nodes.find(n => n.id === this.selectedNodeId) || null;
  }

  deleteNode(nodeId: string, event?: Event): void {
    if (event) event.stopPropagation();

    const node = this.nodes.find(n => n.id === nodeId);
    if (node?.type === 'start') {
      this.showToast('warning', 'Impossible de supprimer le nœud de démarrage');
      return;
    }

    if (confirm('Supprimer ce nœud ?')) {
      this.nodes = this.nodes.filter(n => n.id !== nodeId);
      this.cleanupConnectionsToNode(nodeId);

      if (this.selectedNodeId === nodeId) {
        this.selectedNodeId = null;
      }

      this.updateFlowConfig();
      console.log('🗑️ Nœud supprimé:', nodeId);
    }
  }

  private cleanupConnectionsToNode(nodeId: string): void {
    this.nodes.forEach(node => {
      // Connexions simples
      if (node.nextNodeId === nodeId) {
        node.nextNodeId = undefined;
      }

      // Connexions conditionnelles (compatibilité ancienne)
      if (node.data.trueNextNodeId === nodeId) {
        node.data.trueNextNodeId = undefined;
      }
      if (node.data.falseNextNodeId === nodeId) {
        node.data.falseNextNodeId = undefined;
      }

      // Nouvelles connexions conditionnelles
      if (node.data.conditionalConnections) {
        node.data.conditionalConnections = node.data.conditionalConnections.filter(conn => conn.nextNodeId !== nodeId);
      }
      if (node.data.defaultNextNodeId === nodeId) {
        node.data.defaultNextNodeId = undefined;
      }

      // Boutons
      if (node.data.buttons) {
        node.data.buttons.forEach(button => {
          if (button.nextNodeId === nodeId) {
            button.nextNodeId = undefined;
          }
        });
      }

      // Items de liste
      if (node.data.items) {
        node.data.items.forEach(item => {
          if (item.nextNodeId === nodeId) {
            item.nextNodeId = undefined;
          }
        });
      }
    });
  }

  duplicateNode(nodeId: string): void {
    const node = this.nodes.find(n => n.id === nodeId);
    if (!node) return;

    const duplicatedNode: FlowNode = {
      ...node,
      id: this.generateNodeId(),
      x: node.x + 50,
      y: node.y + 50,
      order: this.getNextNodeOrder(),
      nextNodeId: undefined,
      data: { ...node.data }, // Clone des données
    };

    this.nodes.push(duplicatedNode);
    this.selectedNodeId = duplicatedNode.id;
    this.updateFlowConfig();

    console.log('📄 Nœud dupliqué:', {
      original: nodeId,
      duplicate: duplicatedNode.id,
    });
  }

  onNodeChange(): void {
    this.updateFlowConfig();
  }

  // ================================
  // GESTION CONNEXIONS - BASE
  // ================================

  startConnection(nodeId: string, event: Event): void {
    event.stopPropagation();
    this.isConnecting = true;
    this.connectionStartNodeId = nodeId;
    console.log('🔗 Début connexion depuis:', nodeId);
  }

  finishConnection(nodeId: string, event: Event): void {
    event.stopPropagation();

    if (this.isConnecting && this.connectionStartNodeId && this.connectionStartNodeId !== nodeId) {
      const fromNode = this.nodes.find(n => n.id === this.connectionStartNodeId);
      if (fromNode && fromNode.type !== 'condition') {
        fromNode.nextNodeId = nodeId;
        this.updateFlowConfig();

        console.log('✅ Connexion créée:', {
          from: this.connectionStartNodeId,
          to: nodeId,
        });
      }
    }

    this.isConnecting = false;
    this.connectionStartNodeId = null;
  }

  getTargetNode(node: FlowNode): FlowNode | null {
    if (!node.nextNodeId) return null;
    return this.nodes.find(n => n.id === node.nextNodeId) || null;
  }

  getAvailableNodes(excludeNodeId: string): FlowNode[] {
    return this.nodes.filter(n => n.id !== excludeNodeId);
  }

  // ================================
  // GESTION CONNEXIONS CONDITIONNELLES
  // ================================

  addConditionalConnection(node: FlowNode): void {
    if (!node.data.conditionalConnections) {
      node.data.conditionalConnections = [];
    }

    const newConnection: ConditionalConnection = {
      id: `conn_${Date.now()}`,
      condition: '',
      nextNodeId: '',
      operator: 'equals',
    };

    node.data.conditionalConnections.push(newConnection);
    this.updateFlowConfig();

    console.log('🔀 Connexion conditionnelle ajoutée au nœud:', node.id);
  }

  removeConditionalConnection(node: FlowNode, index: number): void {
    if (node.data.conditionalConnections) {
      node.data.conditionalConnections.splice(index, 1);
      this.updateFlowConfig();

      console.log('🗑️ Connexion conditionnelle supprimée du nœud:', node.id);
    }
  }

  onConditionTypeChange(node: FlowNode): void {
    node.data.conditionalConnections = [];
    node.data.variable = '';
    node.data.defaultNextNodeId = '';
    this.updateFlowConfig();

    console.log('🔄 Type de condition changé pour le nœud:', node.id);
  }

  // ================================
  // GESTION BOUTONS/LISTES
  // ================================

  addButton(node: FlowNode): void {
    if (!node.data.buttons) {
      node.data.buttons = [];
    }

    const buttonIndex = node.data.buttons.length + 1;
    const newButton: NodeButton = {
      id: `btn_${Date.now()}`,
      text: `Bouton ${buttonIndex}`,
      value: `option${buttonIndex}`,
      storeInVariable: node.data.storeInVariable || 'user_choice',
    };

    node.data.buttons.push(newButton);
    this.ensureVariableExists(newButton.storeInVariable || 'user_choice', 'string');
    this.updateFlowConfig();

    console.log('🔘 Bouton ajouté au nœud:', node.id);
  }

  removeButton(node: FlowNode, index: number): void {
    if (node.data.buttons) {
      node.data.buttons.splice(index, 1);
      this.updateFlowConfig();

      console.log('🗑️ Bouton supprimé du nœud:', node.id);
    }
  }

  addListItem(node: FlowNode): void {
    if (!node.data.items) {
      node.data.items = [];
    }

    const itemIndex = node.data.items.length + 1;
    const newItem: NodeListItem = {
      id: `item_${Date.now()}`,
      title: `Option ${itemIndex}`,
      value: `option${itemIndex}`,
      storeInVariable: node.data.storeInVariable || 'user_selection',
    };

    node.data.items.push(newItem);
    this.ensureVariableExists(newItem.storeInVariable || 'user_selection', 'string');
    this.updateFlowConfig();

    console.log('📝 Item de liste ajouté au nœud:', node.id);
  }

  removeListItem(node: FlowNode, index: number): void {
    if (node.data.items) {
      node.data.items.splice(index, 1);
      this.updateFlowConfig();

      console.log('🗑️ Item de liste supprimé du nœud:', node.id);
    }
  }

  // ================================
  // GESTION VARIABLES
  // ================================

  ensureVariableExists(variableName: string, type: 'string' | 'number' | 'boolean' = 'string'): void {
    if (!variableName || variableName.trim() === '') return;

    const exists = this.flowConfig.variables.find(v => v.name === variableName);
    if (!exists) {
      const newVariable: FlowVariable = {
        name: variableName,
        value: type === 'string' ? '' : type === 'number' ? 0 : false,
        type,
        description: `Variable créée automatiquement`,
        isSystem: false,
      };
      this.flowConfig.variables.push(newVariable);

      console.log('📊 Variable créée automatiquement:', variableName);
    }
  }

  addVariable(): void {
    const variableName = prompt('Nom de la variable:');
    if (variableName && variableName.trim()) {
      const newVariable: FlowVariable = {
        name: variableName.trim(),
        value: '',
        type: 'string',
      };
      this.flowConfig.variables.push(newVariable);
      this.updateFlowConfig();

      console.log('📊 Variable ajoutée manuellement:', variableName);
    }
  }

  removeVariable(index: number): void {
    if (confirm('Supprimer cette variable ?')) {
      const variableName = this.flowConfig.variables[index].name;
      this.flowConfig.variables.splice(index, 1);
      this.updateFlowConfig();

      console.log('🗑️ Variable supprimée:', variableName);
    }
  }

  // ================================
  // CONNEXIONS VISUELLES
  // ================================

  getConnectionPath(fromNode: FlowNode, toNode: FlowNode, offset: number = 0): string {
    const startX = fromNode.x + 192;
    const startY = fromNode.y + 60 + offset;
    const endX = toNode.x;
    const endY = toNode.y + 60;

    const midX = (startX + endX) / 2;
    return `M ${startX} ${startY} C ${midX} ${startY}, ${midX} ${endY}, ${endX} ${endY}`;
  }

  getConnectionLabelX(fromNode: FlowNode, toNode: FlowNode, offset: number = 0): number {
    const startX = fromNode.x + 192;
    const endX = toNode.x;
    return (startX + endX) / 2;
  }

  getConnectionLabelY(fromNode: FlowNode, toNode: FlowNode, offset: number = 0): number {
    const startY = fromNode.y + 60 + offset;
    const endY = toNode.y + 60;
    return (startY + endY) / 2;
  }

  getMarkerEnd(color: string): string {
    switch (color) {
      case '#10B981':
        return 'url(#arrowhead-green)';
      case '#8B5CF6':
        return 'url(#arrowhead-purple)';
      case '#F59E0B':
        return 'url(#arrowhead-orange)';
      case '#EF4444':
        return 'url(#arrowhead-red)';
      default:
        return 'url(#arrowhead)';
    }
  }

  // Dans flow-builder-mvp.component.ts - REMPLACER la méthode getNodeConnections

  getNodeConnections(node: FlowNode): Array<{ targetId: string; label: string; color: string }> {
    const connections: Array<{ targetId: string; label: string; color: string }> = [];

    // Connexion simple (principale)
    if (node.nextNodeId) {
      connections.push({
        targetId: node.nextNodeId,
        label: 'Suivant',
        color: '#6B7280',
      });
    }

    // Connexions de boutons
    if (node.data.buttons) {
      node.data.buttons.forEach(button => {
        if (button.nextNodeId) {
          connections.push({
            targetId: button.nextNodeId,
            label: button.text,
            color: '#10B981',
          });
        }
      });
    }

    // Connexions de liste
    if (node.data.items) {
      node.data.items.forEach(item => {
        if (item.nextNodeId) {
          connections.push({
            targetId: item.nextNodeId,
            label: item.title,
            color: '#8B5CF6',
          });
        }
      });
    }

    // Connexions conditionnelles
    if (node.data.conditionalConnections) {
      node.data.conditionalConnections.forEach(conn => {
        if (conn.nextNodeId) {
          connections.push({
            targetId: conn.nextNodeId,
            label: `Si ${conn.condition}`,
            color: '#F59E0B',
          });
        }
      });
    }

    // Connexion par défaut des conditions
    if (node.data.defaultNextNodeId) {
      connections.push({
        targetId: node.data.defaultNextNodeId,
        label: 'Sinon',
        color: '#EF4444',
      });
    }

    return connections;
  }

  // ================================
  // GESTION DRAG & DROP
  // ================================

  startDragging(nodeId: string, event: MouseEvent): void {
    event.stopPropagation();
    this.isDragging = true;
    this.dragNodeId = nodeId;
    this.dragStartX = event.clientX;
    this.dragStartY = event.clientY;
  }

  // Modifier votre méthode onMouseMove existante
  onMouseMove(event: MouseEvent): void {
    if (this.isDragging && this.dragNodeId) {
      const deltaX = event.clientX - this.dragStartX;
      const deltaY = event.clientY - this.dragStartY;

      const node = this.nodes.find(n => n.id === this.dragNodeId);
      if (node) {
        // SUPPRESSION DES LIMITES - permet de déplacer partout
        node.x = node.x + deltaX / this.canvasZoom;
        node.y = node.y + deltaY / this.canvasZoom;
        // Plus de Math.max(0, ...) pour permettre coordonnées négatives
      }

      this.dragStartX = event.clientX;
      this.dragStartY = event.clientY;
    }

    // Pan du canvas
    if (this.isPanning) {
      this.canvasOffsetX = event.clientX - this.panStartX;
      this.canvasOffsetY = event.clientY - this.panStartY;
    }
  }
  onMouseUp(): void {
    if (this.isDragging) {
      this.updateFlowConfig();
    }
    this.isDragging = false;
    this.dragNodeId = null;
  }

  deselectNode(): void {
    this.selectedNodeId = null;
    this.isConnecting = false;
    this.connectionStartNodeId = null;
  }

  // ================================
  // UTILITAIRES NŒUDS
  // ================================

  getNodeLabel(type: NodeType): string {
    const nodeType = this.nodeTypes.find(nt => nt.type === type);
    return nodeType?.label || type;
  }

  getNodeIcon(type: NodeType): string {
    const nodeType = this.nodeTypes.find(nt => nt.type === type);
    return nodeType?.icon || 'fas fa-question-circle';
  }

  getNodeColor(type: NodeType): string {
    const nodeType = this.nodeTypes.find(nt => nt.type === type);
    return nodeType?.color || 'bg-gray-500';
  }

  getNodesByCategory(category: 'message' | 'interaction' | 'logic') {
    return this.nodeTypes.filter(nt => nt.category === category);
  }

  getNodeDisplayLabel(node: FlowNode): string {
    if (node.label && node.label.trim()) {
      return node.label;
    }
    return this.getNodeLabel(node.type);
  }

  editNodeLabel(node: FlowNode, event: Event): void {
    event.stopPropagation();
    const currentLabel = node.label || this.getNodeLabel(node.type);
    const newLabel = prompt('Titre personnalisé du nœud:', currentLabel);
    if (newLabel !== null) {
      node.label = newLabel.trim() || undefined;
      this.updateFlowConfig();

      console.log('✏️ Label du nœud modifié:', node.id);
    }
  }

  // ================================
  // VALIDATION ET TEST (CÔTÉ FRONTEND SEULEMENT)
  // ================================

  validateFlowAdvanced(): void {
    console.log('🔍 Validation avancée du flow...');

    const result = ImprovedFlowUtils.validateImprovedFlow(this.flowConfig);
    let message = result.isValid ? '✅ Flow valide ! Aucune erreur détectée.' : '❌ Erreurs détectées :\n' + result.errors.join('\n');

    if (result.warnings.length > 0) {
      message += '\n\n⚠️ Avertissements :\n' + result.warnings.join('\n');
    }

    this.showToast('info', message, 2000);

    console.log('🔍 Résultat validation avancée:', result);
  }

  // ================================
  // UPLOAD FICHIERS (optionnel)
  // ================================

  onImageUpload(event: Event, node: FlowNode): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (file) {
      console.log('📷 Upload image pour nœud:', node.id);

      this.chatbotService
        .uploadImage(file)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: result => {
            node.data.imageUrl = result.url;
            this.updateFlowConfig();
            this.showToast('success', ' Image uploadée avec succès !');
            console.log('✅ Image uploadée:', result.url);
          },
          error: error => {
            this.showToast('error', `Erreur upload image: ${error.message}`);
            console.error('❌ Erreur upload image:', error);
          },
        });
    }
    input.value = '';
  }

  onFileUpload(event: Event, node: FlowNode): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (file) {
      this.chatbotService
        .uploadFile(file)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: result => {
            node.data.fileUrl = result.url;
            node.data.fileName = result.filename;
            this.updateFlowConfig();
            this.showToast('success', 'Fichier uploadé avec succès !');
          },
          error: error => {
            this.showToast('error', `Erreur upload fichier: ${error.message}`);
          },
        });
    }
    input.value = '';
  }

  // ================================
  // WEBHOOK 3CX (TEST FRONTEND SEULEMENT)
  // ================================

  testWebhook(node: FlowNode): void {
    if (!node.data.webhookUrl) {
      this.showToast('error', 'URL du webhook manquante');
      return;
    }

    // Test côté frontend - simulation
    const testData = {
      partnerId: this.flowConfig.partnerId,
      userPhone: '+1234567890',
      variables: this.flowConfig.variables.reduce((acc, v) => {
        acc[v.name] = v.value;
        return acc;
      }, {} as any),
      message: 'Test depuis le flow builder',
    };

    console.log('🔗 Test webhook (simulation):', {
      url: node.data.webhookUrl,
      method: node.data.method || 'POST',
      data: testData,
    });
    this.showToast('success', '🔗 Webhook testé (simulation) .');
  }

  // ================================
  // MÉTHODES UTILITAIRES
  // ================================

  /**
   * Afficher l'aide pour un type de nœud
   */
  showNodeHelp(nodeType: NodeType): void {
    const helpTexts = {
      start: 'Point de départ du flow. Chaque flow doit avoir un nœud de démarrage.',
      message: 'Envoie un message texte simple. Peut utiliser des variables avec {nom_variable}.',
      buttons: 'Affiche des boutons cliquables. Chaque bouton peut stocker une valeur et rediriger vers un nœud différent.',
      list: "Affiche une liste déroulante d'options. Pratique pour les menus longs.",
      input: "Demande une saisie à l'utilisateur. Peut valider le format (email, téléphone, etc.).",
      wait_response: "Attend une réponse libre de l'utilisateur sans validation spécifique.",
      condition: 'Créé des branchements selon des conditions. Peut tester des variables ou des réponses.',
      variable_set: "Modifie la valeur d'une variable. Utile pour le scoring ou le suivi d'état.",
      image: 'Envoie une image avec légende optionnelle.',
      file: "Envoie un fichier (PDF, document, etc.) avec message d'accompagnement.",
      webhook: 'Envoie les données vers un système externe (CRM, 3CX, etc.).',
      api_connector:
        "Connecte votre flow à une API externe. Permet d'envoyer des données collectées (ex: variables, réponses utilisateur) et de traiter la réponse pour continuer le scénario dynamiquement.",
      whatsapp_form:
        "Créé un formulaire interactif natif dans WhatsApp. Les utilisateurs remplissent le formulaire directement dans l'app WhatsApp sans quitter la conversation. Support de 10 types de champs avec validation et mapping automatique vers les variables du flow.",
      end: 'Termine le flow. Peut envoyer un message de fin.',
    };

    const help = helpTexts[nodeType] || 'Aucune aide disponible pour ce type de nœud.';
    this.showToast('info', `Aide - ${this.getNodeLabel(nodeType)}:\n\n${help}`);
  }

  /**
   * Calculer le nombre total de connexions
   */
  getTotalConnectionCount(): number {
    let count = 0;
    this.nodes.forEach(node => {
      count += this.getNodeConnections(node).length;
    });
    return count;
  }

  // ================================
  // TEMPLATES RAPIDES
  // ================================

  createQuickTemplate(templateType: string): void {
    switch (templateType) {
      case 'welcome':
        this.createWelcomeTemplate();
        break;
      case 'support':
        this.createSupportTemplate();
        break;
      case 'survey':
        this.createSurveyTemplate();
        break;
      default:
        console.warn('Template non reconnu:', templateType);
    }
  }

  private createWelcomeTemplate(): void {
    const startNode = this.nodes.find(n => n.type === 'start');
    this.nodes = startNode ? [startNode] : [];

    this.addNode('message', 300, 100);
    const welcomeNode = this.nodes[this.nodes.length - 1];
    welcomeNode.data.text = 'Bonjour ! Bienvenue sur notre service. Comment puis-je vous aider ?';

    this.addNode('buttons', 300, 250);
    const buttonsNode = this.nodes[this.nodes.length - 1];
    buttonsNode.data.text = 'Choisissez une option :';
    buttonsNode.data.buttons = [
      { id: '1', text: 'Informations produits' },
      { id: '2', text: 'Support technique' },
      { id: '3', text: 'Parler à un agent' },
    ];

    if (startNode) {
      startNode.nextNodeId = welcomeNode.id;
    }
    welcomeNode.nextNodeId = buttonsNode.id;
    this.updateFlowConfig();
  }

  private createSupportTemplate(): void {
    const startNode = this.nodes.find(n => n.type === 'start');
    this.nodes = startNode ? [startNode] : [];

    this.addNode('message', 300, 100);
    const messageNode = this.nodes[this.nodes.length - 1];
    messageNode.data.text = 'Je vais vous mettre en relation avec notre équipe support.';

    this.addNode('variable_set', 300, 250);
    const varNode = this.nodes[this.nodes.length - 1];
    varNode.data.variableName = 'support_request';
    varNode.data.variableOperation = 'set';
    varNode.data.variableValue = 'true';

    this.addNode('webhook', 300, 400);
    const webhookNode = this.nodes[this.nodes.length - 1];
    webhookNode.data.webhookUrl = 'https://your-3cx-server.com/webhook/transfer';
    webhookNode.data.method = 'POST';

    if (startNode) {
      startNode.nextNodeId = messageNode.id;
    }
    messageNode.nextNodeId = varNode.id;
    varNode.nextNodeId = webhookNode.id;

    if (!this.flowConfig.variables.find(v => v.name === 'support_request')) {
      this.flowConfig.variables.push({
        name: 'support_request',
        value: 'false',
        type: 'boolean',
      });
    }
    this.updateFlowConfig();
  }

  private createSurveyTemplate(): void {
    const startNode = this.nodes.find(n => n.type === 'start');
    this.nodes = startNode ? [startNode] : [];

    this.addNode('list', 300, 100);
    const questionNode = this.nodes[this.nodes.length - 1];
    questionNode.data.text = 'Comment évaluez-vous notre service ?';
    questionNode.data.items = [
      { id: '1', title: 'Très satisfait' },
      { id: '2', title: 'Satisfait' },
      { id: '3', title: 'Peu satisfait' },
      { id: '4', title: 'Pas satisfait' },
    ];

    this.addNode('variable_set', 300, 250);
    const varNode = this.nodes[this.nodes.length - 1];
    varNode.data.variableName = 'satisfaction';
    varNode.data.variableOperation = 'set';

    this.addNode('condition', 300, 400);
    const conditionNode = this.nodes[this.nodes.length - 1];
    conditionNode.data.variable = 'satisfaction';
    conditionNode.data.operator = 'contains';
    conditionNode.data.value = 'satisfait';

    this.addNode('message', 150, 550);
    const positiveNode = this.nodes[this.nodes.length - 1];
    positiveNode.data.text = 'Merci pour votre retour positif !';

    this.addNode('message', 450, 550);
    const negativeNode = this.nodes[this.nodes.length - 1];
    negativeNode.data.text = 'Nous sommes désolés. Un agent va vous contacter.';

    if (startNode) {
      startNode.nextNodeId = questionNode.id;
    }
    questionNode.nextNodeId = varNode.id;
    varNode.nextNodeId = conditionNode.id;
    conditionNode.data.trueNextNodeId = positiveNode.id;
    conditionNode.data.falseNextNodeId = negativeNode.id;

    if (!this.flowConfig.variables.find(v => v.name === 'satisfaction')) {
      this.flowConfig.variables.push({
        name: 'satisfaction',
        value: '',
        type: 'string',
      });
    }
    this.updateFlowConfig();
  }

  // ================================
  // MISE À JOUR FLOW - ESSENTIEL
  // ================================

  private updateFlowConfig(): void {
    this.flowConfig.nodes = [...this.nodes];
    this.flowConfig.updatedAt = new Date().toISOString();
    this.chatbotService.setCurrentFlow(this.flowConfig);
  }

  isTestModalOpen = false;
  isTestStarted = false;
  isTestCompleted = false;
  isTyping = false;
  isWaitingForButton = false;
  showDebugInfo = false;

  testInputValue = '';
  currentTestStep = 0;
  currentTestNode: FlowNode | null = null;

  testSession: {
    id: string;
    startTime: Date;
    variables: { [key: string]: any };
    currentNodeId?: string;
  } | null = null;

  testMessages: Array<{
    id: string;
    type: 'bot' | 'user';
    content: string;
    buttons?: Array<{ text: string; value: string; nodeId?: string }>;
    timestamp: Date;
    isButton?: boolean;
    nodeInfo?: string;
    buttonsDisabled?: boolean;
  }> = [];

  // ================================
  // MÉTHODES DE TEST PRINCIPALES
  // ================================

  /**
   * Remplacez la méthode testFlow() existante par celle-ci
   */
  testFlow(): void {
    console.log('🧪 Ouverture du modal de test...');

    // Validation rapide
    if (this.nodes.length === 0) {
      this.showToast('warning', 'Aucun nœud à tester. Ajoutez des nœuds à votre flow.');
      return;
    }

    const startNode = this.nodes.find(n => n.type === 'start');
    if (!startNode) {
      this.showToast('warning', 'Nœud de démarrage manquant. Impossible de tester le flow.');
      return;
    }

    // Ouvrir le modal
    this.isTestModalOpen = true;
    this.resetTestState();
  }

  /**
   * Fermer le modal de test
   */
  closeTestModal(): void {
    this.isTestModalOpen = false;
    this.resetTestState();
  }

  /**
   * Démarrer le test du flow
   */
  startFlowTest(): void {
    console.log('🚀 Démarrage du test du flow...');

    this.isTestStarted = true;
    this.isTestCompleted = false;
    this.initializeTestSession();

    // Démarrer l'exécution
    const startNode = this.nodes.find(n => n.type === 'start');
    if (startNode) {
      setTimeout(() => {
        this.executeTestNode(startNode);
      }, 800);
    }
  }

  /**
   * Redémarrer le test
   */
  restartTest(): void {
    this.resetTestState();
    setTimeout(() => {
      this.startFlowTest();
    }, 100);
  }

  /**
   * Réinitialiser l'état du test
   */
  private resetTestState(): void {
    this.testMessages = [];
    this.testInputValue = '';
    this.isTestStarted = false;
    this.isTestCompleted = false;
    this.isTyping = false;
    this.isWaitingForButton = false;
    this.currentTestStep = 0;
    this.currentTestNode = null;
    this.testSession = null;
  }

  /**
   * Initialiser la session de test
   */
  private initializeTestSession(): void {
    this.testSession = {
      id: `test_${Date.now()}`,
      startTime: new Date(),
      variables: {},
    };

    // Initialiser les variables du flow
    this.flowConfig.variables.forEach(variable => {
      this.testSession!.variables[variable.name] = variable.value;
    });

    console.log('✅ Session de test initialisée:', this.testSession.id);
  }

  // ================================
  // EXÉCUTION DES NŒUDS
  // ================================

  /**
   * Exécuter un nœud dans le contexte du test
   */
  private executeTestNode(node: FlowNode): void {
    console.log('🎯 Exécution du nœud:', node.type, `(${node.order})`);

    this.currentTestNode = node;
    this.testSession!.currentNodeId = node.id;

    switch (node.type) {
      case 'start':
        this.executeStartNode(node);
        break;
      case 'message':
        this.executeMessageNode(node);
        break;
      case 'buttons':
        this.executeButtonsNode(node);
        break;
      case 'list':
        this.executeListNode(node);
        break;
      case 'input':
        this.executeInputNode(node);
        break;
      case 'wait_response':
        this.executeWaitResponseNode(node);
        break;
      case 'condition':
        this.executeConditionNode(node);
        break;
      case 'variable_set':
        this.executeVariableSetNode(node);
        break;
      case 'image':
        this.executeImageNode(node);
        break;
      case 'file':
        this.executeFileNode(node);
        break;
      case 'webhook':
        this.executeWebhookNode(node);
        break;
      case 'end':
        this.executeEndNode(node);
        break;
      default:
        this.addTestMessage('bot', `⚠️ Type de nœud non supporté: ${node.type}`, undefined, `Nœud ${node.order}`);
    }
  }

  private executeStartNode(node: FlowNode): void {
    if (node.nextNodeId) {
      const nextNode = this.getNodeById(node.nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 500);
      }
    } else {
      this.addTestMessage('bot', '⚠️ Nœud de démarrage sans connexion', undefined, `Nœud ${node.order}`);
    }
  }

  private executeMessageNode(node: FlowNode): void {
    let message = node.data.text || 'Message vide';

    // Remplacer les variables
    if (node.data.useVariables && this.testSession) {
      message = this.replaceVariables(message, this.testSession.variables);
    }

    this.addTestMessage('bot', message, undefined, `Message • Nœud ${node.order}`);

    // Continuer vers le nœud suivant
    if (node.nextNodeId) {
      const nextNode = this.getNodeById(node.nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 1200);
      }
    } else {
      this.finishTest('Message sans suite');
    }
  }

  // Dans flow-builder-mvp.component.ts - MODIFIER ces méthodes

  private executeButtonsNode(node: FlowNode): void {
    const message = node.data.text || 'Choisissez une option:';
    const buttons = (node.data.buttons || []).map(btn => ({
      text: btn.text || 'Bouton',
      value: btn.value || btn.text || 'default',
      nodeId: btn.nextNodeId,
    }));

    if (buttons.length > 0) {
      this.isWaitingForButton = true; // ← Bloquer la saisie texte
      this.addTestMessage('bot', message, buttons, `Boutons • Nœud ${node.order}`);
    } else {
      this.addTestMessage('bot', message + '\n⚠️ Aucun bouton configuré', undefined, `Nœud ${node.order}`);
      this.finishTest('Boutons non configurés');
    }
  }

  private executeListNode(node: FlowNode): void {
    const message = node.data.text || 'Sélectionnez dans la liste:';
    const buttons = (node.data.items || []).map(item => ({
      text: item.title || 'Option',
      value: item.value || item.title || 'default',
      nodeId: item.nextNodeId,
    }));

    if (buttons.length > 0) {
      this.isWaitingForButton = true; // ← Bloquer la saisie texte
      this.addTestMessage('bot', message, buttons, `Liste • Nœud ${node.order}`);
    } else {
      this.addTestMessage('bot', message + '\n⚠️ Aucune option configurée', undefined, `Nœud ${node.order}`);
      this.finishTest('Liste non configurée');
    }
  }

  private executeInputNode(node: FlowNode): void {
    const message = node.data.text || 'Veuillez saisir votre réponse:';
    const responseType = node.data.responseType || 'text';

    this.addTestMessage('bot', message, undefined, `Input ${responseType} • Nœud ${node.order}`);

    // ⚠️ NE PAS continuer automatiquement - attendre la saisie utilisateur
    console.log('⏳ En attente de saisie utilisateur pour nœud input');
  }

  private executeWaitResponseNode(node: FlowNode): void {
    const message = node.data.text || "J'attends votre réponse...";
    this.addTestMessage('bot', message, undefined, `Attente • Nœud ${node.order}`);

    // ⚠️ NE PAS continuer automatiquement - attendre la réponse utilisateur
    console.log('⏳ En attente de réponse utilisateur pour nœud wait_response');
  }

  // Dans flow-builder-mvp.component.ts - MODIFIER la méthode existante
  private executeConditionNode(node: FlowNode): void {
    if (!this.testSession) return;

    let nextNodeId: string | undefined;
    let conditionMet = 'Aucune';

    // Récupérer la dernière réponse utilisateur
    const lastUserMessage = this.testMessages.filter(m => m.type === 'user').slice(-1)[0];
    const userInput = lastUserMessage ? lastUserMessage.content : '';

    console.log('🔀 Évaluation conditions avec input utilisateur:', userInput);

    // Évaluer les connexions conditionnelles
    if (node.data.conditionalConnections && node.data.conditionalConnections.length > 0) {
      for (const conn of node.data.conditionalConnections) {
        let conditionResult = false;

        // NOUVEAU : Gestion des expressions custom
        if (conn.operator === 'custom_expression') {
          conditionResult = this.evaluateCustomExpression(conn.condition, userInput, this.testSession.variables);
          conditionMet = `Expression: "${conn.condition}" → ${conditionResult}`;
        } else {
          // Logique existante pour les autres opérateurs
          conditionResult = this.evaluateTestCondition(conn, this.testSession.variables);
          conditionMet = `${conn.operator} "${conn.condition}" → ${conditionResult}`;
        }

        if (conditionResult) {
          nextNodeId = conn.nextNodeId;
          break;
        }
      }
    }

    // Si aucune condition n'est remplie, utiliser le nœud par défaut
    if (!nextNodeId && node.data.defaultNextNodeId) {
      nextNodeId = node.data.defaultNextNodeId;
      conditionMet = 'Défaut (aucune condition remplie)';
    }

    // Afficher le résultat dans le chat de test
    this.addTestMessage(
      'bot',
      `🔀 **Condition évaluée:**\n${conditionMet}\n➜ ${nextNodeId ? 'Condition remplie' : 'Aucune condition'}`,
      undefined,
      `Condition • Nœud ${node.order}`,
    );

    if (nextNodeId) {
      const nextNode = this.getNodeById(nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 1000);
      }
    } else {
      this.finishTest('Aucune condition remplie');
    }
  }

  private executeVariableSetNode(node: FlowNode): void {
    if (!this.testSession || !node.data.variableName) {
      this.addTestMessage('bot', '⚠️ Variable non configurée', undefined, `Variable • Nœud ${node.order}`);
      return;
    }

    const varName = node.data.variableName;
    const operation = node.data.variableOperation || 'set';
    const oldValue = this.testSession.variables[varName];

    switch (operation) {
      case 'set':
        this.testSession.variables[varName] = node.data.variableValue || '';
        break;
      case 'increment':
        this.testSession.variables[varName] = (this.testSession.variables[varName] || 0) + 1;
        break;
      case 'decrement':
        this.testSession.variables[varName] = (this.testSession.variables[varName] || 0) - 1;
        break;
    }

    const newValue = this.testSession.variables[varName];
    this.addTestMessage('bot', `📊 ${varName}: ${oldValue} → ${newValue}`, undefined, `Variable • Nœud ${node.order}`);

    // Continuer
    if (node.nextNodeId) {
      const nextNode = this.getNodeById(node.nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 600);
      }
    } else {
      this.finishTest('Variable sans suite');
    }
  }

  private executeImageNode(node: FlowNode): void {
    const hasImage = node.data.imageUrl && node.data.imageUrl.trim();
    const message = hasImage ? `📷 [Image: ${node.data.imageUrl}]` : '📷 [Image non configurée]';

    if (node.data.text && node.data.text.trim()) {
      this.addTestMessage('bot', node.data.text, undefined, `Image • Nœud ${node.order}`);
      setTimeout(() => {
        this.addTestMessage('bot', message, undefined, `Fichier • Nœud ${node.order}`);
        this.continueFromNode(node);
      }, 1000);
    } else {
      this.addTestMessage('bot', message, undefined, `Image • Nœud ${node.order}`);
      this.continueFromNode(node);
    }
  }

  private executeFileNode(node: FlowNode): void {
    const hasFile = node.data.fileUrl && node.data.fileUrl.trim();
    const fileName = node.data.fileName || 'document';
    const message = hasFile ? `📎 [Fichier: ${fileName}]` : '📎 [Fichier non configuré]';

    if (node.data.text && node.data.text.trim()) {
      this.addTestMessage('bot', node.data.text, undefined, `Fichier • Nœud ${node.order}`);
      setTimeout(() => {
        this.addTestMessage('bot', message, undefined, `Fichier • Nœud ${node.order}`);
        this.continueFromNode(node);
      }, 1000);
    } else {
      this.addTestMessage('bot', message, undefined, `Fichier • Nœud ${node.order}`);
      this.continueFromNode(node);
    }
  }

  private executeWebhookNode(node: FlowNode): void {
    if (node.data.text && node.data.text.trim()) {
      this.addTestMessage('bot', node.data.text, undefined, `Webhook • Nœud ${node.order}`);
    }

    setTimeout(() => {
      const webhookMessage = node.data.webhookUrl ? `🔗 [Webhook vers: ${node.data.webhookUrl}]` : '🔗 [Webhook non configuré]';

      this.addTestMessage('bot', webhookMessage, undefined, `3CX • Nœud ${node.order}`);
      this.continueFromNode(node);
    }, 1200);
  }

  private executeEndNode(node: FlowNode): void {
    const message = node.data.text || 'Conversation terminée.';
    this.addTestMessage('bot', message, undefined, `Fin • Nœud ${node.order}`);

    setTimeout(() => {
      this.finishTest('Fin du flow atteinte');
    }, 1000);
  }

  private continueFromNode(node: FlowNode): void {
    if (node.nextNodeId) {
      const nextNode = this.getNodeById(node.nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 800);
      }
    } else {
      this.finishTest('Nœud sans suite');
    }
  }
  // Ajouter cette méthode dans votre composant
  // Dans flow-builder-mvp.component.ts - REMPLACER la méthode existante
  private evaluateCustomExpression(expression: string, userInput: string, variables: { [key: string]: any }): boolean {
    if (!expression) return false;

    console.log('🔍 Évaluation expression custom:', {
      expression,
      userInput,
      variables,
    });

    try {
      const input = userInput.toLowerCase().trim();
      const expr = expression.toLowerCase().trim();

      // Patterns de base
      const containsPattern = /result contains '([^']+)'/g;
      const isTextPattern = /result is text/g;
      const isNumberPattern = /result is number/g;
      const isFilePattern = /result is file/g;

      // Remplacer les patterns
      let evalExpr = expr
        .replace(containsPattern, (match, searchTerm) => {
          const result = input.includes(searchTerm.toLowerCase());
          console.log(`  - "${input}" contains "${searchTerm}": ${result}`);
          return result.toString();
        })
        .replace(isTextPattern, () => {
          const result = input.length > 0 && isNaN(Number(input));
          console.log(`  - "${input}" is text: ${result}`);
          return result.toString();
        })
        .replace(isNumberPattern, () => {
          const result = !isNaN(Number(input)) && input !== '';
          console.log(`  - "${input}" is number: ${result}`);
          return result.toString();
        })
        .replace(isFilePattern, () => {
          const result = /\.(pdf|doc|docx|xls|xlsx|jpg|png|gif)$/i.test(input);
          console.log(`  - "${input}" is file: ${result}`);
          return result.toString();
        })
        .replace(/\s+and\s+/g, ' && ')
        .replace(/\s+or\s+/g, ' || ');

      console.log('  - Expression transformée:', evalExpr);

      // Évaluation finale
      const finalResult = eval(evalExpr);
      console.log('  - Résultat final:', finalResult);

      return finalResult;
    } catch (error) {
      console.warn('❌ Erreur expression custom:', error);
      return false;
    }
  }
  // Modifier votre méthode evaluateTestCondition existante
  private evaluateTestCondition(connection: ConditionalConnection, variables: { [key: string]: any }): any {
    if (!this.currentTestNode || !connection.condition) return false;

    // NOUVEAU : Gestion des expressions custom
    if (connection.operator === 'custom_expression') {
      const lastUserMessage = this.testMessages.filter(m => m.type === 'user').slice(-1)[0];
      const userInput = lastUserMessage ? lastUserMessage.content : '';

      return this.evaluateCustomExpression(connection.condition, userInput, variables);
    }

    // ... logique existante pour les autres opérateurs ...
  }
  // ================================
  // GESTION DES INTERACTIONS
  // ================================

  /**
   * Envoyer un message utilisateur
   */
  // Dans flow-builder-mvp.component.ts - REMPLACER cette méthode

  sendTestMessage(): void {
    if (!this.testInputValue.trim() || !this.testSession || this.isTyping) return;

    const userMessage = this.testInputValue.trim();
    this.addTestMessage('user', userMessage);

    // Identifier le type de nœud actuel et traiter en conséquence
    if (this.currentTestNode) {
      console.log(`🎯 Nœud actuel: ${this.currentTestNode.type} (${this.currentTestNode.order})`);

      switch (this.currentTestNode.type) {
        case 'input':
        case 'wait_response':
          // Pour ces types, traiter comme une saisie utilisateur
          this.handleUserInput(this.currentTestNode, userMessage);
          break;

        case 'condition':
          // Pour les conditions, stocker puis évaluer
          this.storeUserChoice(this.currentTestNode, userMessage);
          setTimeout(() => {
            this.executeConditionNode(this.currentTestNode!);
          }, 500);
          break;

        default:
          // Pour les autres types, message d'erreur
          this.addTestMessage(
            'bot',
            "⚠️ Ce nœud n'attend pas de saisie utilisateur. Utilisez les boutons disponibles.",
            undefined,
            'Erreur • Système',
          );
          break;
      }
    } else {
      this.addTestMessage('bot', '⚠️ Aucun nœud actif. Le test semble terminé.', undefined, 'Erreur • Système');
    }

    this.testInputValue = '';
  }

  /**
   * Gérer un clic de bouton
   */
  handleTestButtonClick(button: { text: string; value: string; nodeId?: string }, message: any): void {
    if (!this.testSession) return;

    // Désactiver tous les boutons de ce message
    message.buttonsDisabled = true;
    this.isWaitingForButton = false;

    this.addTestMessage('user', button.text, undefined, undefined, true);

    // Stocker la valeur si le nœud actuel le permet
    if (this.currentTestNode) {
      this.storeUserChoice(this.currentTestNode, button.value);
    }

    // Continuer vers le nœud spécifié ou le nœud suivant
    let nextNodeId = button.nodeId;
    if (!nextNodeId && this.currentTestNode) {
      nextNodeId = this.currentTestNode.nextNodeId;
    }

    if (nextNodeId) {
      const nextNode = this.getNodeById(nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 800);
      }
    } else {
      this.finishTest('Bouton sans destination');
    }
  }

  // Dans flow-builder-mvp.component.ts - REMPLACER cette méthode

  private handleUserInput(node: FlowNode, input: string): void {
    console.log(`📝 Traitement input utilisateur: "${input}" pour nœud ${node.type}`);

    // Stocker la réponse selon le type de nœud
    this.storeUserChoice(node, input);

    // Validation pour les nœuds input avec type spécifique
    if (node.type === 'input' && node.data.responseType) {
      const isValid = this.validateUserInput(input, node.data.responseType);

      if (!isValid && node.data.required) {
        const errorMessage = node.data.validationMessage || 'Format de réponse invalide, veuillez réessayer.';
        this.addTestMessage('bot', `❌ ${errorMessage}`, undefined, `Validation • Nœud ${node.order}`);

        // Redemander la saisie - rester sur le même nœud
        setTimeout(() => {
          this.addTestMessage('bot', node.data.text || 'Veuillez saisir votre réponse:', undefined, `Input retry • Nœud ${node.order}`);
        }, 1000);
        return;
      }
    }

    // ✅ Input valide - continuer vers le nœud suivant
    if (node.nextNodeId) {
      const nextNode = this.getNodeById(node.nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 800);
      } else {
        this.finishTest('Nœud suivant introuvable');
      }
    } else {
      this.finishTest('Aucun nœud suivant configuré');
    }
  }

  // NOUVELLE méthode pour valider les inputs
  private validateUserInput(input: string, responseType: string): boolean {
    switch (responseType) {
      case 'email':
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(input);
      case 'phone':
        return /^[\+]?[0-9\s\-\(\)]{8,}$/.test(input);
      case 'number':
        return !isNaN(Number(input)) && input.trim() !== '';
      case 'text':
      default:
        return input.trim().length > 0;
    }
  }
  private storeUserChoice(node: FlowNode, value: string): void {
    if (!this.testSession) return;

    let variableName = '';

    switch (node.type) {
      case 'buttons':
        variableName = node.data.storeInVariable || 'user_choice';
        break;
      case 'list':
        variableName = node.data.storeInVariable || 'user_selection';
        break;
      case 'input':
        variableName = node.data.storeInVariable || 'user_input';
        break;
      case 'wait_response':
        variableName = node.data.storeInVariable || 'user_response';
        break;
    }

    if (variableName) {
      this.testSession.variables[variableName] = value;
      console.log(`📊 Variable ${variableName} = "${value}"`);
    }
  }

  // ================================
  // MÉTHODES UTILITAIRES
  // ================================

  /**
   * Ajouter un message au chat de test
   */
  // Dans flow-builder-mvp.component.ts - MODIFIER la méthode addTestMessage
  private addTestMessage(
    type: 'bot' | 'user',
    content: string,
    buttons?: Array<{ text: string; value: string; nodeId?: string }>,
    nodeInfo?: string,
    isButton: boolean = false,
  ): void {
    if (type === 'bot') {
      this.isTyping = true;
      setTimeout(
        () => {
          this.testMessages.push({
            id: `msg_${Date.now()}_${Math.random()}`,
            type,
            content,
            buttons,
            timestamp: new Date(),
            isButton,
            nodeInfo,
            buttonsDisabled: false,
          });
          this.isTyping = false;
          this.scrollToBottomTest();
        },
        type === 'bot' ? 800 : 100,
      );
    } else {
      this.testMessages.push({
        id: `msg_${Date.now()}_${Math.random()}`,
        type,
        content,
        buttons,
        timestamp: new Date(),
        isButton,
        nodeInfo,
      });
      this.scrollToBottomTest();
    }
  }

  /**
   * Terminer le test
   */
  private finishTest(reason: string): void {
    console.log('🏁 Test terminé:', reason);
    this.isTestCompleted = true;
    this.isWaitingForButton = false;
    this.currentTestNode = null;

    setTimeout(() => {
      this.addTestMessage('bot', `🔚 Test terminé: ${reason}`, undefined, 'Système');
    }, 1000);
  }

  /**
   * Évaluer une condition de test
   */

  /**
   * Remplacer les variables dans un texte
   */
  private replaceVariables(text: string, variables: { [key: string]: any }): string {
    let result = text;
    Object.keys(variables).forEach(varName => {
      const regex = new RegExp(`\\{${varName}\\}`, 'g');
      result = result.replace(regex, variables[varName]?.toString() || '');
    });
    return result;
  }

  /**
   * Faire défiler vers le bas
   */
  private scrollToBottomTest(): void {
    setTimeout(() => {
      const container = document.querySelector('#messagesContainer');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  // ================================
  // MÉTHODES DE FORMATAGE
  // ================================

  /**
   * Formater l'heure d'un message
   */
  formatMessageTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  }

  /**
   * Formater l'heure de début de test
   */
  formatTestTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  /**
   * Obtenir les variables sous forme de chaîne pour le debug
   */
  getVariablesDebugString(): string {
    if (!this.testSession) return 'Aucune';

    const vars = Object.entries(this.testSession.variables)
      .map(([key, value]) => `${key}=${value}`)
      .join(', ');

    return vars || 'Aucune variable définie';
  }
  addApiHeader(node: FlowNode): void {
    if (!node.data.apiHeaders) {
      node.data.apiHeaders = [];
    }

    node.data.apiHeaders.push({
      id: `header_${Date.now()}`,
      key: '',
      value: '',
      enabled: true,
    });

    this.updateFlowConfig();
  }

  /**
   * Supprimer un header API
   */
  removeApiHeader(node: FlowNode, index: number): void {
    if (node.data.apiHeaders) {
      node.data.apiHeaders.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Ajouter un paramètre API
   */
  addApiParameter(node: FlowNode): void {
    if (!node.data.apiParameters) {
      node.data.apiParameters = [];
    }

    node.data.apiParameters.push({
      id: `param_${Date.now()}`,
      key: '',
      value: '',
      type: 'query',
      enabled: true,
    });

    this.updateFlowConfig();
  }

  /**
   * Supprimer un paramètre API
   */
  removeApiParameter(node: FlowNode, index: number): void {
    if (node.data.apiParameters) {
      node.data.apiParameters.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Ajouter un mapping de réponse
   */
  addResponseMapping(node: FlowNode): void {
    if (!node.data.responseMapping) {
      node.data.responseMapping = [];
    }

    node.data.responseMapping.push({
      id: `mapping_${Date.now()}`,
      jsonPath: '',
      variableName: '',
      enabled: true,
    });

    this.updateFlowConfig();
  }

  /**
   * Supprimer un mapping de réponse
   */
  removeResponseMapping(node: FlowNode, index: number): void {
    if (node.data.responseMapping) {
      node.data.responseMapping.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Tester l'appel API
   */
  testApiCall(node: FlowNode): void {
    if (!node.data.apiUrl) {
      this.showToast('warning', "URL de l'API manquante");
      return;
    }

    this.showToast('info', "Test de l'API en cours...", 2000);

    // Variables de test basiques
    const testVariables = this.generateTestVariables();

    this.apiConnectorService
      .testApiConfiguration(node.data, testVariables)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: result => {
          node.data.lastTestResult = result;
          node.data.isApiValid = result.success;
          this.updateFlowConfig();

          if (result.success) {
            this.showToast('success', `✅ API testée avec succès (${result.responseTime}ms)`);

            // Auto-créer les mappings si la réponse contient des données
            if (result.responseData && (!node.data.responseMapping || node.data.responseMapping.length === 0)) {
              this.suggestResponseMappings(node, result.responseData);
            }
          } else {
            this.showToast('error', `❌ Erreur API: ${result.error || 'Erreur inconnue'}`);
          }
        },
        error: error => {
          node.data.lastTestResult = error;
          node.data.isApiValid = false;
          this.updateFlowConfig();

          this.showToast('error', `❌ Erreur test API: ${error.error || 'Erreur inconnue'}`);
        },
      });
  }

  /**
   * Générer des variables de test pour l'API
   */
  private generateTestVariables(): { [key: string]: any } {
    const testVars: { [key: string]: any } = {};

    // Variables du flow avec valeurs de test
    this.flowConfig.variables.forEach(variable => {
      switch (variable.type) {
        case 'string':
          testVars[variable.name] = variable.value || 'test_value';
          break;
        case 'number':
          testVars[variable.name] = variable.value || 123;
          break;
        case 'boolean':
          testVars[variable.name] = variable.value || true;
          break;
        default:
          testVars[variable.name] = variable.value || 'test';
      }
    });

    // Variables système courantes
    testVars['user_id'] = '12345';
    testVars['user_name'] = 'Test User';
    testVars['user_email'] = 'test@example.com';
    testVars['user_phone'] = '+1234567890';
    testVars['current_date'] = new Date().toISOString().split('T')[0];
    testVars['current_time'] = new Date().toLocaleTimeString();

    return testVars;
  }

  /**
   * Suggérer des mappings automatiques basés sur la réponse
   */
  private suggestResponseMappings(node: FlowNode, responseData: any): void {
    if (!responseData || typeof responseData !== 'object') return;

    const suggestions: ApiResponseMapping[] = [];

    // Analyser la structure de la réponse
    this.analyzeObjectForMappings(responseData, '', suggestions);

    if (suggestions.length > 0) {
      const confirmed = confirm(`L'API a retourné ${suggestions.length} champs. Voulez-vous créer automatiquement les mappings ?`);

      if (confirmed) {
        node.data.responseMapping = suggestions.slice(0, 10); // Limiter à 10 pour éviter l'encombrement
        this.updateFlowConfig();
        this.showToast('success', `${suggestions.length} mappings créés automatiquement`);
      }
    }
  }

  /**
   * Analyser récursivement un objet pour créer des mappings
   */
  private analyzeObjectForMappings(obj: any, prefix: string, suggestions: ApiResponseMapping[], maxDepth: number = 3): void {
    if (maxDepth <= 0 || !obj || typeof obj !== 'object') return;

    Object.keys(obj).forEach(key => {
      const path = prefix ? `${prefix}.${key}` : key;
      const value = obj[key];

      if (value && typeof value === 'object' && !Array.isArray(value)) {
        // Objet imbriqué - continuer récursivement
        this.analyzeObjectForMappings(value, path, suggestions, maxDepth - 1);
      } else if (!Array.isArray(value)) {
        // Valeur primitive - créer un mapping
        suggestions.push({
          id: `mapping_${Date.now()}_${suggestions.length}`,
          jsonPath: path,
          variableName: this.sanitizeVariableName(path),
          enabled: true,
        });
      }
    });
  }

  /**
   * Nettoyer un nom de variable
   */
  private sanitizeVariableName(path: string): string {
    return path
      .toLowerCase()
      .replace(/[^a-z0-9]/g, '_')
      .replace(/_{2,}/g, '_')
      .replace(/^_|_$/g, '');
  }

  /**
   * Obtenir le placeholder pour le corps de requête
   */
  getRequestBodyPlaceholder(bodyType?: string): string {
    switch (bodyType) {
      case 'json':
        return '{\n  "key": "value",\n  "user_id": "{user_id}"\n}';
      case 'form':
        return '{\n  "field1": "value1",\n  "field2": "{variable}"\n}';
      case 'xml':
        return '<?xml version="1.0"?>\n<root>\n  <field>{variable}</field>\n</root>';
      default:
        return 'Corps de la requête...';
    }
  }

  /**
   * Formater la réponse JSON pour affichage
   */
  formatJsonResponse(data: any): string {
    try {
      return JSON.stringify(data, null, 2);
    } catch {
      return String(data);
    }
  }

  /**
   * Obtenir une URL d'affichage raccourcie
   */
  getApiDisplayUrl(url?: string): string {
    if (!url) return 'Non configurée';

    try {
      const urlObj = new URL(url);
      return urlObj.hostname + urlObj.pathname;
    } catch {
      return url.length > 30 ? url.substring(0, 30) + '...' : url;
    }
  }

  // ================================
  // 6. EXÉCUTION DU NŒUD API CONNECTOR DANS LE TEST
  // ================================

  /**
   * Simuler une réponse API réussie
   */
  private simulateApiSuccess(node: FlowNode, result: ApiTestResult): void {
    // Afficher le résultat
    const message = `✅ API ${node.data.apiMethod || 'GET'} réussie (${result.responseTime}ms)\nStatut: ${result.status}`;
    this.addTestMessage('bot', message, undefined, `API Success • Nœud ${node.order}`);

    // Mapper les variables si configuré
    if (node.data.responseMapping && node.data.responseMapping.length > 0 && this.testSession) {
      const mappedVars = this.apiConnectorService.mapResponseToVariables(result.responseData, node.data.responseMapping);

      Object.keys(mappedVars).forEach(varName => {
        this.testSession!.variables[varName] = mappedVars[varName];
      });

      // Afficher les variables mappées
      const varsList = Object.keys(mappedVars)
        .map(name => `${name} = ${mappedVars[name]}`)
        .join('\n');
      if (varsList) {
        this.addTestMessage('bot', `📊 Variables mises à jour:\n${varsList}`, undefined, `Variables • Nœud ${node.order}`);
      }
    }

    // Continuer vers le nœud de succès ou le nœud suivant
    const nextNodeId = node.data.successNextNodeId || node.nextNodeId;
    if (nextNodeId) {
      const nextNode = this.getNodeById(nextNodeId);
      if (nextNode) {
        setTimeout(() => this.executeTestNode(nextNode), 800);
      }
    } else {
      this.finishTest('API réussie mais aucun nœud suivant');
    }
  }
  addFormField(node: FlowNode): void {
    if (!node.data.formFields) {
      node.data.formFields = [];
    }

    const fieldIndex = node.data.formFields.length + 1;
    const newField: WhatsAppFormField = {
      id: `field_${Date.now()}`,
      type: 'text',
      name: `champ_${fieldIndex}`,
      label: `Champ ${fieldIndex}`,
      placeholder: '',
      required: false,
      enabled: true,
      validation: {},
    };

    node.data.formFields.push(newField);
    this.updateFlowConfig();
  }

  /**
   * Supprimer un champ du formulaire
   */
  removeFormField(node: FlowNode, index: number): void {
    if (node.data.formFields && confirm('Supprimer ce champ ?')) {
      node.data.formFields.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Quand le type de champ change
   */
  onFormFieldTypeChange(field: WhatsAppFormField): void {
    // Initialiser les options pour les types qui en ont besoin
    if (['dropdown', 'radio', 'checkbox'].includes(field.type)) {
      if (!field.options || field.options.length === 0) {
        field.options = [
          { id: 'opt1', title: 'Option 1', value: 'option1' },
          { id: 'opt2', title: 'Option 2', value: 'option2' },
        ];
      }
    } else {
      field.options = undefined;
    }

    // Initialiser la validation pour les types texte
    if (['text', 'textarea'].includes(field.type)) {
      if (!field.validation) {
        field.validation = {};
      }
    } else {
      field.validation = undefined;
    }

    this.updateFlowConfig();
  }

  /**
   * Ajouter une option à un champ
   */
  addFieldOption(field: WhatsAppFormField): void {
    if (!field.options) {
      field.options = [];
    }

    const optionIndex = field.options.length + 1;
    field.options.push({
      id: `opt_${Date.now()}`,
      title: `Option ${optionIndex}`,
      value: `option${optionIndex}`,
    });

    this.updateFlowConfig();
  }

  /**
   * Supprimer une option d'un champ
   */
  removeFieldOption(field: WhatsAppFormField, index: number): void {
    if (field.options) {
      field.options.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Ajouter un mapping de réponse
   */
  addFormMapping(node: FlowNode): void {
    if (!node.data.formResponseMapping) {
      node.data.formResponseMapping = [];
    }

    node.data.formResponseMapping.push({
      id: `mapping_${Date.now()}`,
      fieldName: '',
      variableName: '',
      enabled: true,
    });

    this.updateFlowConfig();
  }

  /**
   * Supprimer un mapping de réponse
   */
  removeFormMapping(node: FlowNode, index: number): void {
    if (node.data.formResponseMapping) {
      node.data.formResponseMapping.splice(index, 1);
      this.updateFlowConfig();
    }
  }

  /**
   * Obtenir les champs actifs du formulaire
   */
  getEnabledFormFields(node: FlowNode): WhatsAppFormField[] {
    return (node.data.formFields || []).filter(field => field.enabled);
  }

  /**
   * Appliquer un template de formulaire
   */
  applyFormTemplate(node: FlowNode, templateType: string): void {
    switch (templateType) {
      case 'contact':
        this.applyContactTemplate(node);
        break;
      case 'feedback':
        this.applyFeedbackTemplate(node);
        break;
      case 'registration':
        this.applyRegistrationTemplate(node);
        break;
      case 'survey':
        this.applySurveyTemplate(node);
        break;
    }
    this.updateFlowConfig();
  }

  private applyContactTemplate(node: FlowNode): void {
    node.data.formTitle = 'Informations de contact';
    node.data.formSubtitle = 'Merci de nous fournir vos coordonnées';
    node.data.formFields = [
      {
        id: 'field_name',
        type: 'text',
        name: 'full_name',
        label: 'Nom complet',
        placeholder: 'Votre nom et prénom',
        required: true,
        enabled: true,
        validation: { minLength: 2, maxLength: 50 },
      },
      {
        id: 'field_email',
        type: 'email',
        name: 'email',
        label: 'Adresse email',
        placeholder: 'votre@email.com',
        required: true,
        enabled: true,
        validation: {},
      },
      {
        id: 'field_phone',
        type: 'phone',
        name: 'phone',
        label: 'Téléphone',
        placeholder: '+33123456789',
        required: false,
        enabled: true,
        validation: {},
      },
      {
        id: 'field_company',
        type: 'text',
        name: 'company',
        label: 'Entreprise',
        placeholder: 'Nom de votre entreprise',
        required: false,
        enabled: true,
        validation: {},
      },
    ];
    this.generateAutoMapping(node);
  }

  private applyFeedbackTemplate(node: FlowNode): void {
    node.data.formTitle = 'Votre avis nous intéresse';
    node.data.formSubtitle = 'Aidez-nous à améliorer notre service';
    node.data.formFields = [
      {
        id: 'field_rating',
        type: 'radio',
        name: 'rating',
        label: 'Note globale',
        required: true,
        enabled: true,
        options: [
          { id: 'r1', title: '⭐ 1 - Très insatisfait', value: '1' },
          { id: 'r2', title: '⭐⭐ 2 - Insatisfait', value: '2' },
          { id: 'r3', title: '⭐⭐⭐ 3 - Neutre', value: '3' },
          { id: 'r4', title: '⭐⭐⭐⭐ 4 - Satisfait', value: '4' },
          { id: 'r5', title: '⭐⭐⭐⭐⭐ 5 - Très satisfait', value: '5' },
        ],
        validation: {},
      },
      {
        id: 'field_comment',
        type: 'textarea',
        name: 'comment',
        label: 'Commentaires',
        placeholder: 'Dites-nous ce que vous avez pensé de notre service...',
        required: false,
        enabled: true,
        validation: { maxLength: 500 },
      },
      {
        id: 'field_recommend',
        type: 'radio',
        name: 'recommend',
        label: 'Recommanderiez-vous notre service ?',
        required: true,
        enabled: true,
        options: [
          { id: 'rec_yes', title: 'Oui, certainement', value: 'yes' },
          { id: 'rec_maybe', title: 'Peut-être', value: 'maybe' },
          { id: 'rec_no', title: 'Non', value: 'no' },
        ],
        validation: {},
      },
    ];
    this.generateAutoMapping(node);
  }

  private applyRegistrationTemplate(node: FlowNode): void {
    node.data.formTitle = 'Inscription';
    node.data.formSubtitle = 'Créez votre compte';
    node.data.formFields = [
      {
        id: 'field_firstname',
        type: 'text',
        name: 'first_name',
        label: 'Prénom',
        placeholder: 'Votre prénom',
        required: true,
        enabled: true,
        validation: { minLength: 2 },
      },
      {
        id: 'field_lastname',
        type: 'text',
        name: 'last_name',
        label: 'Nom',
        placeholder: 'Votre nom de famille',
        required: true,
        enabled: true,
        validation: { minLength: 2 },
      },
      {
        id: 'field_email',
        type: 'email',
        name: 'email',
        label: 'Email',
        placeholder: 'votre@email.com',
        required: true,
        enabled: true,
        validation: {},
      },
      {
        id: 'field_birthdate',
        type: 'date',
        name: 'birth_date',
        label: 'Date de naissance',
        required: false,
        enabled: true,
        validation: {},
      },
      {
        id: 'field_interests',
        type: 'checkbox',
        name: 'interests',
        label: "Centres d'intérêt",
        required: false,
        enabled: true,
        options: [
          { id: 'int_tech', title: 'Technologie', value: 'technology' },
          { id: 'int_sport', title: 'Sport', value: 'sport' },
          { id: 'int_culture', title: 'Culture', value: 'culture' },
          { id: 'int_travel', title: 'Voyage', value: 'travel' },
        ],
        validation: {},
      },
    ];
    this.generateAutoMapping(node);
  }

  private applySurveyTemplate(node: FlowNode): void {
    node.data.formTitle = 'Enquête de satisfaction';
    node.data.formSubtitle = 'Quelques questions pour nous améliorer';
    node.data.formFields = [
      {
        id: 'field_usage',
        type: 'dropdown',
        name: 'usage_frequency',
        label: 'À quelle fréquence utilisez-vous notre service ?',
        required: true,
        enabled: true,
        options: [
          { id: 'daily', title: 'Quotidiennement', value: 'daily' },
          { id: 'weekly', title: 'Hebdomadairement', value: 'weekly' },
          { id: 'monthly', title: 'Mensuellement', value: 'monthly' },
          { id: 'rarely', title: 'Rarement', value: 'rarely' },
        ],
        validation: {},
      },
      {
        id: 'field_features',
        type: 'checkbox',
        name: 'desired_features',
        label: 'Quelles fonctionnalités aimeriez-vous voir ajoutées ?',
        required: false,
        enabled: true,
        options: [
          { id: 'feat_mobile', title: 'Application mobile', value: 'mobile_app' },
          { id: 'feat_notif', title: 'Notifications push', value: 'notifications' },
          { id: 'feat_api', title: 'API publique', value: 'public_api' },
          { id: 'feat_integration', title: "Plus d'intégrations", value: 'integrations' },
        ],
        validation: {},
      },
      {
        id: 'field_satisfaction',
        type: 'number',
        name: 'satisfaction_score',
        label: 'Note de satisfaction (1-10)',
        placeholder: 'Entre 1 et 10',
        required: true,
        enabled: true,
        validation: {},
      },
    ];
    this.generateAutoMapping(node);
  }

  /**
   * Générer automatiquement les mappings
   */
  private generateAutoMapping(node: FlowNode): void {
    if (!node.data.formFields) return;

    node.data.formResponseMapping = node.data.formFields
      .filter(field => field.enabled)
      .map(field => ({
        id: `mapping_${field.id}`,
        fieldName: field.name,
        variableName: field.name,
        enabled: true,
      }));
  }

  /**
   * Aperçu du formulaire
   */
  previewWhatsAppForm(node: FlowNode): void {
    const validation = this.whatsappFormService.validateFormConfiguration(node.data);

    if (!validation.isValid) {
      this.showToast('error', `Erreurs de configuration:\n${validation.errors.join('\n')}`);
      return;
    }

    const preview = this.whatsappFormService.generateFormPreview(node.data);
    this.showToast('info', preview, 5000);
  }
  getEnabledFormFieldsCount(node: FlowNode): number {
    if (!node.data.formFields) {
      return 0;
    }
    return node.data.formFields.filter(field => field.enabled).length;
  }
  /**
   * Publier le formulaire WhatsApp
   */
  publishWhatsAppForm(node: FlowNode): void {
    console.log('🚀 Début publication formulaire WhatsApp...');

    // 1. VALIDATION PRÉALABLE
    const validation = this.whatsappFormService.validateFormConfiguration(node.data);

    if (!validation.isValid) {
      console.error('❌ Validation échouée:', validation.errors);
      this.showToast('error', `Impossible de publier le formulaire:\n\n${validation.errors.join('\n')}`);
      return;
    }

    // 2. VÉRIFICATION DES PRÉREQUIS
    if (!this.checkWhatsAppPrerequisites()) {
      this.showToast('error', 'Configuration WhatsApp Business requise. Vérifiez vos tokens et permissions.');
      return;
    }

    // 3. AFFICHAGE DU STATUT
    this.showToast('info', '📋 Publication du formulaire en cours...', 3000);

    // Désactiver temporairement le bouton pour éviter les doublons
    const originalFormPublished = node.data.isFormPublished;

    try {
      // 4. GÉNÉRATION DU JSON DU FLOW WHATSAPP
      console.log('📝 Génération du JSON Flow...');
      const flowJson = this.whatsappFormService.generateFlowJson(node.data);

      if (!flowJson || !flowJson.screens || flowJson.screens.length === 0) {
        throw new Error('Échec de la génération du JSON Flow');
      }

      console.log('✅ JSON Flow généré:', flowJson);

      // 5. CONFIGURATION DU FLOW
      const flowConfig: WhatsAppFlowConfig = {
        name: this.sanitizeFlowName(node.data.formTitle || 'Formulaire WhatsApp'),
        status: 'draft',
        categories: ['OTHER'],
        flowJson: flowJson,
        preview: {
          previewUrl: undefined,
          body: node.data.text || 'Veuillez remplir ce formulaire',
          footer: node.data.formSubtitle || undefined,
          ctaText: node.data.ctaText || 'Ouvrir le formulaire',
        },
      };

      console.log('📋 Configuration Flow:', flowConfig);

      // 6. CRÉATION DU FLOW VIA L'API WHATSAPP
      this.whatsappFormService
        .createWhatsAppFlow(flowConfig)
        .pipe(
          takeUntil(this.destroy$),
          // Timeout après 30 secondes
          timeout(30000),
          // Retry 2 fois en cas d'échec réseau
          retry(2),
        )
        .subscribe({
          next: createResponse => {
            console.log('✅ Flow créé avec succès:', createResponse);

            if (!createResponse.id) {
              throw new Error('ID du Flow non reçu dans la réponse');
            }

            // Sauvegarder l'ID du Flow
            node.data.whatsappFlowId = createResponse.id;
            node.data.whatsappFlowConfig = flowConfig;

            // 7. PUBLICATION DU FLOW
            console.log('📤 Publication du Flow ID:', createResponse.id);
            this.showToast('info', '📤 Publication en cours...', 2000);

            this.whatsappFormService
              .publishWhatsAppFlow(createResponse.id)
              .pipe(takeUntil(this.destroy$), timeout(20000), retry(1))
              .subscribe({
                next: publishResponse => {
                  console.log('✅ Flow publié avec succès:', publishResponse);

                  // 8. SUCCÈS FINAL
                  node.data.isFormPublished = true;
                  node.data.formPreviewUrl = publishResponse.preview_url || undefined;

                  // Sauvegarder automatiquement le flow
                  this.updateFlowConfig();

                  // 9. NOTIFICATION DE SUCCÈS
                  this.showToast(
                    'success',
                    '🎉 Formulaire WhatsApp publié avec succès !\n\nLe formulaire est maintenant disponible dans votre chatbot.',
                  );

                  // 10. LOG FINAL
                  console.log('🎯 Publication terminée avec succès:', {
                    flowId: createResponse.id,
                    formTitle: node.data.formTitle,
                    fieldsCount: this.getEnabledFormFieldsCount(node),
                    previewUrl: node.data.formPreviewUrl,
                  });

                  // 11. CRÉER AUTOMATIQUEMENT LES VARIABLES SI NÉCESSAIRE
                  this.createVariablesFromFormMapping(node);
                },
                error: publishError => {
                  console.error('❌ Erreur lors de la publication:', publishError);
                  node.data.isFormPublished = originalFormPublished;

                  this.handlePublishError(publishError, 'publication');
                },
              });
          },
          error: createError => {
            console.error('❌ Erreur lors de la création:', createError);
            node.data.isFormPublished = originalFormPublished;

            this.handlePublishError(createError, 'création');
          },
        });
    } catch (error: any) {
      console.error('❌ Erreur générale:', error);
      node.data.isFormPublished = originalFormPublished;
      this.showToast('error', `❌ Erreur inattendue: ${error.message}`);
    }
  }

  /**
   * Vérifier les prérequis WhatsApp Business
   */
  private checkWhatsAppPrerequisites(): boolean {
    // Vérification basique - à adapter selon votre configuration
    const hasAccessToken = this.whatsappFormService.hasValidAccessToken();
    const hasBusinessAccount = this.whatsappFormService.hasBusinessAccount();

    return hasAccessToken && hasBusinessAccount;
  }

  /**
   * Nettoyer le nom du Flow pour l'API WhatsApp
   */
  private sanitizeFlowName(name: string): string {
    return name
      .trim()
      .replace(/[^a-zA-Z0-9\s\-_]/g, '') // Supprimer caractères spéciaux
      .substring(0, 100) // Limiter à 100 caractères
      .trim();
  }

  /**
   * Gérer les erreurs de publication
   */
  private handlePublishError(error: any, phase: 'création' | 'publication'): void {
    console.error(`❌ Erreur ${phase}:`, error);

    let errorMessage = `❌ Erreur lors de la ${phase} du formulaire`;

    // Messages d'erreur spécifiques selon le code d'erreur
    if (error.status) {
      switch (error.status) {
        case 400:
          errorMessage +=
            '\n\n🔍 Erreur de configuration. Vérifiez:\n• Tous les champs obligatoires\n• Format des données\n• Validation des options';
          break;
        case 401:
          errorMessage += "\n\n🔐 Erreur d'authentification. Vérifiez:\n• Token d'accès WhatsApp\n• Permissions du compte Business";
          break;
        case 403:
          errorMessage +=
            '\n\n⛔ Permissions insuffisantes. Vérifiez:\n• Droits sur le compte WhatsApp Business\n• Statut de vérification du compte';
          break;
        case 429:
          errorMessage += '\n\n⏰ Limite de taux atteinte. Réessayez dans quelques minutes.';
          break;
        case 500:
          errorMessage += '\n\n🔧 Erreur serveur WhatsApp. Réessayez plus tard.';
          break;
        default:
          errorMessage += `\n\n📊 Code d'erreur: ${error.status}`;
      }
    }

    // Ajouter détails techniques si disponibles
    if (error.error && error.error.error && error.error.error.message) {
      errorMessage += `\n\n💻 Détail technique: ${error.error.error.message}`;
    }

    this.showToast('error', errorMessage, 8000);
  }

  /**
   * Créer automatiquement les variables depuis le mapping du formulaire
   */
  private createVariablesFromFormMapping(node: FlowNode): void {
    if (!node.data.formResponseMapping) return;

    let createdCount = 0;

    node.data.formResponseMapping
      .filter(mapping => mapping.enabled && mapping.variableName)
      .forEach(mapping => {
        const existingVar = this.flowConfig.variables.find(v => v.name === mapping.variableName);

        if (!existingVar) {
          // Déterminer le type de variable selon le champ
          const field = node.data.formFields?.find(f => f.name === mapping.fieldName);
          let varType = 'string';

          if (field) {
            switch (field.type) {
              case 'number':
                varType = 'number';
                break;
              case 'checkbox':
                varType = 'array';
                break;
              default:
                varType = 'string';
            }
          }

          const newVariable: FlowVariable = {
            name: mapping.variableName,
            value: varType === 'number' ? 0 : varType === 'array' ? [] : '',
            type: varType,
            description: `Variable créée automatiquement depuis le formulaire: ${field?.label || mapping.fieldName}`,
            isSystem: false,
          };

          this.flowConfig.variables.push(newVariable);
          createdCount++;
        }
      });

    if (createdCount > 0) {
      this.updateFlowConfig();
      console.log(`✅ ${createdCount} variables créées automatiquement`);
      this.showToast('info', `📊 ${createdCount} variable(s) créée(s) automatiquement pour le formulaire`);
    }
  }
}
