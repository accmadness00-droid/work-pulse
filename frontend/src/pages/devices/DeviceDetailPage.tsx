import { EditOutlined, KeyOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Descriptions, Empty, Modal, Space, Spin, Tag, Typography, message } from "antd";
import dayjs from "dayjs";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { RotateDeviceApiKeyResponse, deviceApi } from "../../features/device/api/deviceApi";

function display(value?: string | number | boolean | null) {
  if (typeof value === "boolean") {
    return value ? "Yes" : "No";
  }
  return value ?? "-";
}

export default function DeviceDetailPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const [rotatedKey, setRotatedKey] = useState<RotateDeviceApiKeyResponse>();

  const deviceQuery = useQuery({
    queryKey: ["devices", "detail", id],
    queryFn: () => deviceApi.getDevice(id!),
    enabled: Boolean(id)
  });

  const branchQuery = useQuery({
    queryKey: ["branches", "detail", deviceQuery.data?.branchId],
    queryFn: () => branchApi.getBranch(deviceQuery.data!.branchId),
    enabled: Boolean(deviceQuery.data?.branchId)
  });

  const rotateMutation = useMutation({
    mutationFn: () => deviceApi.rotateApiKey(id!),
    onSuccess: (data) => {
      setRotatedKey(data);
      message.success("API key rotated");
      queryClient.invalidateQueries({ queryKey: ["devices"] });
      queryClient.invalidateQueries({ queryKey: ["devices", "detail", id] });
    },
    onError: () => message.error("Failed to rotate API key")
  });

  const device = deviceQuery.data;

  if (deviceQuery.isLoading) {
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
          <Typography.Title level={3}>Device Detail</Typography.Title>
          <Typography.Text type="secondary">Connection, branch, and API key status.</Typography.Text>
        </div>
        {device ? (
          <Space>
            <Button icon={<KeyOutlined />} loading={rotateMutation.isPending} onClick={() => rotateMutation.mutate()}>
              Rotate API Key
            </Button>
            <Button type="primary" icon={<EditOutlined />} onClick={() => navigate(`/devices/${device.id}/edit`)}>
              Edit
            </Button>
          </Space>
        ) : null}
      </div>

      {deviceQuery.isError ? <Alert type="error" message="Failed to load device" showIcon /> : null}

      {device ? (
        <Card>
          <Descriptions
            bordered
            column={{ xs: 1, sm: 1, md: 2 }}
            title={
              <Space>
                <span>{device.name}</span>
                <Tag color={device.status === "ACTIVE" ? "green" : "default"}>{device.status}</Tag>
              </Space>
            }
          >
            <Descriptions.Item label="Serial number">{device.serialNumber}</Descriptions.Item>
            <Descriptions.Item label="Branch">{branchQuery.data?.name ?? device.branchId}</Descriptions.Item>
            <Descriptions.Item label="Type">{device.type}</Descriptions.Item>
            <Descriptions.Item label="Connection type">{device.connectionType}</Descriptions.Item>
            <Descriptions.Item label="IP address">{display(device.ipAddress)}</Descriptions.Item>
            <Descriptions.Item label="Port">{device.port}</Descriptions.Item>
            <Descriptions.Item label="Username">{display(device.username)}</Descriptions.Item>
            <Descriptions.Item label="API key configured">{display(device.apiKeyConfigured)}</Descriptions.Item>
            <Descriptions.Item label="Last sync">
              {device.lastSyncTime ? dayjs(device.lastSyncTime).format("YYYY-MM-DD HH:mm") : "-"}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      ) : (
        <Empty description="Device not found" />
      )}

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
