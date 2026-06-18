import { Card, Empty, Table, Tag } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import {
  AttendanceSummaryResponse,
  ReportSessionRow,
  formatMinutes
} from "../../features/report/api/reportApi";

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
