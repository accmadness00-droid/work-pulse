import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { useLookupOptions } from "../../shared/hooks/useLookups";
import {
  CreateDeviceRequest,
  UpdateDeviceRequest,
  deviceApi
} from "../../features/device/api/deviceApi";

type DeviceFormValues = CreateDeviceRequest & {
  companyId?: string;
  active?: boolean;
};

function cleanText(value?: string) {
  const next = value?.trim();
  return next ? next : undefined;
}

function toRequest(values: DeviceFormValues): CreateDeviceRequest | UpdateDeviceRequest {
  return {
    name: values.name.trim(),
    serialNumber: values.serialNumber.trim(),
    ipAddress: cleanText(values.ipAddress),
    port: values.port,
    username: cleanText(values.username),
    credentialsSecret: cleanText(values.credentialsSecret),
    branchId: values.branchId,
    type: values.type,
    connectionType: values.connectionType
  };
}

export default function DeviceFormPage() {
  const [form] = Form.useForm<DeviceFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const isEdit = Boolean(id);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>();

  const companiesQuery = useAccessibleCompanies();
  const deviceTypeOptions = useLookupOptions("deviceTypes");
  const connectionTypeOptions = useLookupOptions("deviceConnectionTypes");

  const deviceQuery = useQuery({
    queryKey: ["devices", "detail", id],
    queryFn: () => deviceApi.getDevice(id!),
    enabled: isEdit
  });

  const deviceBranchQuery = useQuery({
    queryKey: ["branches", "detail", deviceQuery.data?.branchId],
    queryFn: () => branchApi.getBranch(deviceQuery.data!.branchId),
    enabled: Boolean(deviceQuery.data?.branchId)
  });

  useEffect(() => {
    if (!isEdit && !selectedCompanyId && companiesQuery.data?.length) {
      setSelectedCompanyId(companiesQuery.data[0].id);
      form.setFieldValue("companyId", companiesQuery.data[0].id);
    }
  }, [companiesQuery.data, form, isEdit, selectedCompanyId]);

  useEffect(() => {
    if (deviceQuery.data) {
      form.setFieldsValue({
        name: deviceQuery.data.name,
        serialNumber: deviceQuery.data.serialNumber,
        ipAddress: deviceQuery.data.ipAddress ?? undefined,
        port: deviceQuery.data.port,
        username: deviceQuery.data.username ?? undefined,
        branchId: deviceQuery.data.branchId,
        type: deviceQuery.data.type,
        connectionType: deviceQuery.data.connectionType,
        active: deviceQuery.data.status === "ACTIVE"
      });
    }
  }, [deviceQuery.data, form]);

  useEffect(() => {
    if (deviceBranchQuery.data) {
      setSelectedCompanyId(deviceBranchQuery.data.companyId);
      form.setFieldValue("companyId", deviceBranchQuery.data.companyId);
    }
  }, [deviceBranchQuery.data, form]);

  const branchesQuery = useQuery({
    queryKey: ["branches", selectedCompanyId],
    queryFn: () => branchApi.listBranches(selectedCompanyId!),
    enabled: Boolean(selectedCompanyId)
  });

  useEffect(() => {
    if (isEdit && deviceQuery.data && branchesQuery.data?.some((branch) => branch.id === deviceQuery.data?.branchId)) {
      form.setFieldValue("branchId", deviceQuery.data.branchId);
    }
  }, [branchesQuery.data, deviceQuery.data, form, isEdit]);

  const submitMutation = useMutation({
    mutationFn: async (values: DeviceFormValues) => {
      let device = isEdit
        ? await deviceApi.updateDevice(id!, toRequest(values))
        : await deviceApi.createDevice(toRequest(values));

      if (values.active !== undefined && values.active !== (device.status === "ACTIVE")) {
        device = values.active ? await deviceApi.activateDevice(device.id) : await deviceApi.deactivateDevice(device.id);
      }

      return device;
    },
    onSuccess: () => {
      message.success(isEdit ? "Device updated" : "Device created");
      queryClient.invalidateQueries({ queryKey: ["devices"] });
      navigate("/devices");
    },
    onError: () => message.error(isEdit ? "Failed to update device" : "Failed to create device")
  });

  const companyOptions = useMemo(
    () => (companiesQuery.data ?? []).map((company) => ({ value: company.id, label: company.name })),
    [companiesQuery.data]
  );

  const branchOptions = useMemo(
    () => (branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name })),
    [branchesQuery.data]
  );

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>{isEdit ? "Edit Device" : "Create Device"}</Typography.Title>
        <Typography.Text type="secondary">Configure device connection and assignment.</Typography.Text>
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}
      {deviceQuery.isError ? <Alert type="error" message="Failed to load device" showIcon /> : null}

      <Card loading={(deviceQuery.isLoading && isEdit) || companiesQuery.isLoading}>
        <Form<DeviceFormValues>
          form={form}
          layout="vertical"
          initialValues={{ type: "HIKVISION", connectionType: "PUSH", port: 80, active: true }}
          onFinish={(values) => submitMutation.mutate(values)}
        >
          <div className="form-grid two">
            <Form.Item name="companyId" label="Company">
              <Select
                placeholder="Select company to load branches"
                options={companyOptions}
                onChange={(value) => {
                  setSelectedCompanyId(value);
                  form.setFieldValue("branchId", undefined);
                }}
              />
            </Form.Item>

            <Form.Item name="branchId" label="Branch" rules={[{ required: true, message: "Branch is required" }]}>
              <Select placeholder="Select branch" loading={branchesQuery.isLoading} options={branchOptions} />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="name" label="Name" rules={[{ required: true, message: "Name is required" }]}>
              <Input placeholder="HIK-001" />
            </Form.Item>
            <Form.Item name="serialNumber" label="Serial number" rules={[{ required: true, message: "Serial number is required" }]}>
              <Input placeholder="HIK-001" />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="ipAddress" label="IP address">
              <Input placeholder="192.168.1.50" />
            </Form.Item>
            <Form.Item name="port" label="Port">
              <InputNumber className="full-width" min={1} max={65535} />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="username" label="Username">
              <Input placeholder="admin" />
            </Form.Item>
            <Form.Item name="credentialsSecret" label="Credentials secret">
              <Input.Password placeholder={isEdit ? "Leave empty to keep current secret" : "Device password or secret"} />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="type" label="Type" rules={[{ required: true, message: "Type is required" }]}>
              <Select options={deviceTypeOptions.options} loading={deviceTypeOptions.isLoading} />
            </Form.Item>
            <Form.Item
              name="connectionType"
              label="Connection type"
              rules={[{ required: true, message: "Connection type is required" }]}
            >
              <Select options={connectionTypeOptions.options} loading={connectionTypeOptions.isLoading} />
            </Form.Item>
          </div>

          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitMutation.isPending}>
              Save
            </Button>
            <Button onClick={() => navigate("/devices")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
