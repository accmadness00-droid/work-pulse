import { EyeOutlined, PlayCircleOutlined, PlusOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, DatePicker, Empty, Modal, Popconfirm, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import dayjs, { Dayjs } from "dayjs";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { deviceApi } from "../../features/device/api/deviceApi";
import {
  DeviceEventAuthType,
  DeviceEventDirection,
  DeviceEventResponse,
  deviceEventApi
} from "../../features/deviceEvent/api/deviceEventApi";

const directionOptions: Array<{ value: DeviceEventDirection; label: string }> = [
  { value: "IN", label: "IN" },
  { value: "OUT", label: "OUT" },
  { value: "UNKNOWN", label: "UNKNOWN" }
];

const authTypeOptions: Array<{ value: DeviceEventAuthType; label: string }> = [
  { value: "CARD", label: "Card" },
  { value: "FACE", label: "Face" },
  { value: "FINGERPRINT", label: "Fingerprint" },
  { value: "QR", label: "QR" }
];

function processedTag(processed: boolean, hasError?: boolean) {
  if (processed) {
    return <Tag color="green">Processed</Tag>;
  }
  return <Tag color={hasError ? "red" : "gold"}>{hasError ? "Failed" : "Pending"}</Tag>;
}

function shortError(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.length > 60 ? `${value.slice(0, 60)}...` : value;
}

export default function DeviceEventsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [deviceId, setDeviceId] = useState<string>();
  const [processed, setProcessed] = useState<boolean>();
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [direction, setDirection] = useState<DeviceEventDirection>();
  const [authType, setAuthType] = useState<DeviceEventAuthType>();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const devicesQuery = useQuery({
    queryKey: ["devices", "event-select"],
    queryFn: () => deviceApi.listDevices({ page: 0, size: 100 })
  });

  const eventsQuery = useQuery({
    queryKey: ["device-events", { deviceId, processed, range, direction, authType, page, size }],
    queryFn: () =>
      deviceEventApi.list({
        deviceId,
        processed,
        from: range?.[0]?.toISOString(),
        to: range?.[1]?.toISOString(),
        direction,
        authType,
        page: page - 1,
        size
      })
  });

  const deviceById = useMemo(
    () => new Map((devicesQuery.data?.content ?? []).map((device) => [device.id, `${device.name} (${device.serialNumber})`])),
    [devicesQuery.data]
  );

  const processOneMutation = useMutation({
    mutationFn: deviceEventApi.processOne,
    onSuccess: () => {
      message.success("Event processed");
      queryClient.invalidateQueries({ queryKey: ["device-events"] });
    },
    onError: () => message.error("Failed to process event")
  });

  const processBatchMutation = useMutation({
    mutationFn: deviceEventApi.processBatch,
    onSuccess: (count) => {
      message.success(`Batch processed: ${count}`);
      queryClient.invalidateQueries({ queryKey: ["device-events"] });
    },
    onError: () => message.error("Failed to process batch")
  });

  const columns: ColumnsType<DeviceEventResponse> = [
    {
      title: "Device",
      dataIndex: "deviceId",
      key: "deviceId",
      render: (value: string) => deviceById.get(value) ?? value
    },
    {
      title: "Employee Code",
      dataIndex: "employeeCode",
      key: "employeeCode",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Credential",
      dataIndex: "credentialValue",
      key: "credentialValue",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Event Time",
      dataIndex: "eventTime",
      key: "eventTime",
      render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm:ss")
    },
    {
      title: "Direction",
      dataIndex: "direction",
      key: "direction",
      render: (value: DeviceEventDirection) => <Tag color={value === "IN" ? "blue" : value === "OUT" ? "purple" : "default"}>{value}</Tag>
    },
    {
      title: "Auth",
      dataIndex: "authType",
      key: "authType",
      render: (value?: DeviceEventAuthType | null) => value || "-"
    },
    {
      title: "Processed",
      dataIndex: "processed",
      key: "processed",
      render: (value: boolean, record) => processedTag(value, Boolean(record.processingError))
    },
    {
      title: "Retry",
      dataIndex: "retryCount",
      key: "retryCount"
    },
    {
      title: "Error",
      dataIndex: "processingError",
      key: "processingError",
      render: shortError
    },
    {
      title: "Actions",
      key: "actions",
      width: 180,
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/device-events/${record.id}`)}>
            Detail
          </Button>
          <Popconfirm
            title="Process this event?"
            okText="Process"
            onConfirm={() => processOneMutation.mutate(record.id)}
          >
            <Button icon={<PlayCircleOutlined />} loading={processOneMutation.isPending}>
              Process
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
          <Typography.Title level={3}>Device Events</Typography.Title>
          <Typography.Text type="secondary">Inspect raw device events and trigger processing.</Typography.Text>
        </div>
        <Space>
          <Button onClick={() => navigate("/device-events/unprocessed")}>Unprocessed</Button>
          <Button
            icon={<ThunderboltOutlined />}
            loading={processBatchMutation.isPending}
            onClick={() =>
              Modal.confirm({
                title: "Process event batch?",
                content: "This will process pending device events using the backend batch processor.",
                okText: "Process batch",
                onOk: () => processBatchMutation.mutate()
              })
            }
          >
            Process Batch
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/device-events/new")}>
            Ingest Event
          </Button>
        </Space>
      </div>

      <div className="filter-bar wrap">
        <Select
          allowClear
          placeholder="Device"
          loading={devicesQuery.isLoading}
          value={deviceId}
          onChange={(value) => {
            setDeviceId(value);
            setPage(1);
          }}
          options={(devicesQuery.data?.content ?? []).map((device) => ({
            value: device.id,
            label: `${device.name} (${device.serialNumber})`
          }))}
          className="company-filter"
        />
        <Select
          allowClear
          placeholder="Processed"
          value={processed}
          onChange={(value) => {
            setProcessed(value);
            setPage(1);
          }}
          options={[
            { value: true, label: "Processed" },
            { value: false, label: "Unprocessed" }
          ]}
          className="status-filter"
        />
        <DatePicker.RangePicker showTime value={range} onChange={(value) => setRange(value)} />
        <Select allowClear placeholder="Direction" value={direction} onChange={setDirection} options={directionOptions} className="status-filter" />
        <Select allowClear placeholder="Auth type" value={authType} onChange={setAuthType} options={authTypeOptions} className="status-filter" />
      </div>

      {eventsQuery.isError ? <Alert type="error" message="Failed to load device events" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={eventsQuery.data?.content ?? []}
        loading={eventsQuery.isLoading}
        locale={{ emptyText: <Empty description="No device events" /> }}
        pagination={{
          current: page,
          pageSize: size,
          total: eventsQuery.data?.totalElements ?? 0,
          showSizeChanger: true
        }}
        onChange={(pagination: TablePaginationConfig) => {
          setPage(pagination.current ?? 1);
          setSize(pagination.pageSize ?? 10);
        }}
        scroll={{ x: 1400 }}
      />
    </Space>
  );
}
