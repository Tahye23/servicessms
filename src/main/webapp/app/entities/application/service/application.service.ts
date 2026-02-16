import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import {
  IApplication,
  NewApplication,
  IApplicationStats,
  ITokenResponse,
  ITokenCreateResponse,
  ITokenValidationRequest,
  ITokenValidationResponse,
  ITokenStats,
  ICreateTokenRequest,
  ITokensApp,
} from '../application.model';

export type EntityResponseType = HttpResponse<IApplication>;
export type EntityArrayResponseType = HttpResponse<IApplication[]>;

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/applications');
  protected resourceTokenUrl = this.applicationConfigService.getEndpointFor('api/tokens-apps');

  constructor(
    protected http: HttpClient,
    protected applicationConfigService: ApplicationConfigService,
  ) {}

  /**
   * ✅ MÉTHODE MODIFIÉE: Crée une application avec gestion du token
   * Envoie tokenNeverExpires et dateExpiration au backend
   */
  create(application: NewApplication): Observable<EntityResponseType> {
    // Préparer le payload
    const payload: any = {
      name: application.name,
      description: application.description,
      userId: application.userId,
      environment: application.environment,
      webhookUrl: application.webhookUrl,
      webhookSecret: application.webhookSecret,
      dailyLimit: application.dailyLimit,
      monthlyLimit: application.monthlyLimit,
      allowedServices: application.allowedServices,
      isActive: application.isActive !== undefined ? application.isActive : true,

      // ✅ CHAMPS DU TOKEN
      tokenNeverExpires: application.tokenNeverExpires || false,
      dateExpiration: application.tokenNeverExpires ? null : application.tokenDateExpiration,
    };

    // Nettoyer les valeurs undefined
    Object.keys(payload).forEach(key => {
      if (payload[key] === undefined) {
        delete payload[key];
      }
    });

    return this.http.post<IApplication>(this.resourceUrl, payload, { observe: 'response' });
  }

  /**
   * ✅ MÉTHODE MODIFIÉE: Met à jour une application
   * N'envoie PAS les champs du token (tokenDateExpiration, tokenNeverExpires)
   */
  update(application: IApplication): Observable<EntityResponseType> {
    // Créer une copie sans les champs du token
    const { tokenDateExpiration, tokenNeverExpires, tokens, ...updatePayload } = application;

    return this.http.put<IApplication>(`${this.resourceUrl}/${this.getApplicationIdentifier(application)}`, updatePayload, {
      observe: 'response',
    });
  }

  /**
   * CORRIGÉ: Accepte Partial<IApplication> et vérifie que id existe
   */
  partialUpdate(application: Partial<IApplication>): Observable<EntityResponseType> {
    // Exclure les champs du token lors des mises à jour partielles
    const { tokenDateExpiration, tokenNeverExpires, tokens, ...updatePayload } = application;

    return this.http.patch<IApplication>(`${this.resourceUrl}/${this.getApplicationIdentifier(application)}`, updatePayload, {
      observe: 'response',
    });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<IApplication>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IApplication[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /**
   * CORRIGÉ: Accepte Partial et gère le cas où id pourrait être undefined
   */
  getApplicationIdentifier(application: Pick<IApplication, 'id'> | Partial<IApplication>): number {
    return application.id as number;
  }

  /**
   * CORRIGÉ: Permet des valeurs undefined/null et fait la comparaison en toute sécurité
   */
  compareApplications(o1: Pick<IApplication, 'id'> | null | undefined, o2: Pick<IApplication, 'id'> | null | undefined): boolean {
    return o1 && o2 ? this.getApplicationIdentifier(o1) === this.getApplicationIdentifier(o2) : o1 === o2;
  }

  /**
   * Alias pour compareApplications (pour la compatibilité)
   */
  compareApplication(o1: IApplication | null | undefined, o2: IApplication | null | undefined): boolean {
    return this.compareApplications(o1, o2);
  }

  // ==================== TOKENS MANAGEMENT ====================

  /**
   * Récupère tous les tokens d'une application
   */
  getApplicationTokens(applicationId: number): Observable<HttpResponse<ITokensApp[]>> {
    return this.http.get<ITokensApp[]>(`${this.resourceUrl}/${applicationId}/tokens`, { observe: 'response' });
  }

  /**
   * Récupère le token actif d'une application
   */
  getActiveToken(applicationId: number): Observable<HttpResponse<ITokensApp>> {
    return this.http.get<ITokensApp>(`${this.resourceUrl}/${applicationId}/tokens/active`, { observe: 'response' });
  }

  /**
   * Crée un nouveau token avec date d'expiration personnalisée
   */
  createToken(request: ICreateTokenRequest): Observable<HttpResponse<ITokensApp>> {
    return this.http.post<ITokensApp>(`${this.resourceTokenUrl}/create-with-expiration`, request, {
      observe: 'response',
    });
  }

  /**
   * Régénère un token existant
   */
  regenerateToken(tokenId: number): Observable<HttpResponse<ITokensApp>> {
    return this.http.post<ITokensApp>(`${this.resourceTokenUrl}/${tokenId}/regenerate`, {}, { observe: 'response' });
  }

  /**
   * Active un token
   */
  activateToken(tokenId: number): Observable<HttpResponse<ITokensApp>> {
    return this.http.post<ITokensApp>(`${this.resourceTokenUrl}/${tokenId}/activate`, {}, { observe: 'response' });
  }

  /**
   * Désactive un token
   */
  deactivateToken(tokenId: number): Observable<HttpResponse<ITokensApp>> {
    return this.http.post<ITokensApp>(`${this.resourceTokenUrl}/${tokenId}/deactivate`, {}, { observe: 'response' });
  }

  /**
   * Supprime un token
   */
  deleteToken(tokenId: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceTokenUrl}/${tokenId}`, { observe: 'response' });
  }

  /**
   * Valide un token
   */
  validateToken(request: ITokenValidationRequest): Observable<HttpResponse<ITokenValidationResponse>> {
    return this.http.post<ITokenValidationResponse>(`${this.resourceTokenUrl}/validate`, request, {
      observe: 'response',
    });
  }

  /**
   * Récupère les statistiques des tokens pour une application
   */
  getTokenStats(applicationId: number): Observable<HttpResponse<ITokenStats>> {
    return this.http.get<ITokenStats>(`${this.resourceUrl}/${applicationId}/tokens/stats`, { observe: 'response' });
  }

  /**
   * Récupère les tokens qui expirent bientôt
   */
  getExpiringTokens(applicationId: number): Observable<HttpResponse<ITokensApp[]>> {
    return this.http.get<ITokensApp[]>(`${this.resourceUrl}/${applicationId}/tokens/expiring`, { observe: 'response' });
  }

  /**
   * Vérifie si une application a un token valide
   */
  hasValidToken(applicationId: number): Observable<HttpResponse<{ hasValidToken: boolean }>> {
    return this.http.get<{ hasValidToken: boolean }>(`${this.resourceUrl}/${applicationId}/tokens/valid`, {
      observe: 'response',
    });
  }

  // ==================== APPLICATION MANAGEMENT ====================

  /**
   * Récupère les statistiques globales des applications
   */
  getStats(): Observable<IApplicationStats> {
    return this.http.get<IApplicationStats>(`${this.resourceUrl}/stats`);
  }

  /**
   * Met à jour le statut d'une application
   */
  updateStatus(applicationId: number, isActive: boolean): Observable<EntityResponseType> {
    return this.http.patch<IApplication>(`${this.resourceUrl}/${applicationId}`, { isActive }, { observe: 'response' });
  }

  /**
   * Réinitialise les limites d'utilisation d'une application
   */
  resetUsageLimits(applicationId: number): Observable<EntityResponseType> {
    return this.http.post<IApplication>(`${this.resourceUrl}/${applicationId}/reset-usage`, {}, { observe: 'response' });
  }

  /**
   * Obtient l'utilisation API d'une application
   */
  getUsageStats(applicationId: number): Observable<HttpResponse<any>> {
    return this.http.get<any>(`${this.resourceUrl}/${applicationId}/usage`, { observe: 'response' });
  }

  /**
   * Ajoute un service autorisé à une application
   */
  addAllowedService(applicationId: number, service: string): Observable<EntityResponseType> {
    return this.http.post<IApplication>(
      `${this.resourceUrl}/${applicationId}/services/${service}`,
      {},
      {
        observe: 'response',
      },
    );
  }

  /**
   * Supprime un service autorisé d'une application
   */
  removeAllowedService(applicationId: number, service: string): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${applicationId}/services/${service}`, { observe: 'response' });
  }

  // ==================== WEBHOOK MANAGEMENT ====================

  /**
   * Valide l'URL du webhook
   */
  validateWebhookUrl(url: string): Observable<HttpResponse<{ valid: boolean; message: string }>> {
    return this.http.post<{ valid: boolean; message: string }>(`${this.resourceUrl}/validate-webhook`, { url }, { observe: 'response' });
  }

  /**
   * Teste un webhook
   */
  testWebhook(applicationId: number, testData: any): Observable<HttpResponse<any>> {
    return this.http.post<any>(`${this.resourceUrl}/${applicationId}/test-webhook`, testData, { observe: 'response' });
  }

  // ==================== ADVANCED OPERATIONS ====================

  /**
   * Réinitialise les compteurs d'utilisation
   */
  resetUsageCounters(applicationId: number): Observable<EntityResponseType> {
    return this.http.post<IApplication>(`${this.resourceUrl}/${applicationId}/reset-counters`, {}, { observe: 'response' });
  }

  /**
   * Récupère l'historique des appels API
   */
  getApiCallHistory(applicationId: number, params?: any): Observable<HttpResponse<any[]>> {
    return this.http.get<any[]>(`${this.resourceUrl}/${applicationId}/api-call-history`, {
      params: createRequestOption(params),
      observe: 'response',
    });
  }

  /**
   * Exporte les données de l'application
   */
  exportApplicationData(applicationId: number, format: 'json' | 'csv'): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${applicationId}/export?format=${format}`, {
      responseType: 'blob',
    });
  }

  /**
   * Clone une application
   */
  cloneApplication(applicationId: number, newName: string): Observable<EntityResponseType> {
    return this.http.post<IApplication>(`${this.resourceUrl}/${applicationId}/clone`, { newName }, { observe: 'response' });
  }

  // ==================== COLLECTION HELPERS ====================

  /**
   * Ajoute des applications manquantes à une collection
   */
  addApplicationToCollectionIfMissing<Type extends { id: number | null | undefined }>(
    applicationCollection: Type[],
    ...applicationsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const applications: Type[] = applicationsToCheck.filter(isPresent);
    if (applications.length > 0) {
      const applicationCollectionIdentifiers = applicationCollection.map(a => a.id);
      applications.forEach(application => {
        if (!applicationCollectionIdentifiers.includes(application.id)) {
          applicationCollection.push(application);
        }
      });
    }
    return applicationCollection;
  }
}
