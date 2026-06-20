import { Card, Empty, Table, Tabs, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import { useLocation, useNavigate } from "react-router-dom";
import {
  AttendanceSummaryResponse,
  ReportSessionRow,
  formatMinutes
} from "../../features/report/api/reportApi";
import type { Permission } from "../../shared/auth/authorization";
import { hasPermission } from "../../shared/auth/authorization";
import { useAuth } from "../../shared/auth/useAuth";

const reportTabs = [
  { key: "summary", label: "Summary", path: "/reports", permission: "VIEW_REPORTS" },
  { key: "daily", label: "Daily", path: "/reports/daily", permission: "VIEW_REPORTS" },
  { key: "monthly", label: "Monthly", path: "/reports/monthly", permission: "VIEW_REPORTS" },
  { key: "employee", label: "Employee", path: "/reports/employee", permission: "VIEW_REPORTS" },
  { key: "branch", label: "Branch", path: "/reports/branch", permission: "VIEW_REPORTS" },
  { key: "payroll", label: "Payroll", path: "/reports/payroll", permission: "VIEW_PAYROLL" }
] satisfies Array<{
  key: string;
  label: string;
  path: string;
  permission: Permission;
}>;

export function ReportNavigation() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const visibleTabs = reportTabs.filter((tab) => hasPermission(user, tab.permission));
  const activeTab = visibleTabs.find((tab) => tab.path === location.pathname)?.key ?? visibleTabs[0]?.key;

  return (
    <Card className="report-navigation-card">
      <Tabs
        activeKey={activeTab}
        items={visibleTabs.map(({ key, label }) => ({ key, label }))}
        onChange={(key) => {
          const destination = visibleTabs.find((tab) => tab.key === key);
          if (destination && destination.path !== location.pathname) {
            navigate(destination.path);
          }
        }}
      />
    </Card>
  );
}

export function SummaryCards({ summary }: { summary?: AttendanceSummaryResponse }) {
  const items = [
    { label: "Total Employees", value: summary?.totalEmployees ?? 0 },
    { label: "Present", value: summary?.presentCount ?? 0 },
    { label: "Late", value: summary?.lateCount ?? 0, danger: true },
    { label: "Absent", value: summary?.absentCount ?? 0 },
    { label: "Leave", value: summary?.leaveCount ?? 0 },
    { label: "Total Work", value: formatMinutes(summary?.totalWorkMinutes ?? 0) },
    { label: "Average Work", value: formatMinutes(summary?.averageWorkMinutes ?? 0) },
    { label: "Total Late", value: `${summary?.totalLateMinutes ?? 0} min`, danger: true },
    { label: "Sessions", value: summary?.sessionsCount ?? 0 }
  ];

  return (
    <div className="summary-grid reports-summary-grid">
      {items.map((item) => (
        <Card key={item.label}>
          <div className="summary-label">{item.label}</div>
          <div className={item.danger ? "summary-value danger" : "summary-value"}>{item.value}</div>
        </Card>
      ))}
    </div>
  );
}

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

const columns: ColumnsType<ReportSessionRow> = [
  {
    title: "Employee",
    dataIndex: "employeeId",
    key: "employeeId"
  },
  {
    title: "Branch",
    dataIndex: "branchId",
    key: "branchId"
  },
  {
    title: "Date",
    dataIndex: "date",
    key: "date"
  },
  {
    title: "Check In",
    dataIndex: "checkInTime",
    key: "checkInTime",
    render: formatDateTime
  },
  {
    title: "Check Out",
    dataIndex: "checkOutTime",
    key: "checkOutTime",
    render: formatDateTime
  },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (status: string) => <Tag color={status === "LATE" ? "red" : status === "PRESENT" ? "green" : "default"}>{status}</Tag>
  },
  {
    title: "Late",
    dataIndex: "lateMinutes",
    key: "lateMinutes",
    render: (value: number) => <Tag color={value > 0 ? "red" : "default"}>{value} min</Tag>
  },
  {
    title: "Work",
    dataIndex: "workMinutes",
    key: "workMinutes",
    render: formatMinutes
  },
  {
    title: "Method",
    dataIndex: "method",
    key: "method"
  }
];

export function ReportSessionTable({ rows, loading }: { rows?: ReportSessionRow[]; loading?: boolean }) {
  return (
    <Table
      rowKey={(row) => `${row.employeeId}-${row.branchId}-${row.date}-${row.checkInTime ?? "none"}`}
      columns={columns}
      dataSource={rows ?? []}
      loading={loading}
      locale={{ emptyText: <Empty description="No report rows" /> }}
      pagination={{ pageSize: 10 }}
      scroll={{ x: 1100 }}
    />
  );
}
