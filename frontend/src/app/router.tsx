import { Suspense, lazy } from "react";
import type { ReactElement } from "react";
import { Navigate, createBrowserRouter } from "react-router-dom";
import type { Permission } from "../shared/auth/authorization";
import { defaultPathForRole } from "../shared/auth/authorization";
import { useAuth } from "../shared/auth/useAuth";
import AppLayout from "../shared/components/AppLayout";
import PermissionRoute from "../shared/components/PermissionRoute";
import ProtectedRoute from "../shared/components/ProtectedRoute";
import { useTranslation } from "../shared/i18n/I18nProvider";

const LoginPage = lazy(() => import("../pages/LoginPage"));
const DashboardPage = lazy(() => import("../pages/DashboardPage"));
const CompaniesPage = lazy(() => import("../pages/companies/CompaniesPage"));
const CompanyFormPage = lazy(() => import("../pages/companies/CompanyFormPage"));
const CompanySettingsPage = lazy(() => import("../pages/companies/CompanySettingsPage"));
const BranchesPage = lazy(() => import("../pages/branches/BranchesPage"));
const BranchFormPage = lazy(() => import("../pages/branches/BranchFormPage"));
const BranchSchedulePage = lazy(() => import("../pages/branches/BranchSchedulePage"));
const EmployeesPage = lazy(() => import("../pages/employees/EmployeesPage"));
const EmployeeFormPage = lazy(() => import("../pages/employees/EmployeeFormPage"));
const EmployeeDetailPage = lazy(() => import("../pages/employees/EmployeeDetailPage"));
const DevicesPage = lazy(() => import("../pages/devices/DevicesPage"));
const DeviceFormPage = lazy(() => import("../pages/devices/DeviceFormPage"));
const DeviceDetailPage = lazy(() => import("../pages/devices/DeviceDetailPage"));
const EmployeeCredentialsPage = lazy(() => import("../pages/credentials/EmployeeCredentialsPage"));
const EmployeeCredentialFormPage = lazy(() => import("../pages/credentials/EmployeeCredentialFormPage"));
const AttendancePage = lazy(() => import("../pages/attendance/AttendancePage"));
const ManualCheckPage = lazy(() => import("../pages/attendance/ManualCheckPage"));
const CameraCheckPage = lazy(() => import("../pages/attendance/CameraCheckPage"));
const EmployeeAttendanceHistoryPage = lazy(() => import("../pages/attendance/EmployeeAttendanceHistoryPage"));
const AttendanceDetailPage = lazy(() => import("../pages/attendance/AttendanceDetailPage"));
const DeviceEventsPage = lazy(() => import("../pages/deviceEvents/DeviceEventsPage"));
const IngestDeviceEventPage = lazy(() => import("../pages/deviceEvents/IngestDeviceEventPage"));
const UnprocessedDeviceEventsPage = lazy(() => import("../pages/deviceEvents/UnprocessedDeviceEventsPage"));
const DeviceEventDetailPage = lazy(() => import("../pages/deviceEvents/DeviceEventDetailPage"));
const ReportsPage = lazy(() => import("../pages/reports/ReportsPage"));
const DailyReportPage = lazy(() => import("../pages/reports/DailyReportPage"));
const MonthlyReportPage = lazy(() => import("../pages/reports/MonthlyReportPage"));
const EmployeeReportPage = lazy(() => import("../pages/reports/EmployeeReportPage"));
const BranchReportPage = lazy(() => import("../pages/reports/BranchReportPage"));
const PayrollPage = lazy(() => import("../pages/payroll/PayrollPage"));

function routePage(element: ReactElement) {
  return <Suspense fallback={<RouteLoading />}>{element}</Suspense>;
}

function RouteLoading() {
  const { t } = useTranslation();
  return <div className="route-loading">{t("app.loading")}</div>;
}

function permissionPage(permission: Permission, element: ReactElement) {
  return <PermissionRoute permission={permission}>{routePage(element)}</PermissionRoute>;
}

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={defaultPathForRole(user?.role)} replace />;
}

export const router = createBrowserRouter([
  {
    path: "/login",
    element: routePage(<LoginPage />)
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <AppLayout />
      </ProtectedRoute>
    ),
    children: [
      {
        index: true,
        element: <HomeRedirect />
      },
      {
        path: "dashboard",
        element: permissionPage("VIEW_DASHBOARD", <DashboardPage />)
      },
      {
        path: "companies",
        element: permissionPage("MANAGE_COMPANIES", <CompaniesPage />)
      },
      {
        path: "companies/new",
        element: permissionPage("MANAGE_COMPANIES", <CompanyFormPage />)
      },
      {
        path: "companies/:id/edit",
        element: permissionPage("VIEW_COMPANY_SETTINGS", <CompanyFormPage />)
      },
      {
        path: "companies/:id/settings",
        element: permissionPage("VIEW_COMPANY_SETTINGS", <CompanySettingsPage />)
      },
      {
        path: "branches",
        element: permissionPage("MANAGE_BRANCHES", <BranchesPage />)
      },
      {
        path: "branches/new",
        element: permissionPage("MANAGE_BRANCHES", <BranchFormPage />)
      },
      {
        path: "branches/:id/edit",
        element: permissionPage("MANAGE_BRANCHES", <BranchFormPage />)
      },
      {
        path: "branches/:id/schedule",
        element: permissionPage("MANAGE_BRANCHES", <BranchSchedulePage />)
      },
      {
        path: "employees",
        element: permissionPage("VIEW_EMPLOYEES", <EmployeesPage />)
      },
      {
        path: "employees/new",
        element: permissionPage("CREATE_EMPLOYEES", <EmployeeFormPage />)
      },
      {
        path: "employees/:id",
        element: permissionPage("VIEW_EMPLOYEES", <EmployeeDetailPage />)
      },
      {
        path: "employees/:id/edit",
        element: permissionPage("VIEW_EMPLOYEES", <EmployeeFormPage />)
      },
      {
        path: "devices",
        element: permissionPage("VIEW_DEVICES", <DevicesPage />)
      },
      {
        path: "devices/new",
        element: permissionPage("MANAGE_DEVICES", <DeviceFormPage />)
      },
      {
        path: "devices/:id",
        element: permissionPage("VIEW_DEVICES", <DeviceDetailPage />)
      },
      {
        path: "devices/:id/edit",
        element: permissionPage("MANAGE_DEVICES", <DeviceFormPage />)
      },
      {
        path: "credentials",
        element: permissionPage("MANAGE_CREDENTIALS", <EmployeeCredentialsPage />)
      },
      {
        path: "credentials/new",
        element: permissionPage("MANAGE_CREDENTIALS", <EmployeeCredentialFormPage />)
      },
      {
        path: "attendance",
        element: permissionPage("VIEW_ATTENDANCE", <AttendancePage />)
      },
      {
        path: "attendance/manual",
        element: permissionPage("VIEW_ATTENDANCE", <ManualCheckPage />)
      },
      {
        path: "attendance/camera",
        element: permissionPage("CAMERA_ATTENDANCE", <CameraCheckPage />)
      },
      {
        path: "attendance/employee",
        element: permissionPage("VIEW_ATTENDANCE", <EmployeeAttendanceHistoryPage />)
      },
      {
        path: "attendance/:id",
        element: permissionPage("VIEW_ATTENDANCE", <AttendanceDetailPage />)
      },
      {
        path: "device-events",
        element: permissionPage("VIEW_DEVICE_EVENTS", <DeviceEventsPage />)
      },
      {
        path: "device-events/new",
        element: permissionPage("VIEW_DEVICE_EVENTS", <IngestDeviceEventPage />)
      },
      {
        path: "device-events/unprocessed",
        element: permissionPage("VIEW_DEVICE_EVENTS", <UnprocessedDeviceEventsPage />)
      },
      {
        path: "device-events/:id",
        element: permissionPage("VIEW_DEVICE_EVENTS", <DeviceEventDetailPage />)
      },
      {
        path: "reports",
        element: permissionPage("VIEW_REPORTS", <ReportsPage />)
      },
      {
        path: "reports/daily",
        element: permissionPage("VIEW_REPORTS", <DailyReportPage />)
      },
      {
        path: "reports/monthly",
        element: permissionPage("VIEW_REPORTS", <MonthlyReportPage />)
      },
      {
        path: "reports/employee",
        element: permissionPage("VIEW_REPORTS", <EmployeeReportPage />)
      },
      {
        path: "reports/branch",
        element: permissionPage("VIEW_REPORTS", <BranchReportPage />)
      },
      {
        path: "reports/payroll",
        element: permissionPage("VIEW_PAYROLL", <PayrollPage />)
      },
      {
        path: "payroll",
        element: <Navigate to="/reports/payroll" replace />
      }
    ]
  },
  {
    path: "*",
    element: <Navigate to="/dashboard" replace />
  }
]);
