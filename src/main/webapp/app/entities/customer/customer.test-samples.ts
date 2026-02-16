import { ICustomer, NewCustomer } from './customer.model';

export const sampleWithRequiredData: ICustomer = {
  id: 23310,
};

export const sampleWithPartialData: ICustomer = {
  id: 27528,
  lastName: 'Dupuy',
  country: 'Cuba',
  telephone: '0215494050',
};

export const sampleWithFullData: ICustomer = {
  id: 30476,
  customerId: 31620,
  firstName: 'Boniface',
  lastName: 'Pierre',
  country: 'Liechtenstein',
  telephone: '0739790143',
};

export const sampleWithNewData: NewCustomer = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
