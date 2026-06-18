import { Suspense, lazy } from "react";
import type { ReactElement } from "react";
import { Navigate, createBrowserRouter } from "react-router-dom";
import AppLayout from "../shared/components/AppLayout";
import ProtectedRoute from "../shared/components/ProtectedRoute";

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
  return <Suspense fallback={<div className="route-loading">Loading...</div>}>{element}</Suspense>;
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
        element: <Navigate to="/dashboard" replace />
      },
      {
        path: "dashboard",
        element: routePage(<DashboardPage />)
      },
      {
        path: "companies",
        element: routePage(<CompaniesPage />)
      },
      {
        path: "companies/new",
        element: routePage(<CompanyFormPage />)
      },
      {
        path: "companies/:id/edit",
        element: routePage(<CompanyFormPage />)
      },
      {
        path: "companies/:id/settings",
        element: routePage(<CompanySettingsPage />)
      },
      {
        path: "branches",
        element: routePage(<BranchesPage />)
      },
      {
        path: "branches/new",
        element: routePage(<BranchFormPage />)
      },
      {
        path: "branches/:id/edit",
        element: routePage(<BranchFormPage />)
      },
      {
        path: "branches/:id/schedule",
        element: routePage(<BranchSchedulePage />)
      },
      {
        path: "employees",
        element: routePage(<EmployeesPage />)
      },
      {
        path: "employees/new",
        element: routePage(<EmployeeFormPage />)
      },
      {
        path: "employees/:id",
        element: routePage(<EmployeeDetailPage />)
      },
      {
        path: "employees/:id/edit",
        element: routePage(<EmployeeFormPage />)
      },
      {
        path: "devices",
        element: routePage(<DevicesPage />)
      },
      {
        path: "devices/new",
        element: routePage(<DeviceFormPage />)
      },
      {
        path: "devices/:id",
        element: routePage(<DeviceDetailPage />)
      },
      {
        path: "devices/:id/edit",
        element: routePage(<DeviceFormPage />)
      },
      {
        path: "credentials",
        element: routePage(<EmployeeCredentialsPage />)
      },
      {
        path: "credentials/new",
        element: routePage(<EmployeeCredentialFormPage />)
      },
      {
        path: "attendance",
        element: routePage(<AttendancePage />)
      },
      {
        path: "attendance/manual",
        element: routePage(<ManualCheckPage />)
      },
      {
        path: "attendance/camera",
        element: routePage(<CameraCheckPage />)
      },
      {
        path: "attendance/employee",
        element: routePage(<EmployeeAttendanceHistoryPage />)
      },
      {
        path: "attendance/:id",
        element: routePage(<AttendanceDetailPage />)
      },
      {
        path: "device-events",
        element: routePage(<DeviceEventsPage />)
      },
      {
        path: "device-events/new",
        element: routePage(<IngestDeviceEventPage />)
      },
      {
        path: "device-events/unprocessed",
        element: routePage(<UnprocessedDeviceEventsPage />)
      },
      {
        path: "device-events/:id",
        element: routePage(<DeviceEventDetailPage />)
      },
      {
        path: "reports",
        element: routePage(<ReportsPage />)
      },
      {
        path: "reports/daily",
        element: routePage(<DailyReportPage />)
      },
      {
        path: "reports/monthly",
        element: routePage(<MonthlyReportPage />)
      },
      {
        path: "reports/employee",
        element: routePage(<EmployeeReportPage />)
      },
      {
        path: "reports/branch",
        element: routePage(<BranchReportPage />)
      },
      {
        path: "reports/payroll",
        element: routePage(<PayrollPage />)
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
