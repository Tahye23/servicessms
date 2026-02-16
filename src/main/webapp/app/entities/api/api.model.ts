export interface IApi {
  id: number;
  apiNom?: string | null;
  apiUrl?: string | null;
  apiVersion?: number | null;
}

export type NewApi = Omit<IApi, 'id'> & { id: null };
