import { EyeOutlined, PlayCircleOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Modal, Popconfirm, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType, TablePaginationConfig } from "antd/es/table";
import dayjs from "dayjs";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { DeviceEventResponse, deviceEventApi } from "../../features/deviceEvent/api/deviceEventApi";

function shortError(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.length > 80 ? `${value.slice(0, 80)}...` : value;
}

export default function UnprocessedDeviceEventsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);

  const eventsQuery = useQuery({
    queryKey: ["device-events", "unprocessed", { page, size }],
    queryFn: () => deviceEventApi.listUnprocessed({ page: page - 1, size })
  });

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
      title: "Event Time",
      dataIndex: "eventTime",
      key: "eventTime",
      render: (value: string) => dayjs(value).format("YYYY-MM-DD HH:mm:ss")
    },
    {
      title: "Direction",
      dataIndex: "direction",
      key: "direction",
      render: (value: string) => <Tag>{value}</Tag>
    },
    {
      title: "Auth",
      dataIndex: "authType",
      key: "authType",
      render: (value?: string | null) => value || "-"
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
      title: "Retry",
      dataIndex: "retryCount",
      key: "retryCount"
    },
    {
      title: "Error",
      dataIndex: "processingError",
      key: "processingError",
      render: (value?: string | null) => (value ? <Tag color="red">{shortError(value)}</Tag> : "-")
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
          <Popconfirm title="Process this event?" okText="Process" onConfirm={() => processOneMutation.mutate(record.id)}>
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
          <Typography.Title level={3}>Unprocessed Device Events</Typography.Title>
          <Typography.Text type="secondary">Pending or failed events that still need processing.</Typography.Text>
        </div>
        <Button
          icon={<ThunderboltOutlined />}
          loading={processBatchMutation.isPending}
          onClick={() =>
            Modal.confirm({
              title: "Process unprocessed batch?",
              content: "This will ask the backend to process pending events.",
              okText: "Process batch",
              onOk: () => processBatchMutation.mutate()
            })
          }
        >
          Process Batch
        </Button>
      </div>

      {eventsQuery.isError ? <Alert type="error" message="Failed to load unprocessed events" showIcon /> : null}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={eventsQuery.data?.content ?? []}
        loading={eventsQuery.isLoading}
        locale={{ emptyText: <Empty description="No unprocessed events" /> }}
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
        scroll={{ x: 1000 }}
      />
    </Space>
  );
}
