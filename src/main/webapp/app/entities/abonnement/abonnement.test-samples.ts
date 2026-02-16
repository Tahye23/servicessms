import { IAbonnement, NewAbonnement } from './abonnement.model';

export const sampleWithRequiredData: IAbonnement = {
  id: 29220,
};

export const sampleWithPartialData: IAbonnement = {
  id: 32602,
  aboStatut: 'plier bang à même',
};

export const sampleWithFullData: IAbonnement = {
  id: 26133,
  aboStatut: 'autrement débile oui',
  aboCompteur: 14474,
};

export const sampleWithNewData: NewAbonnement = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
