import { IReferentiel, NewReferentiel } from './referentiel.model';

export const sampleWithRequiredData: IReferentiel = {
  id: 25596,
  refCode: 'complètement',
  refRadical: 'dès que',
};

export const sampleWithPartialData: IReferentiel = {
  id: 1165,
  refCode: 'paf quand ?',
  refRadical: 'charitable',
  refArTitle: 'certes tic-tac juriste',
  refEnTitle: 'proche membre du personnel efficace',
};

export const sampleWithFullData: IReferentiel = {
  id: 4285,
  refCode: 'clac antique',
  refRadical: 'ouin bof',
  refFrTitle: 'exploser trop',
  refArTitle: 'orange',
  refEnTitle: 'toutefois cyan vorace',
};

export const sampleWithNewData: NewReferentiel = {
  refCode: 'complètement où',
  refRadical: 'bè dessus complètement',
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
