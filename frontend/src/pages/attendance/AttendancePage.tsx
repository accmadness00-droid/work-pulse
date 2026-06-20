import { EditOutlined, EyeOutlined, PlusOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, DatePicker, Empty, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  AttendanceMethod,
  AttendanceResponse,
  AttendanceStatus,
  attendanceApi,
  formatMinutes
} from "../../features/attendance/api/attendanceApi";
import { branchApi } from "../../features/branch/api/branchApi";
import { employeeApi } from "../../features/employee/api/employeeApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { useLookupOptions } from "../../shared/hooks/useLookups";
import AttendanceUpdateModal from "./AttendanceUpdateModal";

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function statusTag(status: AttendanceStatus) {
  const color = status === "LATE" ? "red" : status === "PRESENT" ? "green" : "default";
  return <Tag color={color}>{status}</Tag>;
}

export default function AttendancePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [employeeId, setEmployeeId] = useState<string>();
  const [date, setDate] = useState<Dayjs | null>(null);
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [status, setStatus] = useState<AttendanceStatus>();
  const [method, setMethod] = useState<AttendanceMethod>();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [editing, setEditing] = useState<AttendanceResponse>();

  const companiesQuery = useAccessibleCompanies();
  const statusOptions = useLookupOptions("attendanceStatuses");
  const methodOptions = useLookupOptions("attendanceMethods");

  useEffect(() => {
    if (!companyId && companiesQuery.data?.length) {
      setCompanyId(companiesQuery.data[0].id);
    }
  }, [companyId, companiesQuery.data]);

  const branchesQuery = useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => branchApi.listBranches(companyId!),
    enabled: Boolean(companyId)
  });

  const employeesQuery = useQuery({
    queryKey: ["employees", "attendance-select", companyId, branchId, employeeSearch],
    queryFn: () => employeeApi.listEmployees({ companyId, branchId, search: employeeSearch, active: true, page: 0, size: 50 }),
    enabled: Boolean(companyId)
  });

  const attendanceQuery = useQuery({
    queryKey: ["attendance", { employeeId, branchId, date, range, status, method, page, size }],
    queryFn: () =>
      attendanceApi.listAttendance({
        employeeId,
        branchId,
        date: date?.format("YYYY-MM-DD"),
        from: range?.[0]?.format("YYYY-MM-DD"),
        to: range?.[1]?.format("YYYY-MM-DD"),
        status,
        method,
        page: page - 1,
        size
      })
  });

  const employeeById = useMemo(
    () =>
      new Map(
        (employeesQuery.data?.content ?? []).map((employee) => [
          employee.id,
          `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
        ])
      ),
    [employeesQuery.data]
  );

  const branchById = useMemo(
    () => new Map((branchesQuery.data ?? []).map((branch) => [branch.id, branch.name])),
    [branchesQuery.data]
  );

  const updateMutation = useMutation({
    mutationFn: ({ id, values }: { id: string; values: Parameters<typeof attendanceApi.updateAttendance>[1] }) =>
      attendanceApi.updateAttendance(id, values),
    onSuccess: () => {
      message.success("Attendance updated");
      setEditing(undefined);
      queryClient.invalidateQueries({ queryKey: ["attendance"] });
    },
    onError: () => message.error("Failed to update attendance")
  });

  const columns: ColumnsType<AttendanceResponse> = [
    {
      title: "Employee",
      dataIndex: "employeeId",
      key: "employeeId",
      render: (value: string) => employeeById.get(value) ?? value
    },
    {
      title: "Branch",
      dataIndex: "branchId",
      key: "branchId",
      render: (value: string) => branchById.get(value) ?? value
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
      render: statusTag
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
    },
    {
      title: "Actions",
      key: "actions",
      width: 180,
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/attendance/${record.id}`)}>
            Detail
          </Button>
          <Button icon={<EditOutlined />} onClick={() => setEditing(record)}>
            Edit
          </Button>
        </Space>
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Attendance</Typography.Title>
          <Typography.Text type="secondary">Review manual and device attendance sessions.</Typography.Text>
        </div>
        <Space>
          <Button onClick={() => navigate("/attendance/employee")}>Employee History</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/attendance/manual")}>
            Manual Check
          </Button>
        </Space>
      </div>

      <div className="filter-bar wrap">
        <Select
          placeholder="Company"
          loading={companiesQuery.isLoading}
          value={companyId}
          onChange={(value) => {
            setCompanyId(value);
            setBranchId(undefined);
            setEmployeeId(undefined);
            setPage(1);
          }}
          options={(companiesQuery.data ?? []).map((company) => ({ value: company.id, label: company.name }))}
          className="company-filter"
        />
        <Select
          allowClear
          placeholder="Branch"
          value={branchId}
          loading={branchesQuery.isLoading}
          onChange={(value) => {
            setBranchId(value);
            setEmployeeId(undefined);
            setPage(1);
          }}
          options={(branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name }))}
          className="company-filter"
        />
        <Select
          allowClear
          showSearch
          placeholder="Employee"
          value={employeeId}
          filterOption={false}
          onSearch={setEmployeeSearch}
          onChange={(value) => {
            setEmployeeId(value);
            setPage(1);
          }}
          loading={employeesQuery.isLoading}
          options={(employeesQuery.data?.content ?? []).map((employee) => ({
            value: employee.id,
            label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
          }))}
          className="company-filter"
        />
        <DatePicker placeholder="Date" value={date} onChange={(value) => setDate(value)} />
        <DatePicker.RangePicker value={range} onChange={(value) => setRange(value)} />
        <Select
          allowClear
          placeholder="Status"
          value={status}
          onChange={setStatus}
          options={statusOptions.options}
          loading={statusOptions.isLoading}
          className="status-filter"
        />
        <Select
          allowClear
          placeholder="Method"
          value={method}
          onChange={setMethod}
          options={methodOptions.options}
          loading={methodOptions.isLoading}
          className="status-filter"
        />
      </div>

      {attendanceQuery.isError ? <Alert type="error" message="Failed to load attendance" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={attendanceQuery.data?.content ?? []}
        loading={attendanceQuery.isLoading}
        locale={{ emptyText: <Empty description="No attendance records" /> }}
        pagination={{
          current: page,
          pageSize: size,
          total: attendanceQuery.data?.totalElements ?? 0,
          showSizeChanger: true
        }}
        onChange={(pagination: TablePaginationConfig) => {
          setPage(pagination.current ?? 1);
          setSize(pagination.pageSize ?? 10);
        }}
        scroll={{ x: 1300 }}
      />

      <AttendanceUpdateModal
        open={Boolean(editing)}
        attendance={editing}
        loading={updateMutation.isPending}
        onCancel={() => setEditing(undefined)}
        onSubmit={(values) => editing && updateMutation.mutate({ id: editing.id, values })}
      />
    </Space>
  );
}
