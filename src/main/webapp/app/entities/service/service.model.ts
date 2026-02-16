import { IReferentiel } from 'app/entities/referentiel/referentiel.model';

export interface IService {
  id: number;
  servNom?: string | null;
  servDescription?: string | null;
  nbrusage?: number | null;
  montant?: number | null;
  statut?: boolean | null;
  accessType?: IReferentiel | null;
}

export type NewService = Omit<IService, 'id'> & { id: null };
