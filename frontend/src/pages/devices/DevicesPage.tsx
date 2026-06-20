import { EditOutlined, EyeOutlined, KeyOutlined, PlusOutlined, PoweroffOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { hasPermission } from "../../shared/auth/authorization";
import { useAuth } from "../../shared/auth/useAuth";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { useLookupOptions } from "../../shared/hooks/useLookups";
import {
  DeviceConnectionType,
  DeviceResponse,
  DeviceStatus,
  DeviceType,
  RotateDeviceApiKeyResponse,
  deviceApi
} from "../../features/device/api/deviceApi";

export default function DevicesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const canManageDevices = hasPermission(user, "MANAGE_DEVICES");
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [type, setType] = useState<DeviceType>();
  const [status, setStatus] = useState<DeviceStatus>();
  const [connectionType, setConnectionType] = useState<DeviceConnectionType>();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [rotatedKey, setRotatedKey] = useState<RotateDeviceApiKeyResponse>();

  const companiesQuery = useAccessibleCompanies();
  const deviceTypeOptions = useLookupOptions("deviceTypes");
  const statusOptions = useLookupOptions("deviceStatuses");
  const connectionTypeOptions = useLookupOptions("deviceConnectionTypes");

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

  const devicesQuery = useQuery({
    queryKey: ["devices", { branchId, type, status, connectionType, page, size }],
    queryFn: () =>
      deviceApi.listDevices({
        branchId,
        type,
        status,
        connectionType,
        page: page - 1,
        size
      })
  });

  const branchById = useMemo(
    () => new Map((branchesQuery.data ?? []).map((branch) => [branch.id, branch.name])),
    [branchesQuery.data]
  );

  const statusMutation = useMutation({
    mutationFn: ({ id, nextStatus }: { id: string; nextStatus: DeviceStatus }) =>
      nextStatus === "ACTIVE" ? deviceApi.activateDevice(id) : deviceApi.deactivateDevice(id),
    onSuccess: (_, variables) => {
      message.success(variables.nextStatus === "ACTIVE" ? "Device activated" : "Device deactivated");
      queryClient.invalidateQueries({ queryKey: ["devices"] });
    },
    onError: () => message.error("Failed to update device status")
  });

  const rotateMutation = useMutation({
    mutationFn: deviceApi.rotateApiKey,
    onSuccess: (data) => {
      setRotatedKey(data);
      message.success("API key rotated");
      queryClient.invalidateQueries({ queryKey: ["devices"] });
    },
    onError: () => message.error("Failed to rotate API key")
  });

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setPage(pagination.current ?? 1);
    setSize(pagination.pageSize ?? 10);
  };

  const columns: ColumnsType<DeviceResponse> = [
    {
      title: "Device",
      key: "device",
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.name}</Typography.Text>
          <Typography.Text type="secondary">{record.serialNumber}</Typography.Text>
        </Space>
      )
    },
    {
      title: "Branch",
      dataIndex: "branchId",
      key: "branchId",
      render: (value: string) => branchById.get(value) ?? value
    },
    {
      title: "Type",
      dataIndex: "type",
      key: "type"
    },
    {
      title: "Connection",
      dataIndex: "connectionType",
      key: "connectionType"
    },
    {
      title: "IP",
      dataIndex: "ipAddress",
      key: "ipAddress",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Status",
      dataIndex: "status",
      key: "status",
      render: (value: DeviceStatus) => <Tag color={value === "ACTIVE" ? "green" : "default"}>{value}</Tag>
    },
    {
      title: "Actions",
      key: "actions",
      width: 340,
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/devices/${record.id}`)}>
            View
          </Button>
          {canManageDevices ? (
            <>
              <Button icon={<EditOutlined />} onClick={() => navigate(`/devices/${record.id}/edit`)}>
                Edit
              </Button>
              <Button icon={<KeyOutlined />} loading={rotateMutation.isPending} onClick={() => rotateMutation.mutate(record.id)}>
                Rotate
              </Button>
              <Popconfirm
                title={record.status === "ACTIVE" ? "Deactivate device?" : "Activate device?"}
                okText={record.status === "ACTIVE" ? "Deactivate" : "Activate"}
                okButtonProps={{ danger: record.status === "ACTIVE" }}
                onConfirm={() =>
                  statusMutation.mutate({ id: record.id, nextStatus: record.status === "ACTIVE" ? "INACTIVE" : "ACTIVE" })
                }
              >
                <Button icon={<PoweroffOutlined />} danger={record.status === "ACTIVE"} loading={statusMutation.isPending}>
                  {record.status === "ACTIVE" ? "Deactivate" : "Activate"}
                </Button>
              </Popconfirm>
            </>
          ) : null}
        </Space>
      )
    }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Devices</Typography.Title>
          <Typography.Text type="secondary">Manage biometric and attendance devices.</Typography.Text>
        </div>
        {canManageDevices ? (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/devices/new")}>
            Create Device
          </Button>
        ) : null}
      </div>

      <div className="filter-bar wrap">
        <Select
          placeholder="Company"
          loading={companiesQuery.isLoading}
          value={companyId}
          onChange={(value) => {
            setCompanyId(value);
            setBranchId(undefined);
            setPage(1);
          }}
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
        <Select
          allowClear
          placeholder="Type"
          value={type}
          onChange={setType}
          options={deviceTypeOptions.options}
          loading={deviceTypeOptions.isLoading}
          className="status-filter"
        />
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
          placeholder="Connection"
          value={connectionType}
          onChange={setConnectionType}
          options={connectionTypeOptions.options}
          loading={connectionTypeOptions.isLoading}
          className="company-filter"
        />
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}
      {devicesQuery.isError ? <Alert type="error" message="Failed to load devices" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={devicesQuery.data?.content ?? []}
        loading={devicesQuery.isLoading}
        locale={{ emptyText: <Empty description="No devices yet" /> }}
        pagination={{
          current: page,
          pageSize: size,
          total: devicesQuery.data?.totalElements ?? 0,
          showSizeChanger: true
        }}
        onChange={handleTableChange}
        scroll={{ x: 1200 }}
      />

      <Modal
        title="Rotated API Key"
        open={Boolean(rotatedKey)}
        onCancel={() => setRotatedKey(undefined)}
        footer={[
          <Button key="close" type="primary" onClick={() => setRotatedKey(undefined)}>
            I copied the key
          </Button>
        ]}
      >
        <Alert type="warning" showIcon message="Bu kalit faqat bir marta ko'rsatiladi" className="modal-alert" />
        <Typography.Paragraph copyable className="secret-box">
          {rotatedKey?.apiKey}
        </Typography.Paragraph>
      </Modal>
    </Space>
  );
}
