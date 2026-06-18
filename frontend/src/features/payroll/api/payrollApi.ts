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

export type PayrollEmployeeRow = {
  employeeId: string;
  branchId: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  position?: string | null;
  baseSalary: number;
  expectedWorkMinutes: number;
  actualWorkMinutes: number;
  lateMinutes: number;
  overtimeMinutes: number;
  workedDays: number;
  attendanceRate: number;
  grossPay: number;
  attendanceAdjustmentAmount: number;
  automaticBonusAmount: number;
  automaticPenaltyAmount: number;
  manualBonusAmount: number;
  manualPenaltyAmount: number;
  bonusAmount: number;
  penaltyAmount: number;
  deductions: number;
  netPay: number;
  netDifferenceAmount: number;
  increaseAmount: number;
  decreaseAmount: number;
  explanation: string;
  adjustmentNote?: string | null;
};

export type PayrollSummaryResponse = {
  employeeCount: number;
  expectedWorkMinutes: number;
  actualWorkMinutes: number;
  lateMinutes: number;
  totalBaseSalary: number;
  totalGrossPay: number;
  totalBonusAmount: number;
  totalPenaltyAmount: number;
  totalDeductions: number;
  totalNetPay: number;
  totalNetDifferenceAmount: number;
  totalIncreaseAmount: number;
  totalDecreaseAmount: number;
};

export type PayrollResponse = {
  companyId: string;
  branchId?: string | null;
  year: number;
  month: number;
  policy: {
    overtimeBonusEnabled: boolean;
    latePenaltyEnabled: boolean;
    overtimeMultiplier: number;
    latePenaltyMultiplier: number;
  };
  summary: PayrollSummaryResponse;
  rows: PayrollEmployeeRow[];
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

export const payrollApi = {
  async getMonthlyPayroll(companyId: string, year: number, month: number, branchId?: string) {
    const response = await axiosInstance.get<ApiResponse<PayrollResponse> | PayrollResponse>("/api/v1/payroll/monthly", {
      params: params({ companyId, branchId, year, month })
    });
    return unwrap<PayrollResponse>(response.data);
  },

  async updateAdjustment(
    employeeId: string,
    year: number,
    month: number,
    request: { bonusAmount: number; penaltyAmount: number; note?: string | null }
  ) {
    const response = await axiosInstance.put<ApiResponse<unknown> | unknown>(
      `/api/v1/payroll/monthly/${employeeId}/adjustment`,
      request,
      { params: params({ year, month }) }
    );
    return unwrap<unknown>(response.data);
  }
};
