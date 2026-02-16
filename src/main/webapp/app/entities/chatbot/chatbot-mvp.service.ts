// ================================
// SERVICE ANGULAR CORRIGÉ - COHÉRENT AVEC LE BACKEND
// ================================

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApplicationConfigService } from '../../core/config/application-config.service';
import { ApiResponse, FlowConfig, FlowNode, SaveFlowResponse, FlowVariable } from './chatbot-mvp.models';

@Injectable({
  providedIn: 'root',
})
export class ChatbotMvpService {
  private applicationConfigService = inject(ApplicationConfigService);
  private apiUrl = this.applicationConfigService.getEndpointFor('api/chatbot-flows');

  // État local pour le flow actuel
  private currentFlowSubject = new BehaviorSubject<FlowConfig | null>(null);
  public currentFlow$ = this.currentFlowSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadFlowFromStorage();
  }

  // ================================
  // FONCTION 1: SAUVEGARDER LE FLOW (CREATE/EDIT)
  // ================================

  /**
   * Sauvegarde le flow - création ou modification automatique
   */
  saveFlow(flowConfig: FlowConfig): Observable<SaveFlowResponse> {
    console.log('💾 Sauvegarde du flow:', flowConfig.name);
    console.log('📊 Données:', {
      nodes: flowConfig.nodes.length,
      variables: flowConfig.variables.length,
      flowId: flowConfig.flowId,
    });

    // Le payload est exactement votre FlowConfig
    const payload = {
      ...flowConfig,
      updatedAt: new Date().toISOString(),
    };

    return this.http.post<ApiResponse<SaveFlowResponse>>(`${this.apiUrl}/save`, payload).pipe(
      map(response => {
        if (response.success && response.data) {
          // Mettre à jour le flow local avec l'ID retourné
          const updatedFlow = {
            ...flowConfig,
            flowId: response.data.id.toString(),
            updatedAt: new Date().toISOString(),
          };
          this.setCurrentFlow(updatedFlow);

          console.log('✅ Flow sauvegardé avec succès, ID:', response.data.id);
          return response.data;
        } else {
          throw new Error(response.message || 'Erreur lors de la sauvegarde');
        }
      }),
      catchError(this.handleError),
    );
  }

  // ================================
  // FONCTION 2: CHARGER LE FLOW ACTUEL
  // ================================

  /**
   * Charge le flow actuel depuis la base de données
   */
  loadCurrentFlow(): Observable<FlowConfig | null> {
    console.log('📥 Chargement du flow actuel...');

    return this.http.get<ApiResponse<FlowConfig>>(`${this.apiUrl}/current`).pipe(
      map(response => {
        if (response.success && response.data) {
          console.log('✅ Flow chargé:', response.data.name);
          console.log('📊 Contenu:', {
            nodes: response.data.nodes?.length || 0,
            variables: response.data.variables?.length || 0,
            flowId: response.data.flowId,
          });

          // Mettre à jour l'état local
          this.setCurrentFlow(response.data);
          return response.data;
        } else {
          console.log("ℹ️ Aucun flow trouvé, création d'un flow par défaut");
          // Pas de flow, créer un flow par défaut
          const defaultFlow = this.createDefaultFlow();
          return defaultFlow;
        }
      }),
      catchError(error => {
        console.error('❌ Erreur chargement flow:', error);
        // En cas d'erreur, créer un flow par défaut
        const defaultFlow = this.createDefaultFlow();
        return of(defaultFlow);
      }),
    );
  }

  // ================================
  // MÉTHODES UTILITAIRES (compatibilité avec votre code existant)
  // ================================

  /**
   * Obtenir le flow actuel depuis l'observable
   */
  getCurrentFlow(): Observable<FlowConfig | null> {
    return this.currentFlow$;
  }

  /**
   * Définir le flow actuel
   */
  setCurrentFlow(flow: FlowConfig): void {
    this.currentFlowSubject.next(flow);
    this.saveFlowToStorage(flow);
  }

  /**
   * Créer un nouveau flow par défaut
   */
  createNewFlow(partnerId: number, name: string): FlowConfig {
    const newFlow: FlowConfig = {
      partnerId,
      flowId: `flow_${Date.now()}`,
      name,
      description: '',
      active: false,
      nodes: [
        {
          id: 'start_node',
          type: 'start',
          x: 100,
          y: 100,
          data: { text: 'Bienvenue ! Comment puis-je vous aider ?' },
          order: 1,
        },
      ],
      variables: [],
      language: 'fr',
      createdAt: new Date().toISOString(),
    };

    this.setCurrentFlow(newFlow);
    console.log('🆕 Nouveau flow créé:', name);
    return newFlow;
  }

  /**
   * Créer un flow par défaut
   */
  private createDefaultFlow(): FlowConfig {
    return this.createNewFlow(1, 'Mon Premier Flow');
  }

  /**
   * Valider un flow (validation côté client)
   */
  validateFlow(flowConfig: FlowConfig): Observable<{ isValid: boolean; errors: string[] }> {
    const errors: string[] = [];

    // Validation de base
    if (!flowConfig.name || flowConfig.name.trim() === '') {
      errors.push('Le nom du flow est obligatoire');
    }

    if (!flowConfig.nodes || flowConfig.nodes.length === 0) {
      errors.push('Le flow doit contenir au moins un nœud');
    }

    const hasStartNode = flowConfig.nodes.some(n => n.type === 'start');
    if (!hasStartNode) {
      errors.push('Le flow doit avoir un nœud de démarrage');
    }

    // Validation des connexions
    flowConfig.nodes.forEach(node => {
      if (node.type === 'buttons' && (!node.data.buttons || node.data.buttons.length === 0)) {
        errors.push(`Nœud ${node.order} (boutons): Au moins un bouton requis`);
      }

      if (node.type === 'list' && (!node.data.items || node.data.items.length === 0)) {
        errors.push(`Nœud ${node.order} (liste): Au moins une option requise`);
      }

      if (node.type === 'condition' && !node.data.variable) {
        errors.push(`Nœud ${node.order} (condition): Variable de test manquante`);
      }

      if (node.type === 'webhook' && !node.data.webhookUrl) {
        errors.push(`Nœud ${node.order} (webhook): URL manquante`);
      }
    });

    console.log('🔍 Validation flow:', {
      isValid: errors.length === 0,
      errors: errors.length,
      nodes: flowConfig.nodes.length,
    });

    return of({
      isValid: errors.length === 0,
      errors,
    });
  }

  /**
   * Test du flow (simulation côté client)
   */
  testFlow(flowConfig: FlowConfig): Observable<any> {
    console.log('🧪 Test du flow (simulation):', flowConfig.name);

    return of({
      id: `test_${Date.now()}`,
      status: 'SUCCESS',
      message: 'Test du flow simulé avec succès',
      flow: flowConfig.name,
      nodes: flowConfig.nodes.length,
      startNode: flowConfig.nodes.find(n => n.type === 'start')?.id,
    });
  }

  // ================================
  // GESTION DU STOCKAGE LOCAL
  // ================================

  private saveFlowToStorage(flow: FlowConfig): void {
    try {
      localStorage.setItem('currentFlow', JSON.stringify(flow));
      console.log('💾 Flow sauvegardé localement');
    } catch (error) {
      console.warn('⚠️ Impossible de sauvegarder le flow localement', error);
    }
  }

  private loadFlowFromStorage(): void {
    try {
      const stored = localStorage.getItem('currentFlow');
      if (stored) {
        const flow = JSON.parse(stored);
        this.currentFlowSubject.next(flow);
        console.log('📥 Flow chargé depuis le stockage local:', flow.name);
      }
    } catch (error) {
      console.warn('⚠️ Impossible de charger le flow depuis le storage', error);
      localStorage.removeItem('currentFlow');
    }
  }

  /**
   * Effacer le flow local
   */
  clearLocalFlow(): void {
    localStorage.removeItem('currentFlow');
    this.currentFlowSubject.next(null);
    console.log('🗑️ Flow local effacé');
  }

  // ================================
  // UPLOAD FICHIERS (optionnel - pour compatibilité)
  // ================================

  // Dans chatbot-mvp.service.ts - MODIFIER la méthode uploadImage existante

  /**
   * Upload image vers Meta WhatsApp (compatible avec votre structure existante)
   */
  uploadImage(file: File): Observable<{ url: string; filename: string; mediaId?: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'image');

    console.log('📤 Upload image vers Meta:', file.name, file.size, 'bytes');

    return this.http
      .post<{
        success: boolean;
        data: {
          url: string;
          mediaId: string;
          filename: string;
          mimeType: string;
          fileSize: number;
          provider: string;
        };
        message: string;
      }>('/api/chatbot-flows/upload-media', formData)
      .pipe(
        map(response => {
          if (response.success && response.data) {
            console.log('✅ Image uploadée vers Meta:', {
              mediaId: response.data.mediaId,
              filename: response.data.filename,
              size: response.data.fileSize,
            });

            // Retourner dans le format attendu par votre composant
            return {
              url: response.data.url, // "meta://123456"
              filename: response.data.filename,
              mediaId: response.data.mediaId,
            };
          } else {
            throw new Error(response.message || 'Erreur upload');
          }
        }),
        catchError(error => {
          console.error('❌ Erreur upload Meta:', error);
          const errorMessage = error.error?.message || error.message || 'Erreur upload';
          return throwError(() => new Error(`Erreur upload WhatsApp: ${errorMessage}`));
        }),
      );
  }

  /**
   * Upload fichier vers Meta WhatsApp (compatible avec votre structure existante)
   */
  uploadFile(file: File): Observable<{ url: string; filename: string; mediaId?: string }> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'document');

    console.log('📤 Upload fichier vers Meta:', file.name, file.size, 'bytes');

    return this.http
      .post<{
        success: boolean;
        data: {
          url: string;
          mediaId: string;
          filename: string;
          mimeType: string;
          fileSize: number;
          provider: string;
        };
        message: string;
      }>('/api/chatbot-flows/upload-media', formData)
      .pipe(
        map(response => {
          if (response.success && response.data) {
            console.log('✅ Fichier uploadé vers Meta:', {
              mediaId: response.data.mediaId,
              filename: response.data.filename,
              size: response.data.fileSize,
            });

            // Retourner dans le format attendu par votre composant
            return {
              url: response.data.url, // "meta://123456"
              filename: response.data.filename,
              mediaId: response.data.mediaId,
            };
          } else {
            throw new Error(response.message || 'Erreur upload');
          }
        }),
        catchError(error => {
          console.error('❌ Erreur upload fichier Meta:', error);
          const errorMessage = error.error?.message || error.message || 'Erreur upload';
          return throwError(() => new Error(`Erreur upload fichier WhatsApp: ${errorMessage}`));
        }),
      );
  }
  // ================================
  // IMPORT/EXPORT (optionnel - pour compatibilité)
  // ================================

  exportFlow(flowConfig: FlowConfig): string {
    const exportData = {
      version: '2.0',
      exportedAt: new Date().toISOString(),
      flow: flowConfig,
    };

    console.log('📤 Export flow:', flowConfig.name);
    return JSON.stringify(exportData, null, 2);
  }

  importFlow(jsonData: string): Observable<FlowConfig> {
    try {
      console.log('📥 Import flow depuis JSON...');

      const importData = JSON.parse(jsonData);
      let flow: FlowConfig;

      if (importData.flow) {
        flow = importData.flow;
      } else {
        flow = importData;
      }

      // Générer de nouveaux IDs pour éviter les conflits
      flow.flowId = `flow_${Date.now()}`;
      flow.nodes.forEach((node: FlowNode) => {
        const oldId = node.id;
        node.id = `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

        // Mettre à jour les références dans les autres nœuds
        flow.nodes.forEach((n: FlowNode) => {
          if (n.nextNodeId === oldId) {
            n.nextNodeId = node.id;
          }

          // Mettre à jour les références dans les boutons
          if (n.data.buttons) {
            n.data.buttons.forEach(button => {
              if (button.nextNodeId === oldId) {
                button.nextNodeId = node.id;
              }
            });
          }

          // Mettre à jour les références dans les items de liste
          if (n.data.items) {
            n.data.items.forEach(item => {
              if (item.nextNodeId === oldId) {
                item.nextNodeId = node.id;
              }
            });
          }

          // Mettre à jour les connexions conditionnelles
          if (n.data.conditionalConnections) {
            n.data.conditionalConnections.forEach(conn => {
              if (conn.nextNodeId === oldId) {
                conn.nextNodeId = node.id;
              }
            });
          }
        });
      });

      this.setCurrentFlow(flow);
      console.log('✅ Flow importé:', flow.name);
      return of(flow);
    } catch (error) {
      console.error('❌ Erreur import:', error);
      return throwError(() => new Error('Format de fichier invalide'));
    }
  }

  // ================================
  // GESTION DES ERREURS
  // ================================

  private handleError(error: any): Observable<never> {
    let errorMessage = 'Une erreur est survenue';

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      switch (error.status) {
        case 0:
          errorMessage = 'Impossible de contacter le serveur';
          break;
        case 401:
          errorMessage = 'Session expirée, veuillez vous reconnecter';
          break;
        case 403:
          errorMessage = 'Accès refusé';
          break;
        case 404:
          errorMessage = 'Ressource non trouvée';
          break;
        case 422:
          errorMessage = error.error?.message || 'Données invalides';
          break;
        case 500:
          errorMessage = 'Erreur serveur, veuillez réessayer plus tard';
          break;
        default:
          errorMessage = error.error?.message || `Erreur ${error.status}`;
      }
    }

    console.error('❌ Service Error:', {
      status: error.status,
      message: errorMessage,
      error: error,
    });

    return throwError(() => new Error(errorMessage));
  }

  // ================================
  // GETTERS/SETTERS POUR COMPATIBILITÉ
  // ================================

  getCurrentPartnerId(): number {
    const stored = localStorage.getItem('currentPartnerId');
    return stored ? parseInt(stored, 10) : 1;
  }

  setCurrentPartnerId(partnerId: number): void {
    localStorage.setItem('currentPartnerId', partnerId.toString());
    console.log('🏢 Partner ID défini:', partnerId);
  }
}
