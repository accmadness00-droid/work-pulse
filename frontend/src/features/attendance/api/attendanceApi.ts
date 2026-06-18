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

export type AttendanceStatus = "PRESENT" | "LATE" | "ABSENT" | "LEAVE" | "HOLIDAY";
export type AttendanceMethod = "GPS" | "FACE_ID" | "MANUAL" | "PIN" | "DEVICE" | "WEB_CAMERA";
export type AttendanceSessionType = "REGULAR" | "OVERTIME" | "BREAK";

export type AttendanceResponse = {
  id: string;
  employeeId: string;
  branchId: string;
  date: string;
  checkInTime?: string | null;
  checkOutTime?: string | null;
  checkInLat?: number | null;
  checkInLng?: number | null;
  checkOutLat?: number | null;
  checkOutLng?: number | null;
  status: AttendanceStatus;
  lateMinutes: number;
  workMinutes: number;
  method: AttendanceMethod;
  sourceDeviceId?: string | null;
  sessionType: AttendanceSessionType;
  note?: string | null;
};

export type CheckInRequest = {
  employeeId: string;
  branchId: string;
  latitude?: number;
  longitude?: number;
  method?: AttendanceMethod;
  note?: string;
};

export type CheckOutRequest = {
  employeeId: string;
  latitude?: number;
  longitude?: number;
  method?: AttendanceMethod;
  note?: string;
};

export type AttendanceFilterRequest = {
  employeeId?: string;
  branchId?: string;
  date?: string;
  from?: string;
  to?: string;
  status?: AttendanceStatus;
  method?: AttendanceMethod;
  page?: number;
  size?: number;
};

export type UpdateAttendanceRequest = {
  checkInTime?: string;
  checkOutTime?: string;
  status?: AttendanceStatus;
  note?: string;
};

export type TodayAttendanceFilterRequest = {
  companyId?: string;
  branchId?: string;
  page?: number;
  size?: number;
};

export type CameraAttendanceRequest = {
  branchId: string;
  latitude: number;
  longitude: number;
  accuracyMeters?: number | null;
  photoBase64: string;
};

export type CameraAttendanceResponse = {
  attendanceId: string;
  employeeId: string;
  branchId: string;
  action: "CHECK_IN" | "CHECK_OUT";
  status?: AttendanceStatus | null;
  method: "WEB_CAMERA";
  checkInTime?: string | null;
  checkOutTime?: string | null;
  workMinutes?: number | null;
  faceVerified: boolean;
  faceDistance: number;
  locationVerified: boolean;
  photoUrl?: string | null;
};

function params(filter: Record<string, unknown>) {
  const searchParams = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  return searchParams;
}

export const attendanceApi = {
  async checkIn(request: CheckInRequest) {
    const response = await axiosInstance.post<ApiResponse<AttendanceResponse> | AttendanceResponse>(
      "/api/v1/attendance/check-in",
      request
    );
    return unwrap<AttendanceResponse>(response.data);
  },

  async checkOut(request: CheckOutRequest) {
    const response = await axiosInstance.post<ApiResponse<AttendanceResponse> | AttendanceResponse>(
      "/api/v1/attendance/check-out",
      request
    );
    return unwrap<AttendanceResponse>(response.data);
  },

  async listAttendance(filter: AttendanceFilterRequest) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<AttendanceResponse> | AttendanceResponse[]>>(
      "/api/v1/attendance",
      { params: params(filter) }
    );
    return toPage<AttendanceResponse>(unwrap<PageResponse<AttendanceResponse> | AttendanceResponse[]>(response.data));
  },

  async getToday(filter: TodayAttendanceFilterRequest) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<AttendanceResponse> | AttendanceResponse[]>>(
      "/api/v1/attendance/today",
      { params: params(filter) }
    );
    return toPage<AttendanceResponse>(unwrap<PageResponse<AttendanceResponse> | AttendanceResponse[]>(response.data));
  },

  async getById(id: string) {
    const response = await axiosInstance.get<ApiResponse<AttendanceResponse> | AttendanceResponse>(
      `/api/v1/attendance/${id}`
    );
    return unwrap<AttendanceResponse>(response.data);
  },

  async getEmployeeHistory(employeeId: string, filter: Pick<AttendanceFilterRequest, "from" | "to"> = {}) {
    const response = await axiosInstance.get<ApiResponse<AttendanceResponse[]> | AttendanceResponse[]>(
      `/api/v1/attendance/employee/${employeeId}`,
      { params: params(filter) }
    );
    return unwrap<AttendanceResponse[]>(response.data);
  },

  async updateAttendance(id: string, request: UpdateAttendanceRequest) {
    const response = await axiosInstance.patch<ApiResponse<AttendanceResponse> | AttendanceResponse>(
      `/api/v1/attendance/${id}`,
      request
    );
    return unwrap<AttendanceResponse>(response.data);
  },

  async cameraCheckIn(request: CameraAttendanceRequest) {
    const response = await axiosInstance.post<ApiResponse<CameraAttendanceResponse> | CameraAttendanceResponse>(
      "/api/v1/attendance/camera/check-in",
      request
    );
    return unwrap<CameraAttendanceResponse>(response.data);
  },

  async cameraCheckOut(request: CameraAttendanceRequest) {
    const response = await axiosInstance.post<ApiResponse<CameraAttendanceResponse> | CameraAttendanceResponse>(
      "/api/v1/attendance/camera/check-out",
      request
    );
    return unwrap<CameraAttendanceResponse>(response.data);
  }
};

export function formatMinutes(totalMinutes?: number | null) {
  const minutes = Math.max(0, totalMinutes ?? 0);
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return `${hours}h ${rest}m`;
}
