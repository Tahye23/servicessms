import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IPlanabonnement, NewPlanabonnement } from '../planabonnement.model';

export type PartialUpdatePlanabonnement = Partial<IPlanabonnement> & Pick<IPlanabonnement, 'id'>;

export type EntityResponseType = HttpResponse<IPlanabonnement>;
export type EntityArrayResponseType = HttpResponse<IPlanabonnement[]>;

@Injectable({ providedIn: 'root' })
export class PlanabonnementService {
  protected http = inject(HttpClient);
  protected applicationConfigService = inject(ApplicationConfigService);

  // URL corrigée selon votre controller backend
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/planabonnements');

  /**
   * Crée un nouveau plan d'abonnement (ADMIN uniquement)
   */
  create(planabonnement: NewPlanabonnement): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(planabonnement);
    return this.http
      .post<IPlanabonnement>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Met à jour un plan d'abonnement existant (ADMIN uniquement)
   */
  update(planabonnement: IPlanabonnement): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(planabonnement);
    return this.http
      .put<IPlanabonnement>(`${this.resourceUrl}/${this.getPlanabonnementIdentifier(planabonnement)}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Mise à jour partielle d'un plan d'abonnement (ADMIN uniquement)
   */
  partialUpdate(planabonnement: PartialUpdatePlanabonnement): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(planabonnement);
    return this.http
      .patch<IPlanabonnement>(`${this.resourceUrl}/${this.getPlanabonnementIdentifier(planabonnement)}`, copy, {
        observe: 'response',
      })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Récupère un plan d'abonnement par son ID
   */
  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IPlanabonnement>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Récupère tous les plans actifs (PUBLIC - pour affichage client)
   */
  getAllActivePlans(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IPlanabonnement[]>(`${this.resourceUrl}`, { observe: 'response' })
      .pipe(map(res => this.convertDateArrayFromServer(res)));
  }

  /**
   * Récupère tous les plans pour l'administration (ADMIN uniquement)
   */
  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IPlanabonnement[]>(`${this.resourceUrl}/admin`, { params: options, observe: 'response' })
      .pipe(map(res => this.convertDateArrayFromServer(res)));
  }

  /**
   * Récupère les plans par type
   */
  getByType(type: string): Observable<EntityArrayResponseType> {
    return this.http
      .get<IPlanabonnement[]>(`${this.resourceUrl}/type/${type}`, { observe: 'response' })
      .pipe(map(res => this.convertDateArrayFromServer(res)));
  }

  /**
   * Récupère les plans populaires
   */
  getPopularPlans(): Observable<EntityArrayResponseType> {
    return this.http
      .get<IPlanabonnement[]>(`${this.resourceUrl}/popular`, { observe: 'response' })
      .pipe(map(res => this.convertDateArrayFromServer(res)));
  }

  /**
   * Bascule le statut actif/inactif d'un plan (ADMIN uniquement)
   */
  toggleActive(id: number): Observable<EntityResponseType> {
    return this.http
      .put<IPlanabonnement>(`${this.resourceUrl}/${id}/toggle-active`, {}, { observe: 'response' })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Supprime (désactive) un plan d'abonnement (ADMIN uniquement)
   * Note: Selon votre controller, cela désactive le plan plutôt que de le supprimer
   */
  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /**
   * Méthode pour récupérer tous les plans (compatibilité avec votre ancien code)
   * @deprecated Utilisez getAllActivePlans() ou query() selon le besoin
   */
  getAllPlans(): Observable<IPlanabonnement[]> {
    return this.http.get<IPlanabonnement[]>(`${this.resourceUrl}`);
  }

  getPlanabonnementIdentifier(planabonnement: Pick<IPlanabonnement, 'id'>): number {
    return planabonnement.id;
  }

  comparePlanabonnement(o1: Pick<IPlanabonnement, 'id'> | null, o2: Pick<IPlanabonnement, 'id'> | null): boolean {
    return o1 && o2 ? this.getPlanabonnementIdentifier(o1) === this.getPlanabonnementIdentifier(o2) : o1 === o2;
  }

  addPlanabonnementToCollectionIfMissing<Type extends Pick<IPlanabonnement, 'id'>>(
    planabonnementCollection: Type[],
    ...planabonnementsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const planabonnements: Type[] = planabonnementsToCheck.filter(isPresent);
    if (planabonnements.length > 0) {
      const planabonnementCollectionIdentifiers = planabonnementCollection.map(planabonnementItem =>
        this.getPlanabonnementIdentifier(planabonnementItem),
      );
      const planabonnementsToAdd = planabonnements.filter(planabonnementItem => {
        const planabonnementIdentifier = this.getPlanabonnementIdentifier(planabonnementItem);
        if (planabonnementCollectionIdentifiers.includes(planabonnementIdentifier)) {
          return false;
        }
        planabonnementCollectionIdentifiers.push(planabonnementIdentifier);
        return true;
      });
      return [...planabonnementCollection, ...planabonnementsToAdd];
    }
    return planabonnementCollection;
  }

  // ===== MÉTHODES DE CONVERSION DES DATES =====

  protected convertDateFromClient<T extends IPlanabonnement | NewPlanabonnement | PartialUpdatePlanabonnement>(
    planabonnement: T,
  ): RestOf<T> {
    return {
      ...planabonnement,
      createdDate: planabonnement.createdDate?.toJSON() ?? null,
      updatedDate: planabonnement.updatedDate?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.createdDate = res.body.createdDate ? dayjs(res.body.createdDate) : null;
      res.body.updatedDate = res.body.updatedDate ? dayjs(res.body.updatedDate) : null;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((planabonnement: IPlanabonnement) => {
        planabonnement.createdDate = planabonnement.createdDate ? dayjs(planabonnement.createdDate) : null;
        planabonnement.updatedDate = planabonnement.updatedDate ? dayjs(planabonnement.updatedDate) : null;
      });
    }
    return res;
  }

  // ===== MÉTHODES UTILITAIRES =====

  /**
   * Valide si un plan peut être supprimé
   */
  canDelete(planId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.resourceUrl}/${planId}/can-delete`);
  }

  /**
   * Duplique un plan existant
   */
  duplicate(planId: number): Observable<EntityResponseType> {
    return this.http
      .post<IPlanabonnement>(`${this.resourceUrl}/${planId}/duplicate`, {}, { observe: 'response' })
      .pipe(map(res => this.convertDateFromServer(res)));
  }

  /**
   * Export des plans vers CSV
   */
  exportToCsv(): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/export/csv`, {
      responseType: 'blob',
      headers: { Accept: 'text/csv' },
    });
  }

  /**
   * Import des plans depuis CSV
   */
  importFromCsv(file: File): Observable<EntityArrayResponseType> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http
      .post<IPlanabonnement[]>(`${this.resourceUrl}/import/csv`, formData, { observe: 'response' })
      .pipe(map(res => this.convertDateArrayFromServer(res)));
  }
}

// Type utilitaire pour la conversion des dates
type RestOf<T extends IPlanabonnement | NewPlanabonnement> = Omit<T, 'createdDate' | 'updatedDate'> & {
  createdDate?: string | null;
  updatedDate?: string | null;
};
