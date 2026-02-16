export interface ITemplate {
  id: number;
  name: string;
  content: string;
  expediteur?: string;
  approved: boolean;
  user_id?: string;
  created_at?: string;
  approved_at?: string;
  characterCount?: number;
  templateId?: string;
  templateName?: string;
  code?: string;
  status?: string;
}

export class Template implements ITemplate {
  constructor(
    public id: number,
    public name: string = '',
    public content: string = '',
    public expediteur?: string,
    public approved: boolean = false,
    public user_id: string = '',
    public created_at?: string,
    public approved_at?: string,
    public characterCount?: number,
  ) {}
}
