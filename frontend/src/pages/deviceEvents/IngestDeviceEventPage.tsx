import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Form, Input, Select, Space, Typography, message } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useNavigate } from "react-router-dom";
import { deviceApi } from "../../features/device/api/deviceApi";
import {
  DeviceEventAuthType,
  DeviceEventDirection,
  IngestDeviceEventRequest,
  deviceEventApi
} from "../../features/deviceEvent/api/deviceEventApi";

type FormValues = Omit<IngestDeviceEventRequest, "eventTime" | "rawPayload"> & {
  selectedDeviceId?: string;
  eventTime: Dayjs;
  rawPayload?: string;
};

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

function trim(value?: string) {
  const next = value?.trim();
  return next || undefined;
}

function parseRawPayload(value?: string) {
  if (!value?.trim()) {
    return undefined;
  }
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

export default function IngestDeviceEventPage() {
  const [form] = Form.useForm<FormValues>();
  const navigate = useNavigate();

  const devicesQuery = useQuery({
    queryKey: ["devices", "event-ingest-select"],
    queryFn: () => deviceApi.listDevices({ page: 0, size: 100 })
  });

  const ingestMutation = useMutation({
    mutationFn: (values: FormValues) =>
      deviceEventApi.ingest({
        deviceSerialNumber: trim(values.deviceSerialNumber),
        externalEventId: trim(values.externalEventId),
        employeeCode: trim(values.employeeCode),
        credentialValue: trim(values.credentialValue),
        eventTime: values.eventTime.toISOString(),
        direction: values.direction,
        authType: values.authType,
        rawPayload: parseRawPayload(values.rawPayload)
      }),
    onSuccess: (event) => {
      message.success("Device event ingested");
      navigate(`/device-events/${event.id}`);
    },
    onError: () => message.error("Failed to ingest device event")
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Ingest Device Event</Typography.Title>
        <Typography.Text type="secondary">Manually submit a raw device event for demo or support workflows.</Typography.Text>
      </div>

      {devicesQuery.isError ? <Alert type="error" message="Failed to load devices" showIcon /> : null}

      <Card loading={devicesQuery.isLoading}>
        <Form<FormValues>
          form={form}
          layout="vertical"
          initialValues={{ eventTime: dayjs(), direction: "IN", authType: "CARD" }}
          onFinish={(values) => ingestMutation.mutate(values)}
        >
          <Form.Item name="selectedDeviceId" label="Device">
            <Select
              allowClear
              placeholder="Select device to fill serial number"
              options={(devicesQuery.data?.content ?? []).map((device) => ({
                value: device.id,
                label: `${device.name} (${device.serialNumber})`
              }))}
              onChange={(value) => {
                const device = devicesQuery.data?.content.find((item) => item.id === value);
                if (device) {
                  form.setFieldValue("deviceSerialNumber", device.serialNumber);
                }
              }}
            />
          </Form.Item>

          <Form.Item name="deviceSerialNumber" label="Device serial number" rules={[{ required: true, message: "Device serial number is required" }]}>
            <Input placeholder="HIK-001" />
          </Form.Item>

          <div className="form-grid two">
            <Form.Item name="externalEventId" label="External event ID">
              <Input placeholder="device-event-001" />
            </Form.Item>
            <Form.Item name="eventTime" label="Event time" rules={[{ required: true, message: "Event time is required" }]}>
              <DatePicker showTime className="full-width" />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="employeeCode" label="Employee code">
              <Input placeholder="EMP001" />
            </Form.Item>
            <Form.Item name="credentialValue" label="Credential value">
              <Input placeholder="EMP001-CARD" />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="direction" label="Direction" rules={[{ required: true, message: "Direction is required" }]}>
              <Select options={directionOptions} />
            </Form.Item>
            <Form.Item name="authType" label="Auth type">
              <Select allowClear options={authTypeOptions} />
            </Form.Item>
          </div>

          <Form.Item name="rawPayload" label="Raw payload JSON">
            <Input.TextArea rows={8} placeholder='{"source":"manual"}' />
          </Form.Item>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={ingestMutation.isPending}>
              Submit
            </Button>
            <Button onClick={() => navigate("/device-events")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
