import { IPlanabonnement, NewPlanabonnement } from './planabonnement.model';

export const sampleWithRequiredData: IPlanabonnement = {
  id: 27021,
};

export const sampleWithPartialData: IPlanabonnement = {
  id: 19835,
  abpPrix: 14643.01,
};

export const sampleWithFullData: IPlanabonnement = {
  id: 15602,
  abpPrix: 20724.41,
  abpNbrAcces: 417,
};

export const sampleWithNewData: NewPlanabonnement = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
