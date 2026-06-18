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

export type EmployeeFaceProfileResponse = {
  id: string;
  employeeId: string;
  photoUrl?: string | null;
  modelName: string;
  active: boolean;
  createdAt?: string | null;
};

export type EnrollFaceResponse = {
  employeeId: string;
  faceProfileId: string;
  photoUrl?: string | null;
  modelName: string;
  active: boolean;
};

export const faceApi = {
  async enroll(employeeId: string, file: File) {
    const formData = new FormData();
    formData.append("file", file);
    const response = await axiosInstance.post<ApiResponse<EnrollFaceResponse> | EnrollFaceResponse>(
      `/api/v1/employees/${employeeId}/face/enroll`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      }
    );
    return unwrap<EnrollFaceResponse>(response.data);
  },

  async listProfiles(employeeId: string) {
    const response = await axiosInstance.get<ApiResponse<EmployeeFaceProfileResponse[]> | EmployeeFaceProfileResponse[]>(
      `/api/v1/employees/${employeeId}/face-profiles`
    );
    return unwrap<EmployeeFaceProfileResponse[]>(response.data);
  },

  async deactivate(employeeId: string, profileId: string) {
    const response = await axiosInstance.patch<ApiResponse<EmployeeFaceProfileResponse> | EmployeeFaceProfileResponse>(
      `/api/v1/employees/${employeeId}/face-profiles/${profileId}/deactivate`
    );
    return unwrap<EmployeeFaceProfileResponse>(response.data);
  }
};
