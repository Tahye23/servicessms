import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { timeout, retry, catchError, map } from 'rxjs/operators';
import { ApiResponseMapping, ApiTestResult, NodeData } from './chatbot-mvp.models';

@Injectable({
  providedIn: 'root',
})
export class ApiConnectorService {
  constructor(private http: HttpClient) {}

  /**
   * Exécuter un appel API avec la configuration fournie
   */
  executeApiCall(nodeData: NodeData, variables: { [key: string]: any }): Observable<ApiTestResult> {
    const startTime = Date.now();

    try {
      // Construire l'URL avec variables
      const url = this.replaceVariables(nodeData.apiUrl || '', variables);

      // Construire les headers
      const headers = this.buildHeaders(nodeData, variables);

      // Construire les paramètres
      const params = this.buildParams(nodeData, variables);

      // Construire le body
      const body = this.buildRequestBody(nodeData, variables);

      // Configuration de la requête
      const options = {
        headers,
        params,
        observe: 'response' as const,
        responseType: 'json' as const,
      };

      // Exécuter la requête selon la méthode
      let request$: Observable<any>;

      switch (nodeData.apiMethod) {
        case 'GET':
          request$ = this.http.get(url, options);
          break;
        case 'POST':
          request$ = this.http.post(url, body, options);
          break;
        case 'PUT':
          request$ = this.http.put(url, body, options);
          break;
        case 'DELETE':
          request$ = this.http.delete(url, options);
          break;
        case 'PATCH':
          request$ = this.http.patch(url, body, options);
          break;
        default:
          throw new Error(`Méthode HTTP non supportée: ${nodeData.apiMethod}`);
      }

      // Appliquer timeout et retry
      return request$
        .pipe(
          timeout(nodeData.timeout || 10000),
          retry(nodeData.retryCount || 0),
          catchError(error => {
            const endTime = Date.now();
            return throwError({
              success: false,
              status: error.status || 0,
              statusText: error.statusText || 'Erreur réseau',
              responseTime: endTime - startTime,
              error: error.message || 'Erreur inconnue',
              responseData: error.error || null,
            });
          }),
        )
        .pipe(
          // Mapper la réponse de succès
          catchError(error => throwError(error)),
          // Transformer en ApiTestResult
          map((response: any) => {
            const endTime = Date.now();
            return {
              success: true,
              status: response.status,
              statusText: response.statusText,
              responseTime: endTime - startTime,
              responseData: response.body,
              headers: this.extractHeaders(response.headers),
            };
          }),
        );
    } catch (error: any) {
      const endTime = Date.now();
      return throwError({
        success: false,
        status: 0,
        statusText: 'Erreur de configuration',
        responseTime: endTime - startTime,
        error: error.message,
        responseData: null,
      });
    }
  }

  /**
   * Tester une API avant intégration
   */
  testApiConfiguration(nodeData: NodeData, testVariables: { [key: string]: any } = {}): Observable<ApiTestResult> {
    console.log("🧪 Test de l'API:", nodeData.apiUrl);
    return this.executeApiCall(nodeData, testVariables);
  }

  /**
   * Construire les headers HTTP
   */
  private buildHeaders(nodeData: NodeData, variables: { [key: string]: any }): HttpHeaders {
    let headers = new HttpHeaders();

    // Headers personnalisés
    if (nodeData.apiHeaders) {
      nodeData.apiHeaders
        .filter(header => header.enabled)
        .forEach(header => {
          const value = this.replaceVariables(header.value, variables);
          headers = headers.set(header.key, value);
        });
    }

    // Authentification
    if (nodeData.authType !== 'none') {
      headers = this.addAuthHeaders(headers, nodeData, variables);
    }

    return headers;
  }

  /**
   * Ajouter les headers d'authentification
   */
  private addAuthHeaders(headers: HttpHeaders, nodeData: NodeData, variables: { [key: string]: any }): HttpHeaders {
    switch (nodeData.authType) {
      case 'bearer':
        if (nodeData.authToken) {
          const token = this.replaceVariables(nodeData.authToken, variables);
          headers = headers.set('Authorization', `Bearer ${token}`);
        }
        break;

      case 'basic':
        if (nodeData.authUsername && nodeData.authPassword) {
          const username = this.replaceVariables(nodeData.authUsername, variables);
          const password = this.replaceVariables(nodeData.authPassword, variables);
          const credentials = btoa(`${username}:${password}`);
          headers = headers.set('Authorization', `Basic ${credentials}`);
        }
        break;

      case 'api_key':
        if (nodeData.authApiKey && nodeData.authApiKeyHeader) {
          const apiKey = this.replaceVariables(nodeData.authApiKey, variables);
          headers = headers.set(nodeData.authApiKeyHeader, apiKey);
        }
        break;
    }

    return headers;
  }

  /**
   * Construire les paramètres de requête
   */
  private buildParams(nodeData: NodeData, variables: { [key: string]: any }): HttpParams {
    let params = new HttpParams();

    if (nodeData.apiParameters) {
      nodeData.apiParameters
        .filter(param => param.enabled && param.type === 'query')
        .forEach(param => {
          const value = this.replaceVariables(param.value, variables);
          params = params.set(param.key, value);
        });
    }

    return params;
  }

  /**
   * Construire le corps de la requête
   */
  private buildRequestBody(nodeData: NodeData, variables: { [key: string]: any }): any {
    if (!nodeData.requestBody || nodeData.apiMethod === 'GET') {
      return null;
    }

    let body = this.replaceVariables(nodeData.requestBody, variables);

    // Ajouter les paramètres de type 'body'
    if (nodeData.apiParameters) {
      const bodyParams = nodeData.apiParameters.filter(param => param.enabled && param.type === 'body');

      if (bodyParams.length > 0 && nodeData.requestBodyType === 'json') {
        try {
          const parsedBody = JSON.parse(body);
          bodyParams.forEach(param => {
            const value = this.replaceVariables(param.value, variables);
            parsedBody[param.key] = this.parseValue(value);
          });
          body = JSON.stringify(parsedBody);
        } catch (error) {
          console.warn('Erreur parsing JSON body, utilisation du texte brut');
        }
      }
    }

    // Retourner selon le type
    switch (nodeData.requestBodyType) {
      case 'json':
        try {
          return JSON.parse(body);
        } catch {
          return body;
        }
      case 'form':
        const formData = new FormData();
        try {
          const parsed = JSON.parse(body);
          Object.keys(parsed).forEach(key => {
            formData.append(key, parsed[key]);
          });
          return formData;
        } catch {
          return body;
        }
      default:
        return body;
    }
  }

  /**
   * Extraire les headers de réponse
   */
  private extractHeaders(httpHeaders: any): { [key: string]: string } {
    const headers: { [key: string]: string } = {};

    if (httpHeaders && httpHeaders.keys) {
      httpHeaders.keys().forEach((key: string) => {
        headers[key] = httpHeaders.get(key);
      });
    }

    return headers;
  }

  /**
   * Remplacer les variables dans une chaîne
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
   * Parser une valeur en type approprié
   */
  private parseValue(value: string): any {
    // Essayer de parser en JSON pour les nombres, booléens, etc.
    try {
      return JSON.parse(value);
    } catch {
      return value;
    }
  }

  /**
   * Mapper la réponse API vers les variables
   */
  mapResponseToVariables(responseData: any, mappings: ApiResponseMapping[]): { [key: string]: any } {
    const variables: { [key: string]: any } = {};

    mappings
      .filter(mapping => mapping.enabled)
      .forEach(mapping => {
        try {
          const value = this.getValueByPath(responseData, mapping.jsonPath);
          variables[mapping.variableName] = value;
        } catch (error) {
          console.warn(`Erreur mapping ${mapping.jsonPath}:`, error);
          variables[mapping.variableName] = null;
        }
      });

    return variables;
  }

  /**
   * Récupérer une valeur par chemin JSON (ex: "data.user.name")
   */
  private getValueByPath(obj: any, path: string): any {
    return path.split('.').reduce((current, key) => {
      return current && current[key] !== undefined ? current[key] : null;
    }, obj);
  }

  /**
   * Valider une condition de succès
   */
  evaluateSuccessCondition(condition: string, result: ApiTestResult): boolean {
    try {
      // Remplacer les variables dans la condition
      const evalCondition = condition.replace(/status/g, result.status.toString()).replace(/responseTime/g, result.responseTime.toString());

      // Évaluation sécurisée
      return Function(`"use strict"; return (${evalCondition})`)();
    } catch (error) {
      console.warn('Erreur évaluation condition:', error);
      return result.success && result.status >= 200 && result.status < 300;
    }
  }
}
