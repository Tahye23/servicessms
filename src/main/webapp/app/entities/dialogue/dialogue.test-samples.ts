import { IDialogue, NewDialogue } from './dialogue.model';

export const sampleWithRequiredData: IDialogue = {
  id: 15631,
};

export const sampleWithPartialData: IDialogue = {
  id: 17537,
  dialogueId: 13879,
};

export const sampleWithFullData: IDialogue = {
  id: 18191,
  dialogueId: 9021,
  contenu: 'meuh vide',
};

export const sampleWithNewData: NewDialogue = {
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
