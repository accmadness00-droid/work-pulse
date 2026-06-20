import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form, Input, InputNumber, Select, Space, Typography, message } from "antd";
import { useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { CreateBranchRequest, branchApi } from "../../features/branch/api/branchApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { PHONE_NUMBER_PLACEHOLDER, phoneNumberRules } from "../../shared/validation/phoneNumber";

type BranchFormValues = CreateBranchRequest;

export default function BranchFormPage() {
  const [form] = Form.useForm<BranchFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const isEdit = Boolean(id);

  const companiesQuery = useAccessibleCompanies();

  const branchQuery = useQuery({
    queryKey: ["branches", "detail", id],
    queryFn: () => branchApi.getBranch(id!),
    enabled: isEdit
  });

  useEffect(() => {
    if (!isEdit) {
      const initialCompanyId = searchParams.get("companyId") ?? companiesQuery.data?.[0]?.id;
      if (initialCompanyId) {
        form.setFieldValue("companyId", initialCompanyId);
      }
    }
  }, [companiesQuery.data, form, isEdit, searchParams]);

  useEffect(() => {
    if (branchQuery.data) {
      form.setFieldsValue({
        companyId: branchQuery.data.companyId,
        name: branchQuery.data.name,
        address: branchQuery.data.address ?? undefined,
        latitude: branchQuery.data.latitude ?? undefined,
        longitude: branchQuery.data.longitude ?? undefined,
        radiusMeters: branchQuery.data.geofenceRadiusMeters ?? undefined,
        phone: branchQuery.data.phone ?? undefined
      });
    }
  }, [branchQuery.data, form]);

  const mutation = useMutation({
    mutationFn: (values: BranchFormValues) => (isEdit ? branchApi.updateBranch(id!, values) : branchApi.createBranch(values)),
    onSuccess: () => {
      message.success(isEdit ? "Branch updated" : "Branch created");
      queryClient.invalidateQueries({ queryKey: ["branches"] });
      navigate("/branches");
    },
    onError: () => {
      message.error(isEdit ? "Failed to update branch" : "Failed to create branch");
    }
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>{isEdit ? "Edit Branch" : "Create Branch"}</Typography.Title>
        <Typography.Text type="secondary">Configure branch location and geofence details.</Typography.Text>
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchQuery.isError ? <Alert type="error" message="Failed to load branch" showIcon /> : null}

      <Card loading={(branchQuery.isLoading && isEdit) || companiesQuery.isLoading}>
        <Form<BranchFormValues> form={form} layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item name="companyId" label="Company" rules={[{ required: true, message: "Company is required" }]}>
            <Select
              disabled={isEdit}
              placeholder="Select company"
              options={(companiesQuery.data ?? []).map((company) => ({
                value: company.id,
                label: company.name
              }))}
            />
          </Form.Item>

          <Form.Item name="name" label="Name" rules={[{ required: true, message: "Branch name is required" }]}>
            <Input placeholder="Main Office" />
          </Form.Item>

          <Form.Item name="address" label="Address">
            <Input.TextArea placeholder="Tashkent, Uzbekistan" rows={3} />
          </Form.Item>

          <Form.Item name="phone" label="Phone" rules={phoneNumberRules()}>
            <Input
              placeholder={PHONE_NUMBER_PLACEHOLDER}
              inputMode="tel"
              autoComplete="tel"
              maxLength={16}
            />
          </Form.Item>

          <div className="form-grid">
            <Form.Item name="latitude" label="Latitude">
              <InputNumber className="full-width" placeholder="41.311081" />
            </Form.Item>

            <Form.Item name="longitude" label="Longitude">
              <InputNumber className="full-width" placeholder="69.240562" />
            </Form.Item>

            <Form.Item name="radiusMeters" label="Radius meters">
              <InputNumber className="full-width" min={0} placeholder="150" />
            </Form.Item>
          </div>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={mutation.isPending}>
              Save
            </Button>
            <Button onClick={() => navigate("/branches")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
