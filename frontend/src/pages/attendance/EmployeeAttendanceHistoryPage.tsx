import { useQuery } from "@tanstack/react-query";
import { Alert, Card, DatePicker, Empty, Select, Space, Statistic, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs, { Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { AttendanceResponse, attendanceApi, formatMinutes } from "../../features/attendance/api/attendanceApi";
import { employeeApi } from "../../features/employee/api/employeeApi";

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

export default function EmployeeAttendanceHistoryPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [employeeId, setEmployeeId] = useState<string | undefined>(searchParams.get("employeeId") ?? undefined);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  const employeesQuery = useQuery({
    queryKey: ["employees", "attendance-history-select", employeeSearch],
    queryFn: () => employeeApi.listEmployees({ search: employeeSearch, active: true, page: 0, size: 50 })
  });

  const historyQuery = useQuery({
    queryKey: ["attendance", "employee-history", employeeId, range],
    queryFn: () =>
      attendanceApi.getEmployeeHistory(employeeId!, {
        from: range?.[0]?.format("YYYY-MM-DD"),
        to: range?.[1]?.format("YYYY-MM-DD")
      }),
    enabled: Boolean(employeeId)
  });

  const summary = useMemo(() => {
    const rows = historyQuery.data ?? [];
    return {
      totalSessions: rows.length,
      totalWorkMinutes: rows.reduce((sum, item) => sum + (item.workMinutes ?? 0), 0),
      totalLateMinutes: rows.reduce((sum, item) => sum + (item.lateMinutes ?? 0), 0),
      lateCount: rows.filter((item) => item.status === "LATE").length
    };
  }, [historyQuery.data]);

  const columns: ColumnsType<AttendanceResponse> = [
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
      render: (value: string) => <Tag color={value === "LATE" ? "red" : value === "PRESENT" ? "green" : "default"}>{value}</Tag>
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

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Employee Attendance History</Typography.Title>
        <Typography.Text type="secondary">Review one employee's attendance sessions by period.</Typography.Text>
      </div>

      <div className="filter-bar wrap">
        <Select
          showSearch
          allowClear
          placeholder="Search employee"
          value={employeeId}
          filterOption={false}
          onSearch={setEmployeeSearch}
          onChange={(value) => {
            setEmployeeId(value);
            const nextParams = new URLSearchParams(searchParams);
            if (value) {
              nextParams.set("employeeId", value);
            } else {
              nextParams.delete("employeeId");
            }
            setSearchParams(nextParams);
          }}
          loading={employeesQuery.isLoading}
          options={(employeesQuery.data?.content ?? []).map((employee) => ({
            value: employee.id,
            label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
          }))}
          className="company-filter"
        />
        <DatePicker.RangePicker value={range} onChange={(value) => setRange(value)} />
      </div>

      {employeesQuery.isError ? <Alert type="error" message="Failed to load employees" showIcon /> : null}
      {historyQuery.isError ? <Alert type="error" message="Failed to load attendance history" showIcon /> : null}

      <div className="summary-grid">
        <Card>
          <Statistic title="Total sessions" value={summary.totalSessions} />
        </Card>
        <Card>
          <Statistic title="Total work" value={formatMinutes(summary.totalWorkMinutes)} />
        </Card>
        <Card>
          <Statistic title="Total late" value={`${summary.totalLateMinutes} min`} valueStyle={{ color: "#cf1322" }} />
        </Card>
        <Card>
          <Statistic title="Late count" value={summary.lateCount} valueStyle={{ color: "#cf1322" }} />
        </Card>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={historyQuery.data ?? []}
        loading={historyQuery.isLoading}
        locale={{ emptyText: <Empty description={employeeId ? "No attendance history" : "Select an employee first"} /> }}
        pagination={{ pageSize: 10 }}
        scroll={{ x: 900 }}
      />
    </Space>
  );
}
