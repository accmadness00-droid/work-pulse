import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { useNavigate } from "react-router-dom";
import { axiosInstance } from "../api/axiosInstance";
import { tokenStorage } from "./tokenStorage";

type ApiResponse<T> = {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
};

type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
};

type MeResponse = {
  id: string;
  email: string;
  role: string;
  companyId: string | null;
  branchId: string | null;
  employeeId: string | null;
};

type LoginRequest = {
  email: string;
  password: string;
};

async function login(request: LoginRequest) {
  const response = await axiosInstance.post<ApiResponse<AuthResponse>>("/api/v1/auth/login", request);
  return response.data.data;
}

async function me() {
  const response = await axiosInstance.get<ApiResponse<MeResponse>>("/api/v1/auth/me");
  return response.data.data;
}

async function logoutRequest(refreshToken: string) {
  await axiosInstance.post("/api/v1/auth/logout", { refreshToken });
}

export function useAuth() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const meQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: me,
    enabled: tokenStorage.hasAccessToken(),
    retry: false
  });

  const homePath = (role?: string | null) => (role === "EMPLOYEE" ? "/attendance/camera" : "/dashboard");

  const loginMutation = useMutation<AuthResponse, AxiosError<ApiResponse<unknown>>, LoginRequest>({
    mutationFn: login,
    onSuccess: async (data) => {
      tokenStorage.setTokens(data.accessToken, data.refreshToken);
      const currentUser = await queryClient.fetchQuery({
        queryKey: ["auth", "me"],
        queryFn: me
      });
      navigate(homePath(currentUser.role), { replace: true });
    }
  });

  const logout = async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await logoutRequest(refreshToken);
      }
    } finally {
      tokenStorage.clear();
      queryClient.clear();
      navigate("/login", { replace: true });
    }
  };

  return {
    user: meQuery.data,
    homePath: homePath(meQuery.data?.role),
    isAuthenticated: tokenStorage.hasAccessToken(),
    isLoadingUser: meQuery.isLoading,
    login: loginMutation.mutate,
    loginAsync: loginMutation.mutateAsync,
    isLoggingIn: loginMutation.isPending,
    loginError: loginMutation.error,
    logout
  };
}
