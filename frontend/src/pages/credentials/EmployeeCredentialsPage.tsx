import { DeleteOutlined, PlusOutlined, PoweroffOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Input, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CredentialType,
  EmployeeCredentialResponse,
  employeeCredentialApi
} from "../../features/employeeCredential/api/employeeCredentialApi";
import { employeeApi } from "../../features/employee/api/employeeApi";

const credentialTypeOptions: Array<{ value: CredentialType; label: string }> = [
  { value: "CARD", label: "Card" },
  { value: "FACE", label: "Face" },
  { value: "FINGERPRINT", label: "Fingerprint" },
  { value: "QR", label: "QR" }
];

export default function EmployeeCredentialsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [employeeId, setEmployeeId] = useState<string>();
  const [credentialType, setCredentialType] = useState<CredentialType>();
  const [active, setActive] = useState<boolean | undefined>(true);

  const employeesQuery = useQuery({
    queryKey: ["employees", "credential-select", employeeSearch],
    queryFn: () => employeeApi.listEmployees({ search: employeeSearch, active: true, page: 0, size: 50 })
  });

  const credentialsQuery = useQuery({
    queryKey: ["credentials", { employeeId, credentialType, active }],
    queryFn: () => employeeCredentialApi.listCredentials({ employeeId, credentialType, active })
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

  const statusMutation = useMutation({
    mutationFn: ({ id, nextActive }: { id: string; nextActive: boolean }) =>
      nextActive ? employeeCredentialApi.activateCredential(id) : employeeCredentialApi.deactivateCredential(id),
    onSuccess: (_, variables) => {
      message.success(variables.nextActive ? "Credential activated" : "Credential deactivated");
      queryClient.invalidateQueries({ queryKey: ["credentials"] });
    },
    onError: () => message.error("Failed to update credential status")
  });

  const deleteMutation = useMutation({
    mutationFn: employeeCredentialApi.deleteCredential,
    onSuccess: () => {
      message.success("Credential deleted");
      queryClient.invalidateQueries({ queryKey: ["credentials"] });
    },
    onError: () => message.error("Failed to delete credential")
  });

  const columns: ColumnsType<EmployeeCredentialResponse> = [
    {
      title: "Employee",
      dataIndex: "employeeId",
      key: "employeeId",
      render: (value: string) => employeeById.get(value) ?? value
    },
    {
      title: "Type",
      dataIndex: "credentialType",
      key: "credentialType"
    },
    {
      title: "External ID",
      dataIndex: "externalId",
      key: "externalId"
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
      width: 250,
      render: (_, record) => (
        <Space>
          <Popconfirm
            title={record.active ? "Deactivate credential?" : "Activate credential?"}
            okText={record.active ? "Deactivate" : "Activate"}
            okButtonProps={{ danger: record.active }}
            onConfirm={() => statusMutation.mutate({ id: record.id, nextActive: !record.active })}
          >
            <Button icon={<PoweroffOutlined />} danger={record.active} loading={statusMutation.isPending}>
              {record.active ? "Deactivate" : "Activate"}
            </Button>
          </Popconfirm>
          <Popconfirm
            title="Delete credential?"
            okText="Delete"
            okButtonProps={{ danger: true }}
            onConfirm={() => deleteMutation.mutate(record.id)}
          >
            <Button danger icon={<DeleteOutlined />} loading={deleteMutation.isPending}>
              Delete
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
          <Typography.Title level={3}>Credentials</Typography.Title>
          <Typography.Text type="secondary">Manage employee card, face, fingerprint, and QR identifiers.</Typography.Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/credentials/new")}>
          Create Credential
        </Button>
      </div>

      <div className="filter-bar wrap">
        <Select
          allowClear
          showSearch
          placeholder="Employee"
          value={employeeId}
          onSearch={setEmployeeSearch}
          onChange={setEmployeeId}
          filterOption={false}
          loading={employeesQuery.isLoading}
          options={(employeesQuery.data?.content ?? []).map((employee) => ({
            value: employee.id,
            label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
          }))}
          className="company-filter"
        />
        <Select
          allowClear
          placeholder="Credential type"
          value={credentialType}
          onChange={setCredentialType}
          options={credentialTypeOptions}
          className="company-filter"
        />
        <Select
          allowClear
          placeholder="Status"
          value={active}
          onChange={setActive}
          options={[
            { value: true, label: "Active" },
            { value: false, label: "Inactive" }
          ]}
          className="status-filter"
        />
        <Input.Search
          allowClear
          placeholder="Search employees"
          onSearch={setEmployeeSearch}
          onChange={(event) => setEmployeeSearch(event.target.value)}
          className="search-filter"
        />
      </div>

      {employeesQuery.isError ? <Alert type="error" message="Failed to load employees" showIcon /> : null}
      {credentialsQuery.isError ? <Alert type="error" message="Failed to load credentials" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={credentialsQuery.data ?? []}
        loading={credentialsQuery.isLoading}
        locale={{ emptyText: <Empty description="No credentials yet" /> }}
        pagination={{ pageSize: 10 }}
      />
    </Space>
  );
}
