import { EditOutlined, EyeOutlined, PlusOutlined, PoweroffOutlined, UserOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Avatar, Button, Empty, Image, Input, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { companyApi } from "../../features/company/api/companyApi";
import { EmployeeResponse, employeeApi, employeePhotoUrl } from "../../features/employee/api/employeeApi";

function EmployeePhoto({ photoUrl }: { photoUrl?: string | null }) {
  const [failed, setFailed] = useState(false);
  const src = failed ? undefined : employeePhotoUrl(photoUrl);

  useEffect(() => {
    setFailed(false);
  }, [photoUrl]);

  if (!src) {
    return <Avatar size={56} icon={<UserOutlined />} />;
  }

  return (
    <Image
      src={src}
      alt=""
      width={56}
      height={56}
      className="employee-list-photo"
      preview={{ mask: "View" }}
      fallback=""
      onError={() => setFailed(true)}
    />
  );
}

export default function EmployeesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [search, setSearch] = useState("");
  const [active, setActive] = useState<boolean | undefined>(true);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const companiesQuery = useQuery({
    queryKey: ["companies"],
    queryFn: companyApi.listCompanies
  });

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
    queryKey: ["employees", { companyId, branchId, search, active, page, size }],
    queryFn: () =>
      employeeApi.listEmployees({
        companyId,
        branchId,
        search,
        active,
        page: page - 1,
        size
      }),
    enabled: Boolean(companyId)
  });

  const branchById = useMemo(
    () => new Map((branchesQuery.data ?? []).map((branch) => [branch.id, branch.name])),
    [branchesQuery.data]
  );

  const companyById = useMemo(
    () => new Map((companiesQuery.data ?? []).map((company) => [company.id, company.name])),
    [companiesQuery.data]
  );

  const statusMutation = useMutation({
    mutationFn: ({ id, nextActive }: { id: string; nextActive: boolean }) =>
      nextActive ? employeeApi.activateEmployee(id) : employeeApi.deactivateEmployee(id),
    onSuccess: (_, variables) => {
      message.success(variables.nextActive ? "Employee activated" : "Employee deactivated");
      queryClient.invalidateQueries({ queryKey: ["employees"] });
    },
    onError: () => {
      message.error("Failed to update employee status");
    }
  });

  const handleCompanyChange = (nextCompanyId: string) => {
    setCompanyId(nextCompanyId);
    setBranchId(undefined);
    setPage(1);
  };

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current ?? 1);
    setSize(pagination.pageSize ?? 10);
  };

  const columns: ColumnsType<EmployeeResponse> = [
    {
      title: "Photo",
      dataIndex: "photoUrl",
      key: "photoUrl",
      width: 92,
      render: (photoUrl?: string | null) => <EmployeePhoto photoUrl={photoUrl} />
    },
    {
      title: "Employee",
      key: "employee",
      render: (_, record) => (
        <Space className="employee-list-profile">
          <Space direction="vertical" size={0}>
            <Typography.Text strong>
              {record.firstName} {record.lastName}
            </Typography.Text>
            <Typography.Text type="secondary">{record.employeeCode}</Typography.Text>
          </Space>
        </Space>
      )
    },
    {
      title: "Company",
      dataIndex: "companyId",
      key: "companyId",
      render: (value: string) => companyById.get(value) ?? value
    },
    {
      title: "Branch",
      dataIndex: "branchId",
      key: "branchId",
      render: (value: string) => branchById.get(value) ?? value
    },
    {
      title: "Position",
      dataIndex: "position",
      key: "position",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Phone",
      dataIndex: "phone",
      key: "phone",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Type",
      dataIndex: "employmentType",
      key: "employmentType",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Status",
      dataIndex: "active",
      key: "active",
      render: (value: boolean) => <Tag color={value ? "green" : "default"}>{value ? "Active" : "Inactive"}</Tag>
    },
    {
      title: "Actions",
      key: "actions",
      width: 260,
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/employees/${record.id}`)}>
            View
          </Button>
          <Button icon={<EditOutlined />} onClick={() => navigate(`/employees/${record.id}/edit`)}>
            Edit
          </Button>
          <Popconfirm
            title={record.active ? "Deactivate employee?" : "Activate employee?"}
            okText={record.active ? "Deactivate" : "Activate"}
            okButtonProps={{ danger: record.active }}
            onConfirm={() => statusMutation.mutate({ id: record.id, nextActive: !record.active })}
          >
            <Button icon={<PoweroffOutlined />} danger={record.active} loading={statusMutation.isPending}>
              {record.active ? "Deactivate" : "Activate"}
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Employees</Typography.Title>
          <Typography.Text type="secondary">Manage employee profiles and branch assignments.</Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          disabled={!companyId}
          onClick={() => navigate(companyId ? `/employees/new?companyId=${companyId}` : "/employees/new")}
        >
          Create Employee
        </Button>
      </div>

      <div className="filter-bar wrap">
        <Select
          placeholder="Company"
          loading={companiesQuery.isLoading}
          value={companyId}
          onChange={handleCompanyChange}
          options={(companiesQuery.data ?? []).map((company) => ({ value: company.id, label: company.name }))}
          className="company-filter"
        />
        <Select
          allowClear
          placeholder="Branch"
          loading={branchesQuery.isLoading}
          value={branchId}
          onChange={(value) => {
            setBranchId(value);
            setPage(1);
          }}
          options={(branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name }))}
          className="company-filter"
        />
        <Input.Search
          allowClear
          placeholder="Search by name or code"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          onSearch={(value) => {
            setSearch(value);
            setPage(1);
          }}
          className="search-filter"
        />
        <Select
          value={active}
          onChange={(value) => {
            setActive(value);
            setPage(1);
          }}
          options={[
            { value: true, label: "Active" },
            { value: false, label: "Inactive" }
          ]}
          className="status-filter"
        />
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}
      {employeesQuery.isError ? <Alert type="error" message="Failed to load employees" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={employeesQuery.data?.content ?? []}
        loading={companiesQuery.isLoading || employeesQuery.isLoading}
        locale={{ emptyText: <Empty description={companyId ? "No employees yet" : "Select a company first"} /> }}
        pagination={{
          current: page,
          pageSize: size,
          total: employeesQuery.data?.totalElements ?? 0,
          showSizeChanger: true
        }}
        onChange={handleTableChange}
        scroll={{ x: 1180 }}
      />
    </Space>
  );
}
