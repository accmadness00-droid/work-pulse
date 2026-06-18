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

export type DeviceType = "HIKVISION" | "ZKTECO" | "SUPREMA" | "QR" | "MOBILE";
export type DeviceConnectionType = "PUSH" | "POLLING" | "ALERT_STREAM";
export type DeviceStatus = "ACTIVE" | "INACTIVE";

export type DeviceResponse = {
  id: string;
  name: string;
  serialNumber: string;
  ipAddress?: string | null;
  port: number;
  username?: string | null;
  branchId: string;
  type: DeviceType;
  connectionType: DeviceConnectionType;
  status: DeviceStatus;
  lastSyncTime?: string | null;
  apiKeyConfigured: boolean;
};

export type CreateDeviceRequest = {
  name: string;
  serialNumber: string;
  ipAddress?: string;
  port?: number;
  username?: string;
  credentialsSecret?: string;
  branchId: string;
  type: DeviceType;
  connectionType: DeviceConnectionType;
  status?: DeviceStatus;
};

export type UpdateDeviceRequest = CreateDeviceRequest;

export type DeviceFilterRequest = {
  branchId?: string;
  type?: DeviceType;
  status?: DeviceStatus;
  connectionType?: DeviceConnectionType;
  page?: number;
  size?: number;
};

export type RotateDeviceApiKeyResponse = {
  deviceId: string;
  serialNumber: string;
  apiKey: string;
};

function params(filter: DeviceFilterRequest) {
  const searchParams = new URLSearchParams();
  Object.entries(filter).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      searchParams.set(key, String(value));
    }
  });
  return searchParams;
}

function toBackendRequest(request: CreateDeviceRequest | UpdateDeviceRequest) {
  return {
    name: request.name,
    serialNumber: request.serialNumber,
    ipAddress: request.ipAddress,
    port: request.port,
    username: request.username,
    credentialsSecret: request.credentialsSecret,
    branchId: request.branchId,
    type: request.type,
    connectionType: request.connectionType
  };
}

export const deviceApi = {
  async listDevices(filter: DeviceFilterRequest) {
    const response = await axiosInstance.get<ApiResponse<PageResponse<DeviceResponse> | DeviceResponse[]>>(
      "/api/v1/devices",
      { params: params(filter) }
    );
    return toPage<DeviceResponse>(unwrap<PageResponse<DeviceResponse> | DeviceResponse[]>(response.data));
  },

  async getDevice(id: string) {
    const response = await axiosInstance.get<ApiResponse<DeviceResponse> | DeviceResponse>(`/api/v1/devices/${id}`);
    return unwrap<DeviceResponse>(response.data);
  },

  async createDevice(request: CreateDeviceRequest) {
    const response = await axiosInstance.post<ApiResponse<DeviceResponse> | DeviceResponse>(
      "/api/v1/devices",
      toBackendRequest(request)
    );
    return unwrap<DeviceResponse>(response.data);
  },

  async updateDevice(id: string, request: UpdateDeviceRequest) {
    const response = await axiosInstance.put<ApiResponse<DeviceResponse> | DeviceResponse>(
      `/api/v1/devices/${id}`,
      toBackendRequest(request)
    );
    return unwrap<DeviceResponse>(response.data);
  },

  async activateDevice(id: string) {
    const response = await axiosInstance.patch<ApiResponse<DeviceResponse> | DeviceResponse>(`/api/v1/devices/${id}/activate`);
    return unwrap<DeviceResponse>(response.data);
  },

  async deactivateDevice(id: string) {
    const response = await axiosInstance.patch<ApiResponse<DeviceResponse> | DeviceResponse>(
      `/api/v1/devices/${id}/deactivate`
    );
    return unwrap<DeviceResponse>(response.data);
  },

  async rotateApiKey(id: string) {
    const response = await axiosInstance.post<ApiResponse<RotateDeviceApiKeyResponse> | RotateDeviceApiKeyResponse>(
      `/api/v1/devices/${id}/rotate-key`
    );
    return unwrap<RotateDeviceApiKeyResponse>(response.data);
  }
};
