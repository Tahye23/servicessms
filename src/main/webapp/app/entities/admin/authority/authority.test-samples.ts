import { IAuthority, NewAuthority } from './authority.model';

export const sampleWithRequiredData: IAuthority = {
  name: '5c52cb4b-6581-4173-948f-da01a51980fa',
};

export const sampleWithPartialData: IAuthority = {
  name: '94974165-4e7b-4bab-bb0d-a1c335c16186',
};

export const sampleWithFullData: IAuthority = {
  name: '95f5feaa-ca0c-4eb9-aa3a-f0fae7886a77',
};

export const sampleWithNewData: NewAuthority = {
  name: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
