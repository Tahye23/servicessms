export interface ICustomer {
  id: number;
  customerId?: number | null;
  firstName?: string | null;
  lastName?: string | null;
  country?: string | null;
  telephone?: string | null;
}

export type NewCustomer = Omit<ICustomer, 'id'> & { id: null };
