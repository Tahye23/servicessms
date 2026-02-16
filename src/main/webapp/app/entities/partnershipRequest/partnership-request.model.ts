import dayjs from 'dayjs/esm';

export enum RequestStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  IN_REVIEW = 'IN_REVIEW',
}

export interface IPartnershipRequest {
  id: number;
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
  phone?: string | null;
  companyName?: string | null;
  industry?: string | null;
  projectDescription?: string | null;
  monthlyVolume?: string | null;
  launchDate?: string | null;
  selectedPlanId?: number | null;
  selectedPlanName?: string | null;
  status?: RequestStatus | null;
  createdDate?: any | null;
  processedDate?: any | null;
  adminNotes?: string | null;
}

export type NewPartnershipRequest = Omit<IPartnershipRequest, 'id'> & { id: null };

export interface PartnershipRequestFormData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  companyName: string;
  industry: string;
  projectDescription: string;
  monthlyVolume?: string;
  launchDate?: string;
  selectedPlanId?: number;
  selectedPlanName?: string;
}

export interface PartnershipRequestStats {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  todayRequests: number;
}
