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

export type EmployeeScheduleResponse = {
  id: string;
  employeeId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  lateThresholdMin: number;
  isWorkday: boolean;
  note?: string | null;
};

export type UpdateEmployeeScheduleRequest = {
  schedules: Array<{
    dayOfWeek: number;
    startTime: string;
    endTime: string;
    lateThresholdMin: number;
    isWorkday: boolean;
    note?: string;
  }>;
};

export const employeeScheduleApi = {
  async getSchedule(employeeId: string) {
    const response = await axiosInstance.get<ApiResponse<EmployeeScheduleResponse[]> | EmployeeScheduleResponse[]>(
      `/api/v1/employees/${employeeId}/schedule`
    );
    return unwrap<EmployeeScheduleResponse[]>(response.data);
  },

  async updateSchedule(employeeId: string, request: UpdateEmployeeScheduleRequest) {
    const response = await axiosInstance.put<ApiResponse<EmployeeScheduleResponse[]> | EmployeeScheduleResponse[]>(
      `/api/v1/employees/${employeeId}/schedule`,
      request
    );
    return unwrap<EmployeeScheduleResponse[]>(response.data);
  }
};
