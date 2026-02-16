import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { ITokensApp, NewTokensApp } from '../tokens-app.model';

export type PartialUpdateTokensApp = Partial<ITokensApp> & Pick<ITokensApp, 'id'>;

type RestOf<T extends ITokensApp | NewTokensApp> = Omit<T, 'dateExpiration' | 'createdAt' | 'lastUsedAt'> & {
  dateExpiration?: string | null;
  createdAt?: string | null;
  lastUsedAt?: string | null;
};

export type RestTokensApp = RestOf<ITokensApp>;
export type NewRestTokensApp = RestOf<NewTokensApp>;
export type PartialUpdateRestTokensApp = RestOf<PartialUpdateTokensApp>;
export type EntityResponseType = HttpResponse<ITokensApp>;
export type EntityArrayResponseType = HttpResponse<ITokensApp[]>;

export interface CreateTokenRequest {
  applicationId: number;
  dateExpiration?: dayjs.Dayjs;
  active?: boolean;
}

export interface TokenStats {
  total: number;
  active: number;
  expired: number;
  expiringSoon: number;
}

export interface TokenValidationResult {
  valid: boolean;
  expiresAt?: string;
  reason?: string;
}

@Injectable({ providedIn: 'root' })
export class TokensAppService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/tokens-apps');

  create(tokensApp: NewTokensApp): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(tokensApp);
    return this.http
      .post<RestTokensApp>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  // Nouvelle méthode pour créer avec expiration personnalisée
  createWithCustomExpiration(request: CreateTokenRequest): Observable<EntityResponseType> {
    const copy = this.convertCreateRequestFromClient(request);
    return this.http
      .post<RestTokensApp>(`${this.resourceUrl}/create-with-expiration`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(tokensApp: ITokensApp): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(tokensApp);
    return this.http
      .put<RestTokensApp>(`${this.resourceUrl}/${this.getTokensAppIdentifier(tokensApp)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(tokensApp: PartialUpdateTokensApp): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(tokensApp);
    return this.http
      .patch<RestTokensApp>(`${this.resourceUrl}/${this.getTokensAppIdentifier(tokensApp)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestTokensApp>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestTokensApp[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  // Méthodes spécifiques pour la gestion des tokens
  generateToken(id: number): Observable<EntityResponseType> {
    return this.http
      .post<RestTokensApp>(`${this.resourceUrl}/generateToken/${id}`, {}, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  regenerateToken(id: number): Observable<EntityResponseType> {
    return this.http
      .put<RestTokensApp>(`${this.resourceUrl}/${id}/regenerate`, {}, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  activateToken(id: number): Observable<EntityResponseType> {
    return this.http
      .put<RestTokensApp>(`${this.resourceUrl}/${id}/activate`, {}, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  deactivateToken(id: number): Observable<EntityResponseType> {
    return this.http
      .put<RestTokensApp>(`${this.resourceUrl}/${id}/deactivate`, {}, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  // Méthode pour obtenir les statistiques des tokens
  getTokensStats(): Observable<TokenStats> {
    return this.http.get<TokenStats>(`${this.resourceUrl}/stats`);
  }

  // Méthode pour vérifier la validité d'un token
  validateToken(token: string): Observable<TokenValidationResult> {
    return this.http.post<TokenValidationResult>(`${this.resourceUrl}/validate`, { token });
  }

  getTokensAppIdentifier(tokensApp: Pick<ITokensApp, 'id'>): number {
    return tokensApp.id;
  }

  compareTokensApp(o1: Pick<ITokensApp, 'id'> | null, o2: Pick<ITokensApp, 'id'> | null): boolean {
    return o1 && o2 ? this.getTokensAppIdentifier(o1) === this.getTokensAppIdentifier(o2) : o1 === o2;
  }

  addTokensAppToCollectionIfMissing<Type extends Pick<ITokensApp, 'id'>>(
    tokensAppCollection: Type[],
    ...tokensAppsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const tokensApps: Type[] = tokensAppsToCheck.filter(isPresent);
    if (tokensApps.length > 0) {
      const tokensAppCollectionIdentifiers = tokensAppCollection.map(tokensAppItem => this.getTokensAppIdentifier(tokensAppItem));
      const tokensAppsToAdd = tokensApps.filter(tokensAppItem => {
        const tokensAppIdentifier = this.getTokensAppIdentifier(tokensAppItem);
        if (tokensAppCollectionIdentifiers.includes(tokensAppIdentifier)) {
          return false;
        }
        tokensAppCollectionIdentifiers.push(tokensAppIdentifier);
        return true;
      });
      return [...tokensAppsToAdd, ...tokensAppCollection];
    }
    return tokensAppCollection;
  }

  protected convertDateFromClient<T extends ITokensApp | NewTokensApp | PartialUpdateTokensApp>(tokensApp: T): RestOf<T> {
    return {
      ...tokensApp,
      dateExpiration: tokensApp.dateExpiration?.toJSON() ?? null,
      createdAt: (tokensApp as any).createdAt?.toJSON() ?? null,
      lastUsedAt: (tokensApp as any).lastUsedAt?.toJSON() ?? null,
    };
  }

  protected convertCreateRequestFromClient(request: CreateTokenRequest): any {
    return {
      applicationId: request.applicationId,
      dateExpiration: request.dateExpiration?.toJSON() ?? null,
      active: request.active ?? true,
    };
  }

  protected convertDateFromServer(restTokensApp: RestTokensApp): ITokensApp {
    return {
      ...restTokensApp,
      dateExpiration: restTokensApp.dateExpiration ? dayjs(restTokensApp.dateExpiration) : undefined,
      createdAt: restTokensApp.createdAt ? dayjs(restTokensApp.createdAt) : undefined,
      lastUsedAt: restTokensApp.lastUsedAt ? dayjs(restTokensApp.lastUsedAt) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestTokensApp>): HttpResponse<ITokensApp> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestTokensApp[]>): HttpResponse<ITokensApp[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
