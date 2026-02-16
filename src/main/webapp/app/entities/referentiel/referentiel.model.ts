export interface IReferentiel {
  id: number;
  refCode?: string | null;
  refRadical?: string | null;
  refFrTitle?: string | null;
  refArTitle?: string | null;
  refEnTitle?: string | null;
}

export type NewReferentiel = Omit<IReferentiel, 'id'> & { id: null };
