import { IFileextrait, NewFileextrait } from './fileextrait.model';

export const sampleWithRequiredData: IFileextrait = {
  id: 5083,
};

export const sampleWithPartialData: IFileextrait = {
  id: 26239,
  fexidfile: '9ec7c9f5-7d67-4fb7-ba7a-895dff933afe',
  fexdata: '../fake-data/blob/hipster.png',
  fexdataContentType: 'unknown',
  fextype: 'd’autant que du fait que commis de cuisine',
  fexname: 'adversaire',
};

export const sampleWithFullData: IFileextrait = {
  id: 24108,
  fexidfile: '949a30fa-51c4-459e-806a-7d5f461600f8',
  fexparent: 'guère alors que feindre',
  fexdata: '../fake-data/blob/hipster.png',
  fexdataContentType: 'unknown',
  fextype: 'commissionnaire servir badaboum',
  fexname: 'du moment que jamais presque',
};

export const sampleWithNewData: NewFileextrait = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
