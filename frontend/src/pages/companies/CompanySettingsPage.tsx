import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form, InputNumber, Select, Space, Switch, Typography, message } from "antd";
import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { UpdateCompanySettingsRequest, companyApi } from "../../features/company/api/companyApi";
import { useLookupOptions } from "../../shared/hooks/useLookups";

export default function CompanySettingsPage() {
  const [form] = Form.useForm<UpdateCompanySettingsRequest>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const timezoneOptions = useLookupOptions("companyTimezones");
  const localeOptions = useLookupOptions("companyLocales");
  const planOptions = useLookupOptions("companyPlans");

  const settingsQuery = useQuery({
    queryKey: ["companies", id, "settings"],
    queryFn: () => companyApi.getSettings(id!),
    enabled: Boolean(id)
  });

  useEffect(() => {
    if (settingsQuery.data) {
      form.setFieldsValue(settingsQuery.data);
    }
  }, [form, settingsQuery.data]);

  const mutation = useMutation({
    mutationFn: (values: UpdateCompanySettingsRequest) => companyApi.updateSettings(id!, values),
    onSuccess: () => {
      message.success("Company settings saved");
      queryClient.invalidateQueries({ queryKey: ["companies"] });
      queryClient.invalidateQueries({ queryKey: ["companies", id, "settings"] });
      navigate("/companies");
    },
    onError: () => {
      message.error("Failed to save company settings");
    }
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Company Settings</Typography.Title>
        <Typography.Text type="secondary">Update timezone, locale, plan, and payroll policy values.</Typography.Text>
      </div>

      {settingsQuery.isError ? <Alert type="error" message="Failed to load settings" showIcon /> : null}

      <Card loading={settingsQuery.isLoading}>
        <Form<UpdateCompanySettingsRequest>
          form={form}
          layout="vertical"
          initialValues={{
            timezone: "Asia/Tashkent",
            locale: "uz-UZ",
            plan: "FREE",
            payrollOvertimeBonusEnabled: false,
            payrollLatePenaltyEnabled: false,
            payrollOvertimeMultiplier: 1.5,
            payrollLatePenaltyMultiplier: 1
          }}
          onFinish={(values) => mutation.mutate(values)}
        >
          <Form.Item name="timezone" label="Timezone" rules={[{ required: true, message: "Timezone is required" }]}>
            <Select options={timezoneOptions.options} loading={timezoneOptions.isLoading} />
          </Form.Item>

          <Form.Item name="locale" label="Locale" rules={[{ required: true, message: "Locale is required" }]}>
            <Select options={localeOptions.options} loading={localeOptions.isLoading} />
          </Form.Item>

          <Form.Item name="plan" label="Plan" rules={[{ required: true, message: "Plan is required" }]}>
            <Select options={planOptions.options} loading={planOptions.isLoading} />
          </Form.Item>

          <div className="form-grid two">
            <Form.Item name="payrollOvertimeBonusEnabled" label="Overtime Bonus" valuePropName="checked">
              <Switch checkedChildren="Enabled" unCheckedChildren="Disabled" />
            </Form.Item>

            <Form.Item
              name="payrollOvertimeMultiplier"
              label="Overtime Multiplier"
              rules={[{ required: true, message: "Overtime multiplier is required" }]}
            >
              <InputNumber min={1} step={0.1} className="full-width" />
            </Form.Item>

            <Form.Item name="payrollLatePenaltyEnabled" label="Late Penalty" valuePropName="checked">
              <Switch checkedChildren="Enabled" unCheckedChildren="Disabled" />
            </Form.Item>

            <Form.Item
              name="payrollLatePenaltyMultiplier"
              label="Late Penalty Multiplier"
              rules={[{ required: true, message: "Late penalty multiplier is required" }]}
            >
              <InputNumber min={0} step={0.1} className="full-width" />
            </Form.Item>
          </div>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={mutation.isPending}>
              Save
            </Button>
            <Button onClick={() => navigate("/companies")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
