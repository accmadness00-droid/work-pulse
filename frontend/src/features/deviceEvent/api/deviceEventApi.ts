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

export type DeviceEventDirection = "IN" | "OUT" | "UNKNOWN";
export type DeviceEventAuthType = "CARD" | "FACE" | "FINGERPRINT" | "QR";

export type DeviceEventResponse = {
  id: string;
  deviceId: string;
  externalEventId?: string | null;
  eventHash: string;
  employeeCode?: string | null;
  credentialValue?: string | null;
  eventTime: string;
  direction: DeviceEventDirection;
  authType?: DeviceEventAuthType | null;
  rawPayload?: string | null;
  processed: boolean;
  processingError?: string | null;
  retryCount: number;
  // TODO: link to employee/attendance detail when backend response exposes employeeId or attendanceId.
};

export type IngestDeviceEventRequest = {
  deviceSerialNumber?: string;
  externalEventId?: string;
  employeeCode?: string;
  credentialValue?: string;
  eventTime: string;
  direction: DeviceEventDirection;
  authType?: DeviceEventAuthType;
  rawPayload?: unknown;
};

export type DeviceEventFilterRequest = {
  deviceId?: string;
  processed?: boolean;
  from?: string;
  to?: string;
  direction?: DeviceEventDirection;
  authType?: DeviceEventAuthType;
  page?: number;
  size?: number;
};

export type ProcessDeviceEventResponse = {
  eventId: string;
  processed: boolean;
  processingError?: string | null;
  retryCount: number;
};

function params(filter: DeviceEventFilterRequest) {
  const searchParams = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  return searchParams;
}

export const deviceEventApi = {
  async ingest(request: IngestDeviceEventRequest) {
    const response = await axiosInstance.post<ApiResponse<DeviceEventResponse> | DeviceEventResponse>(
      "/api/v1/device-events",
      request
    );
    return unwrap<DeviceEventResponse>(response.data);
  },

  async list(filter: DeviceEventFilterRequest) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<DeviceEventResponse> | DeviceEventResponse[]>>(
      "/api/v1/device-events",
      { params: params(filter) }
    );
    return toPage<DeviceEventResponse>(unwrap<PageResponse<DeviceEventResponse> | DeviceEventResponse[]>(response.data));
  },

  async listUnprocessed(filter: Pick<DeviceEventFilterRequest, "page" | "size"> = {}) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<DeviceEventResponse> | DeviceEventResponse[]>>(
      "/api/v1/device-events/unprocessed",
      { params: params(filter) }
    );
    return toPage<DeviceEventResponse>(unwrap<PageResponse<DeviceEventResponse> | DeviceEventResponse[]>(response.data));
  },

  async getById(id: string) {
    const response = await axiosInstance.get<ApiResponse<DeviceEventResponse> | DeviceEventResponse>(
      `/api/v1/device-events/${id}`
    );
    return unwrap<DeviceEventResponse>(response.data);
  },

  async processOne(id: string) {
    const response = await axiosInstance.post<ApiResponse<ProcessDeviceEventResponse> | ProcessDeviceEventResponse>(
      `/api/v1/device-events/${id}/process`
    );
    return unwrap<ProcessDeviceEventResponse>(response.data);
  },

  async processBatch() {
    const response = await axiosInstance.post<ApiResponse<number> | number>("/api/v1/device-events/process-batch");
    return unwrap<number>(response.data);
  }
};

export function prettyRawPayload(value?: string | object | null) {
  if (!value) {
    return "-";
  }
  if (typeof value === "object") {
    return JSON.stringify(value, null, 2);
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
