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

export type BranchResponse = {
  id: string;
  companyId: string;
  name: string;
  address?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  geofenceRadiusMeters?: number | null;
  radiusMeters?: number | null;
  phone?: string | null;
  active: boolean;
};

export type CreateBranchRequest = {
  companyId: string;
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  radiusMeters?: number;
  geofenceRadiusMeters?: number;
  phone?: string;
};

export type UpdateBranchRequest = CreateBranchRequest;

export type BranchScheduleResponse = {
  id: string;
  branchId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  lateThresholdMin: number;
  lateThresholdMinutes?: number;
  isWorkday: boolean;
};

export type UpdateBranchScheduleRequest = {
  schedules: Array<{
    dayOfWeek: number;
    startTime: string;
    endTime: string;
    lateThresholdMin: number;
    lateThresholdMinutes?: number;
    isWorkday: boolean;
  }>;
};

function toBackendRequest(request: CreateBranchRequest | UpdateBranchRequest) {
  return {
    name: request.name,
    address: request.address,
    latitude: request.latitude,
    longitude: request.longitude,
    geofenceRadiusMeters: request.geofenceRadiusMeters ?? request.radiusMeters,
    phone: request.phone
  };
}

export const branchApi = {
  async listBranches(companyId: string) {
    const response = await axiosInstance.get<ApiResponse<BranchResponse[]> | BranchResponse[]>(
      `/api/v1/companies/${companyId}/branches`
    );
    return unwrap<BranchResponse[]>(response.data);
  },

  async getBranch(id: string) {
    const response = await axiosInstance.get<ApiResponse<BranchResponse> | BranchResponse>(`/api/v1/branches/${id}`);
    return unwrap<BranchResponse>(response.data);
  },

  async createBranch(request: CreateBranchRequest) {
    const response = await axiosInstance.post<ApiResponse<BranchResponse> | BranchResponse>(
      `/api/v1/companies/${request.companyId}/branches`,
      toBackendRequest(request)
    );
    return unwrap<BranchResponse>(response.data);
  },

  async updateBranch(id: string, request: UpdateBranchRequest) {
    const response = await axiosInstance.put<ApiResponse<BranchResponse> | BranchResponse>(
      `/api/v1/branches/${id}`,
      toBackendRequest(request)
    );
    return unwrap<BranchResponse>(response.data);
  },

  async deleteBranch(id: string) {
    const response = await axiosInstance.delete<ApiResponse<void> | void>(`/api/v1/branches/${id}`);
    return unwrap<void>(response.data);
  },

  async getSchedule(id: string) {
    const response = await axiosInstance.get<ApiResponse<BranchScheduleResponse[]> | BranchScheduleResponse[]>(
      `/api/v1/branches/${id}/schedule`
    );
    return unwrap<BranchScheduleResponse[]>(response.data);
  },

  async updateSchedule(id: string, request: UpdateBranchScheduleRequest) {
    const response = await axiosInstance.put<ApiResponse<BranchScheduleResponse[]> | BranchScheduleResponse[]>(
      `/api/v1/branches/${id}/schedule`,
      {
        schedules: request.schedules.map((schedule) => ({
          dayOfWeek: schedule.dayOfWeek,
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          lateThresholdMin: schedule.lateThresholdMin ?? schedule.lateThresholdMinutes ?? 15,
          isWorkday: schedule.isWorkday
        }))
      }
    );
    return unwrap<BranchScheduleResponse[]>(response.data);
  }
};
