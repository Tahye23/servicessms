export interface IFileextrait {
  id: number;
  fexidfile?: string | null;
  fexparent?: string | null;
  fexdata?: string | null;
  fexdataContentType?: string | null;
  fextype?: string | null;
  fexname?: string | null;
}

export type NewFileextrait = Omit<IFileextrait, 'id'> & { id: null };
