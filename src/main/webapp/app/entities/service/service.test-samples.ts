import { IService, NewService } from './service.model';

export const sampleWithRequiredData: IService = {
  id: 20241,
};

export const sampleWithPartialData: IService = {
  id: 31725,
  servNom: 'craquer prononcer',
  servDescription: 'commissionnaire loin de gigantesque',
  montant: 9725.27,
};

export const sampleWithFullData: IService = {
  id: 5822,
  servNom: 'tailler',
  servDescription: 'grrr',
  nbrusage: 18853,
  montant: 18397.84,
  statut: false,
};

export const sampleWithNewData: NewService = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
