import { PlayCircleOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Descriptions, Empty, Popconfirm, Space, Spin, Tag, Typography, message } from "antd";
import dayjs from "dayjs";
import { useParams } from "react-router-dom";
import { deviceApi } from "../../features/device/api/deviceApi";
import { DeviceEventDirection, deviceEventApi, prettyRawPayload } from "../../features/deviceEvent/api/deviceEventApi";

function processedTag(processed: boolean, hasError?: boolean) {
  if (processed) {
    return <Tag color="green">Processed</Tag>;
  }
  return <Tag color={hasError ? "red" : "gold"}>{hasError ? "Failed" : "Pending"}</Tag>;
}

export default function DeviceEventDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();

  const eventQuery = useQuery({
    queryKey: ["device-events", "detail", id],
    queryFn: () => deviceEventApi.getById(id!),
    enabled: Boolean(id)
  });

  const deviceQuery = useQuery({
    queryKey: ["devices", "detail", eventQuery.data?.deviceId],
    queryFn: () => deviceApi.getDevice(eventQuery.data!.deviceId),
    enabled: Boolean(eventQuery.data?.deviceId)
  });

  const processMutation = useMutation({
    mutationFn: () => deviceEventApi.processOne(id!),
    onSuccess: () => {
      message.success("Event processed");
      queryClient.invalidateQueries({ queryKey: ["device-events"] });
    },
    onError: () => message.error("Failed to process event")
  });

  const event = eventQuery.data;

  if (eventQuery.isLoading) {
    return (
      <div className="centered-state">
        <Spin />
      </div>
    );
  }

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Device Event Detail</Typography.Title>
          <Typography.Text type="secondary">Raw event data and processing status.</Typography.Text>
        </div>
        {event ? (
          <Popconfirm title="Process this event?" okText="Process" onConfirm={() => processMutation.mutate()}>
            <Button icon={<PlayCircleOutlined />} loading={processMutation.isPending}>
              Process
            </Button>
          </Popconfirm>
        ) : null}
      </div>

      {eventQuery.isError ? <Alert type="error" message="Failed to load device event" showIcon /> : null}

      {event ? (
        <>
          {event.processingError ? <Alert type="error" message="Processing Error" description={event.processingError} showIcon /> : null}
          <Card>
            <Descriptions
              bordered
              column={{ xs: 1, sm: 1, md: 2 }}
              title={
                <Space>
                  <span>{dayjs(event.eventTime).format("YYYY-MM-DD HH:mm:ss")}</span>
                  {processedTag(event.processed, Boolean(event.processingError))}
                </Space>
              }
            >
              <Descriptions.Item label="Device">{deviceQuery.data ? `${deviceQuery.data.name} (${deviceQuery.data.serialNumber})` : event.deviceId}</Descriptions.Item>
              <Descriptions.Item label="External Event ID">{event.externalEventId ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="Employee Code">{event.employeeCode ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="Credential Value">{event.credentialValue ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="Direction">
                <Tag color={event.direction === "IN" ? "blue" : event.direction === "OUT" ? "purple" : "default"}>
                  {event.direction as DeviceEventDirection}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Auth Type">{event.authType ?? "-"}</Descriptions.Item>
              <Descriptions.Item label="Retry Count">{event.retryCount}</Descriptions.Item>
              <Descriptions.Item label="Event Hash">{event.eventHash}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card title="Raw Payload">
            <pre className="raw-payload">{prettyRawPayload(event.rawPayload)}</pre>
          </Card>
        </>
      ) : (
        <Empty description="Device event not found" />
      )}
    </Space>
  );
}
