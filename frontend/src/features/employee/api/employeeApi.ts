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

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type EmployeeResponse = {
  id: string;
  userId?: string | null;
  email?: string | null;
  companyId: string;
  branchId: string;
  firstName: string;
  lastName: string;
  middleName?: string | null;
  phone?: string | null;
  photoUrl?: string | null;
  position?: string | null;
  employeeCode: string;
  hiredDate?: string | null;
  hireDate?: string | null;
  birthDate?: string | null;
  employmentType?: EmploymentType | null;
  salary?: number | null;
  active: boolean;
};

export type HikvisionPhotoSyncResult = {
  deviceId: string;
  deviceName: string;
  success: boolean;
  message?: string | null;
};

export type HikvisionPhotoSyncResponse = {
  employeeId: string;
  totalDevices: number;
  successCount: number;
  failureCount: number;
  results: HikvisionPhotoSyncResult[];
};

export type EmploymentType = "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN";

export type CreateEmployeeRequest = {
  userId?: string;
  email?: string;
  password?: string;
  companyId: string;
  branchId: string;
  firstName: string;
  lastName: string;
  middleName?: string;
  phone?: string;
  photoUrl?: string;
  position?: string;
  employeeCode: string;
  hiredDate?: string;
  hireDate?: string;
  birthDate?: string;
  employmentType?: EmploymentType;
  salary?: number;
  active?: boolean;
};

export type UpdateEmployeeRequest = Omit<CreateEmployeeRequest, "email" | "password" | "active">;

export type EmployeeFilterRequest = {
  companyId?: string;
  branchId?: string;
  position?: string;
  active?: boolean;
  search?: string;
  page?: number;
  size?: number;
};

export type GeneratedEmployeeCodeResponse = {
  employeeCode: string;
};

function params(filter: EmployeeFilterRequest) {
  const searchParams = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  return searchParams;
}

function toPage<T>(value: PageResponse<T> | T[]): PageResponse<T> {
  if (Array.isArray(value)) {
    return {
      content: value,
      totalElements: value.length,
      totalPages: 1,
      number: 0,
      size: value.length
    };
  }
  return value;
}

export const employeeApi = {
  async listEmployees(filter: EmployeeFilterRequest) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<EmployeeResponse> | EmployeeResponse[]>>(
      "/api/v1/employees",
      { params: params(filter) }
    );
    return toPage<EmployeeResponse>(unwrap<PageResponse<EmployeeResponse> | EmployeeResponse[]>(response.data));
  },

  async getEmployee(id: string) {
    const response = await axiosInstance.get<ApiResponse<EmployeeResponse> | EmployeeResponse>(`/api/v1/employees/${id}`);
    return unwrap<EmployeeResponse>(response.data);
  },

  async generateEmployeeCode() {
    const response = await axiosInstance.get<ApiResponse<GeneratedEmployeeCodeResponse> | GeneratedEmployeeCodeResponse>(
      "/api/v1/employees/next-code"
    );
    return unwrap<GeneratedEmployeeCodeResponse>(response.data);
  },

  async createEmployee(request: CreateEmployeeRequest) {
    const response = await axiosInstance.post<ApiResponse<EmployeeResponse> | EmployeeResponse>(
      "/api/v1/employees",
      request
    );
    return unwrap<EmployeeResponse>(response.data);
  },

  async updateEmployee(id: string, request: UpdateEmployeeRequest) {
    const response = await axiosInstance.put<ApiResponse<EmployeeResponse> | EmployeeResponse>(
      `/api/v1/employees/${id}`,
      request
    );
    return unwrap<EmployeeResponse>(response.data);
  },

  async deleteEmployee(id: string) {
    const response = await axiosInstance.delete<ApiResponse<void> | void>(`/api/v1/employees/${id}`);
    return unwrap<void>(response.data);
  },

  async activateEmployee(id: string) {
    const response = await axiosInstance.patch<ApiResponse<EmployeeResponse> | EmployeeResponse>(
      `/api/v1/employees/${id}/activate`
    );
    return unwrap<EmployeeResponse>(response.data);
  },

  async deactivateEmployee(id: string) {
    const response = await axiosInstance.patch<ApiResponse<EmployeeResponse> | EmployeeResponse>(
      `/api/v1/employees/${id}/deactivate`
    );
    return unwrap<EmployeeResponse>(response.data);
  },

  async uploadPhoto(id: string, file: File) {
    const formData = new FormData();
    formData.append("file", file);
    const response = await axiosInstance.post<ApiResponse<EmployeeResponse> | EmployeeResponse>(
      `/api/v1/employees/${id}/photo`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      }
    );
    return unwrap<EmployeeResponse>(response.data);
  },

  async syncPhotoToHikvision(id: string) {
    const response = await axiosInstance.post<ApiResponse<HikvisionPhotoSyncResponse> | HikvisionPhotoSyncResponse>(
      `/api/v1/employees/${id}/photo/sync-hikvision`
    );
    return unwrap<HikvisionPhotoSyncResponse>(response.data);
  }
};

export function employeePhotoUrl(photoUrl?: string | null) {
  if (!photoUrl) {
    return undefined;
  }
  if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://") || photoUrl.startsWith("data:")) {
    return photoUrl;
  }
  return new URL(photoUrl, axiosInstance.defaults.baseURL ?? window.location.origin).toString();
}
