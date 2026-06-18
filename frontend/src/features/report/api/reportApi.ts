import dayjs from "dayjs";
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

export type AttendanceSummaryResponse = {
  totalEmployees: number;
  presentCount: number;
  lateCount: number;
  absentCount: number;
  leaveCount: number;
  totalWorkMinutes: number;
  averageWorkMinutes: number;
  totalLateMinutes: number;
  sessionsCount: number;
};

export type SummaryReportResponse = AttendanceSummaryResponse;

export type ReportSessionRow = {
  employeeId: string;
  branchId: string;
  date: string;
  checkInTime?: string | null;
  checkOutTime?: string | null;
  status: string;
  lateMinutes: number;
  workMinutes: number;
  method: string;
};

export type DailyReportResponse = {
  companyId: string;
  branchId?: string | null;
  date: string;
  summary: AttendanceSummaryResponse;
  sessions?: ReportSessionRow[];
  rows?: ReportSessionRow[];
};

export type MonthlyReportResponse = {
  companyId: string;
  branchId?: string | null;
  year: number;
  month: number;
  summary: AttendanceSummaryResponse;
  sessions?: ReportSessionRow[];
  rows?: ReportSessionRow[];
};

export type EmployeeReportResponse = {
  employeeId: string;
  from?: string | null;
  to?: string | null;
  summary: AttendanceSummaryResponse;
  sessions: ReportSessionRow[];
};

export type BranchReportResponse = {
  branchId: string;
  from?: string | null;
  to?: string | null;
  summary: AttendanceSummaryResponse;
  sessions: ReportSessionRow[];
};

export type ReportExportType = "DAILY" | "MONTHLY" | "EMPLOYEE" | "BRANCH";

export type ReportExportRequest = {
  type: ReportExportType;
  companyId?: string;
  branchId?: string;
  employeeId?: string;
  from?: string;
  to?: string;
  date?: string;
  year?: number;
  month?: number;
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

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export const reportApi = {
  async getSummary(companyId: string, from: string, to: string, branchId?: string) {
    const response = await axiosInstance.get<ApiResponse<SummaryReportResponse> | SummaryReportResponse>(
      "/api/v1/reports/summary",
      { params: params({ companyId, branchId, from, to }) }
    );
    return unwrap<SummaryReportResponse>(response.data);
  },

  async getDailyReport(companyId: string, date: string, branchId?: string) {
    const response = await axiosInstance.get<ApiResponse<DailyReportResponse> | DailyReportResponse>(
      "/api/v1/reports/daily",
      { params: params({ companyId, branchId, date }) }
    );
    return unwrap<DailyReportResponse>(response.data);
  },

  async getMonthlyReport(companyId: string, year: number, month: number, branchId?: string) {
    const response = await axiosInstance.get<ApiResponse<MonthlyReportResponse> | MonthlyReportResponse>(
      "/api/v1/reports/monthly",
      { params: params({ companyId, branchId, year, month }) }
    );
    return unwrap<MonthlyReportResponse>(response.data);
  },

  async getEmployeeReport(employeeId: string, from?: string, to?: string) {
    const response = await axiosInstance.get<ApiResponse<EmployeeReportResponse> | EmployeeReportResponse>(
      `/api/v1/reports/employee/${employeeId}`,
      { params: params({ from, to }) }
    );
    return unwrap<EmployeeReportResponse>(response.data);
  },

  async getBranchReport(branchId: string, from?: string, to?: string) {
    const response = await axiosInstance.get<ApiResponse<BranchReportResponse> | BranchReportResponse>(
      `/api/v1/reports/branch/${branchId}`,
      { params: params({ from, to }) }
    );
    return unwrap<BranchReportResponse>(response.data);
  },

  async generateDailySnapshot(date: string) {
    const response = await axiosInstance.post<ApiResponse<unknown> | unknown>("/api/v1/reports/snapshots/daily", null, {
      params: params({ date })
    });
    return unwrap<unknown>(response.data);
  },

  async generateMonthlySnapshot(year: number, month: number) {
    const response = await axiosInstance.post<ApiResponse<unknown> | unknown>("/api/v1/reports/snapshots/monthly", null, {
      params: params({ year, month })
    });
    return unwrap<unknown>(response.data);
  },

  async exportExcel(request: ReportExportRequest) {
    const response = await axiosInstance.get<Blob>("/api/v1/reports/export/excel", {
      params: params(request),
      responseType: "blob"
    });
    const filename = `workpulse-report-${request.type.toLowerCase()}-${dayjs().format("YYYY-MM-DD")}.xlsx`;
    downloadBlob(response.data, filename);
  }
};

export function formatMinutes(totalMinutes?: number | null) {
  const minutes = Math.max(0, totalMinutes ?? 0);
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return `${hours}h ${rest}m`;
}
