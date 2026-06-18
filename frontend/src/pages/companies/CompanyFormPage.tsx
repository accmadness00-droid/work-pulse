import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form, Input, Space, Typography, message } from "antd";
import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { CreateCompanyRequest, companyApi } from "../../features/company/api/companyApi";

type CompanyFormValues = CreateCompanyRequest & {
  address?: string;
};

export default function CompanyFormPage() {
  const [form] = Form.useForm<CompanyFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const isEdit = Boolean(id);

  const companyQuery = useQuery({
    queryKey: ["companies", id],
    queryFn: () => companyApi.getCompany(id!),
    enabled: isEdit
  });

  useEffect(() => {
    if (companyQuery.data) {
      form.setFieldsValue({
        name: companyQuery.data.name,
        legalName: companyQuery.data.legalName ?? undefined,
        inn: companyQuery.data.inn ?? undefined,
        phone: companyQuery.data.phone ?? undefined,
        email: companyQuery.data.email ?? undefined,
        address: companyQuery.data.address ?? undefined,
        plan: companyQuery.data.plan ?? undefined
      });
    }
  }, [companyQuery.data, form]);

  const mutation = useMutation({
    mutationFn: (values: CompanyFormValues) =>
      isEdit ? companyApi.updateCompany(id!, values) : companyApi.createCompany(values),
    onSuccess: () => {
      message.success(isEdit ? "Company updated" : "Company created");
      queryClient.invalidateQueries({ queryKey: ["companies"] });
      navigate("/companies");
    },
    onError: () => {
      message.error(isEdit ? "Failed to update company" : "Failed to create company");
    }
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>{isEdit ? "Edit Company" : "Create Company"}</Typography.Title>
        <Typography.Text type="secondary">Fill in company details for WorkPulse.</Typography.Text>
      </div>

      {companyQuery.isError ? <Alert type="error" message="Failed to load company" showIcon /> : null}

      <Card loading={companyQuery.isLoading && isEdit}>
        <Form<CompanyFormValues> form={form} layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item name="name" label="Name" rules={[{ required: true, message: "Company name is required" }]}>
            <Input placeholder="WorkPulse Demo LLC" />
          </Form.Item>

          <Form.Item name="legalName" label="Legal name">
            <Input placeholder="WorkPulse Demo LLC" />
          </Form.Item>

          <Form.Item name="inn" label="INN">
            <Input placeholder="123456789" />
          </Form.Item>

          <Form.Item name="phone" label="Phone">
            <Input placeholder="+998901234567" />
          </Form.Item>

          <Form.Item name="email" label="Email" rules={[{ type: "email", message: "Enter a valid email" }]}>
            <Input placeholder="info@workpulse.uz" />
          </Form.Item>

          <Form.Item name="address" label="Address">
            <Input.TextArea placeholder="Tashkent, Uzbekistan" rows={3} />
          </Form.Item>

          <Form.Item name="plan" label="Plan">
            <Input placeholder="FREE" />
          </Form.Item>

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
