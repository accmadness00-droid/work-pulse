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

export type CredentialType = "CARD" | "FACE" | "FINGERPRINT" | "QR";

export type EmployeeCredentialResponse = {
  id: string;
  employeeId: string;
  credentialType: CredentialType;
  externalId: string;
  active: boolean;
};

export type CreateEmployeeCredentialRequest = {
  employeeId: string;
  credentialType: CredentialType;
  externalId: string;
  active?: boolean;
};

export type EmployeeCredentialFilterRequest = {
  employeeId?: string;
  credentialType?: CredentialType;
  active?: boolean;
};

function params(filter: EmployeeCredentialFilterRequest) {
  const searchParams = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  return searchParams;
}

export const employeeCredentialApi = {
  async listCredentials(filter: EmployeeCredentialFilterRequest = {}) {
    const response = await axiosInstance.get<ApiResponse<EmployeeCredentialResponse[]> | EmployeeCredentialResponse[]>(
      "/api/v1/employee-credentials",
      { params: params(filter) }
    );
    return unwrap<EmployeeCredentialResponse[]>(response.data);
  },

  async getCredential(id: string) {
    const response = await axiosInstance.get<ApiResponse<EmployeeCredentialResponse> | EmployeeCredentialResponse>(
      `/api/v1/employee-credentials/${id}`
    );
    return unwrap<EmployeeCredentialResponse>(response.data);
  },

  async createCredential(request: CreateEmployeeCredentialRequest) {
    const response = await axiosInstance.post<ApiResponse<EmployeeCredentialResponse> | EmployeeCredentialResponse>(
      "/api/v1/employee-credentials",
      {
        employeeId: request.employeeId,
        credentialType: request.credentialType,
        externalId: request.externalId
      }
    );
    return unwrap<EmployeeCredentialResponse>(response.data);
  },

  async deleteCredential(id: string) {
    const response = await axiosInstance.delete<ApiResponse<void> | void>(`/api/v1/employee-credentials/${id}`);
    return unwrap<void>(response.data);
  },

  async activateCredential(id: string) {
    const response = await axiosInstance.patch<ApiResponse<EmployeeCredentialResponse> | EmployeeCredentialResponse>(
      `/api/v1/employee-credentials/${id}/activate`
    );
    return unwrap<EmployeeCredentialResponse>(response.data);
  },

  async deactivateCredential(id: string) {
    const response = await axiosInstance.patch<ApiResponse<EmployeeCredentialResponse> | EmployeeCredentialResponse>(
      `/api/v1/employee-credentials/${id}/deactivate`
    );
    return unwrap<EmployeeCredentialResponse>(response.data);
  }
};
