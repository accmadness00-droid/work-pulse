import { axiosInstance } from "../../../shared/api/axiosInstance";

type ApiResponse<T> = {
  success?: boolean;
  data?: T;
  object?: T;
  message?: string | null;
};

function unwrap<T>(payload: ApiResponse<T> | T): T {
  if (payload && typeof payload === "object") {
    const response = payload as ApiResponse<T>;
    if (response.data !== undefined) {
      return response.data;
    }
    if (response.object !== undefined) {
      return response.object;
    }
  }
  return payload as T;
}

export type CompanyResponse = {
  id: string;
  name: string;
  legalName?: string | null;
  inn?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  logoUrl?: string | null;
  plan?: string | null;
  ownerId?: string | null;
  active: boolean;
};

export type CreateCompanyRequest = {
  name: string;
  legalName?: string;
  inn?: string;
  phone?: string;
  email?: string;
  address?: string;
  logoUrl?: string;
  plan?: string;
  ownerId?: string;
};

export type UpdateCompanyRequest = CreateCompanyRequest;

export type CompanySettingsResponse = {
  id: string;
  companyId: string;
  timezone: string;
  locale: string;
  plan: string;
  payrollOvertimeBonusEnabled: boolean;
  payrollLatePenaltyEnabled: boolean;
  payrollOvertimeMultiplier: number;
  payrollLatePenaltyMultiplier: number;
};

export type UpdateCompanySettingsRequest = {
  timezone: string;
  locale: string;
  plan: string;
  payrollOvertimeBonusEnabled?: boolean;
  payrollLatePenaltyEnabled?: boolean;
  payrollOvertimeMultiplier?: number;
  payrollLatePenaltyMultiplier?: number;
};

export const companyApi = {
  async listCompanies() {
    const response = await axiosInstance.get<ApiResponse<CompanyResponse[]> | CompanyResponse[]>("/api/v1/companies");
    return unwrap<CompanyResponse[]>(response.data);
  },

  async getCompany(id: string) {
    const response = await axiosInstance.get<ApiResponse<CompanyResponse> | CompanyResponse>(`/api/v1/companies/${id}`);
    return unwrap<CompanyResponse>(response.data);
  },

  async createCompany(request: CreateCompanyRequest) {
    const response = await axiosInstance.post<ApiResponse<CompanyResponse> | CompanyResponse>("/api/v1/companies", request);
    return unwrap<CompanyResponse>(response.data);
  },

  async updateCompany(id: string, request: UpdateCompanyRequest) {
    const response = await axiosInstance.put<ApiResponse<CompanyResponse> | CompanyResponse>(`/api/v1/companies/${id}`, request);
    return unwrap<CompanyResponse>(response.data);
  },

  async deleteCompany(id: string) {
    const response = await axiosInstance.delete<ApiResponse<void> | void>(`/api/v1/companies/${id}`);
    return unwrap<void>(response.data);
  },

  async getSettings(id: string) {
    const response = await axiosInstance.get<ApiResponse<CompanySettingsResponse> | CompanySettingsResponse>(
      `/api/v1/companies/${id}/settings`
    );
    return unwrap<CompanySettingsResponse>(response.data);
  },

  async updateSettings(id: string, request: UpdateCompanySettingsRequest) {
    const response = await axiosInstance.put<ApiResponse<CompanySettingsResponse> | CompanySettingsResponse>(
      `/api/v1/companies/${id}/settings`,
      request
    );
    return unwrap<CompanySettingsResponse>(response.data);
  }
};
