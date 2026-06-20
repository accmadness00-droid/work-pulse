import dayjs from "dayjs";
import { attendanceApi } from "../../attendance/api/attendanceApi";
import { branchApi } from "../../branch/api/branchApi";
import { companyApi } from "../../company/api/companyApi";
import { deviceApi } from "../../device/api/deviceApi";
import { deviceEventApi } from "../../deviceEvent/api/deviceEventApi";
import { employeeApi } from "../../employee/api/employeeApi";
import { reportApi } from "../../report/api/reportApi";
import type { MeResponse } from "../../../shared/auth/useAuth";

export type DashboardStats = {
  companyId?: string;
  companiesCount: number;
  branchesCount: number;
  employeesCount: number;
  activeDevicesCount: number;
  presentToday: number;
  lateToday: number;
  absentToday: number;
  unprocessedEventsCount: number;
  monthlyWorkMinutes: number;
  monthlyLateMinutes: number;
  warnings: string[];
};

const emptyStats: DashboardStats = {
  companiesCount: 0,
  branchesCount: 0,
  employeesCount: 0,
  activeDevicesCount: 0,
  presentToday: 0,
  lateToday: 0,
  absentToday: 0,
  unprocessedEventsCount: 0,
  monthlyWorkMinutes: 0,
  monthlyLateMinutes: 0,
  warnings: []
};

function warning(label: string, result: PromiseSettledResult<unknown>) {
  return result.status === "rejected" ? `${label} could not be loaded` : undefined;
}

function countTodayStatus(rows: Awaited<ReturnType<typeof attendanceApi.getToday>>["content"], status: string) {
  return rows.filter((row) => row.status === status).length;
}

export const dashboardApi = {
  async getDashboardStats(user?: MeResponse): Promise<DashboardStats> {
    const stats: DashboardStats = { ...emptyStats, warnings: [] };

    const companiesResult =
      user?.role === "SUPER_ADMIN"
        ? await Promise.allSettled([companyApi.listCompanies()])
        : [{ status: "fulfilled" as const, value: user?.companyId ? [{ id: user.companyId, name: "Current company" }] : [] }];
    const companies = companiesResult[0].status === "fulfilled" ? companiesResult[0].value : [];

    stats.companiesCount = companies.length;
    const companyId = user?.companyId ?? companies[0]?.id;
    stats.companyId = companyId;
    const branchId = user?.role === "BRANCH_MANAGER" ? user.branchId ?? undefined : undefined;

    // TODO: replace this frontend aggregation with a backend dashboard aggregate endpoint.
    const monthStart = dayjs().startOf("month").format("YYYY-MM-DD");
    const today = dayjs().format("YYYY-MM-DD");

    const branchesPromise = companyId
      ? Promise.allSettled(companies.map((company) => branchApi.listBranches(company.id))).then((results) => {
          results.forEach((result, index) => {
            if (result.status === "rejected") {
              stats.warnings.push(`Branches for ${companies[index].name} could not be loaded`);
            }
          });
          return results.flatMap((result) => (result.status === "fulfilled" ? result.value : []));
        })
      : Promise.resolve([]);

    const [
      branchesResult,
      employeesResult,
      activeDevicesResult,
      todayAttendanceResult,
      unprocessedEventsResult,
      monthlySummaryResult
    ] = await Promise.allSettled([
      branchesPromise,
      employeeApi.listEmployees({ companyId, branchId, page: 0, size: 1 }),
      deviceApi.listDevices({ branchId, status: "ACTIVE", page: 0, size: 1 }),
      attendanceApi.getToday({ companyId, branchId, page: 0, size: 1000 }),
      deviceEventApi.listUnprocessed({ page: 0, size: 1 }),
      companyId ? reportApi.getSummary(companyId, monthStart, today, branchId) : Promise.resolve(undefined)
    ]);

    if (branchesResult.status === "fulfilled") {
      stats.branchesCount = branchesResult.value.length;
    }
    if (employeesResult.status === "fulfilled") {
      stats.employeesCount = employeesResult.value.totalElements;
    }
    if (activeDevicesResult.status === "fulfilled") {
      stats.activeDevicesCount = activeDevicesResult.value.totalElements;
    }
    if (todayAttendanceResult.status === "fulfilled") {
      stats.presentToday = countTodayStatus(todayAttendanceResult.value.content, "PRESENT");
      stats.lateToday = countTodayStatus(todayAttendanceResult.value.content, "LATE");
      stats.absentToday = countTodayStatus(todayAttendanceResult.value.content, "ABSENT");
    }
    if (unprocessedEventsResult.status === "fulfilled") {
      stats.unprocessedEventsCount = unprocessedEventsResult.value.totalElements;
    }
    if (monthlySummaryResult.status === "fulfilled" && monthlySummaryResult.value) {
      stats.monthlyWorkMinutes = monthlySummaryResult.value.totalWorkMinutes;
      stats.monthlyLateMinutes = monthlySummaryResult.value.totalLateMinutes;
    }

    [
      ["Branches", branchesResult],
      ["Employees", employeesResult],
      ["Active devices", activeDevicesResult],
      ["Today attendance", todayAttendanceResult],
      ["Unprocessed device events", unprocessedEventsResult],
      ["Monthly report summary", monthlySummaryResult]
    ].forEach(([label, result]) => {
      const message = warning(label as string, result as PromiseSettledResult<unknown>);
      if (message) {
        stats.warnings.push(message);
      }
    });

    return stats;
  }
};

export function minutesToHours(totalMinutes?: number | null) {
  const minutes = Math.max(0, totalMinutes ?? 0);
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return `${hours}h ${rest}m`;
}
