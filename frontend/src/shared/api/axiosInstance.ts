import axios from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { tokenStorage } from "../auth/tokenStorage";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
};

type ApiResponse<T> = {
  data: T;
};

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

export const axiosInstance = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json"
  }
});

let refreshPromise: Promise<string> | null = null;

function redirectToLogin() {
  tokenStorage.clear();
  if (window.location.pathname !== "/login") {
    window.location.assign("/login");
  }
}

async function refreshAccessToken() {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new Error("Refresh token is missing");
  }

  const response = await axios.post<ApiResponse<AuthResponse>>(
    `${baseURL}/api/v1/auth/refresh`,
    { refreshToken },
    { headers: { "Content-Type": "application/json" } }
  );
  const tokens = response.data.data;
  tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
  return tokens.accessToken;
}

axiosInstance.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config as RetryableRequestConfig | undefined;
    const isAuthRequest =
      request?.url?.includes("/api/v1/auth/login") ||
      request?.url?.includes("/api/v1/auth/refresh");

    if (error.response?.status !== 401 || !request || request._retry || isAuthRequest) {
      if (error.response?.status === 401 && !isAuthRequest) {
        redirectToLogin();
      }
      return Promise.reject(error);
    }

    request._retry = true;

    try {
      refreshPromise ??= refreshAccessToken().finally(() => {
        refreshPromise = null;
      });
      const accessToken = await refreshPromise;
      request.headers.Authorization = `Bearer ${accessToken}`;
      return axiosInstance(request);
    } catch (refreshError) {
      redirectToLogin();
      return Promise.reject(refreshError);
    }
  }
);
