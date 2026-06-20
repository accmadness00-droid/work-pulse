import { SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Card, Form, Input, Select, Space, Switch, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  CreateEmployeeCredentialRequest,
  employeeCredentialApi
} from "../../features/employeeCredential/api/employeeCredentialApi";
import { employeeApi } from "../../features/employee/api/employeeApi";
import { useLookupOptions } from "../../shared/hooks/useLookups";

type CredentialFormValues = CreateEmployeeCredentialRequest;

export default function EmployeeCredentialFormPage() {
  const [form] = Form.useForm<CredentialFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [employeeSearch, setEmployeeSearch] = useState("");
  const credentialTypeOptions = useLookupOptions("credentialTypes");

  const employeesQuery = useQuery({
    queryKey: ["employees", "credential-form-select", employeeSearch],
    queryFn: () => employeeApi.listEmployees({ search: employeeSearch, active: true, page: 0, size: 50 })
  });

  useEffect(() => {
    const employeeId = searchParams.get("employeeId");
    if (employeeId) {
      form.setFieldValue("employeeId", employeeId);
    }
  }, [form, searchParams]);

  const submitMutation = useMutation({
    mutationFn: async (values: CredentialFormValues) => {
      const credential = await employeeCredentialApi.createCredential({
        employeeId: values.employeeId,
        credentialType: values.credentialType,
        externalId: values.externalId.trim()
      });
      if (values.active === false) {
        return employeeCredentialApi.deactivateCredential(credential.id);
      }
      return credential;
    },
    onSuccess: () => {
      message.success("Credential created");
      queryClient.invalidateQueries({ queryKey: ["credentials"] });
      navigate("/credentials");
    },
    onError: () => message.error("Failed to create credential")
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Create Credential</Typography.Title>
        <Typography.Text type="secondary">Assign a device credential to an employee.</Typography.Text>
      </div>

      {employeesQuery.isError ? <Alert type="error" message="Failed to load employees" showIcon /> : null}

      <Card loading={employeesQuery.isLoading}>
        <Form<CredentialFormValues>
          form={form}
          layout="vertical"
          initialValues={{ credentialType: "CARD", active: true }}
          onFinish={(values) => submitMutation.mutate(values)}
        >
          <Form.Item name="employeeId" label="Employee" rules={[{ required: true, message: "Employee is required" }]}>
            <Select
              showSearch
              placeholder="Search employee"
              onSearch={setEmployeeSearch}
              filterOption={false}
              options={(employeesQuery.data?.content ?? []).map((employee) => ({
                value: employee.id,
                label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
              }))}
            />
          </Form.Item>

          <Form.Item
            name="credentialType"
            label="Credential type"
            rules={[{ required: true, message: "Credential type is required" }]}
          >
            <Select options={credentialTypeOptions.options} loading={credentialTypeOptions.isLoading} />
          </Form.Item>

          <Form.Item name="externalId" label="External ID" rules={[{ required: true, message: "External ID is required" }]}>
            <Input placeholder="EMP001-CARD" />
          </Form.Item>

          <Form.Item name="active" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitMutation.isPending}>
              Save
            </Button>
            <Button onClick={() => navigate("/credentials")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
