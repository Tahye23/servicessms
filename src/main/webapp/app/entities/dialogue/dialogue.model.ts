export interface IDialogue {
  id: number;
  dialogueId?: number | null;
  contenu?: string | null;
}

export type NewDialogue = Omit<IDialogue, 'id'> & { id: null };
