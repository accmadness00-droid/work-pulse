import { LoginOutlined, LogoutOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Form, Input, Select, Space, Typography, message } from "antd";
import { useEffect, useState } from "react";
import { AttendanceMethod, attendanceApi } from "../../features/attendance/api/attendanceApi";
import { branchApi } from "../../features/branch/api/branchApi";
import { employeeApi } from "../../features/employee/api/employeeApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { useLookupOptions } from "../../shared/hooks/useLookups";

type ManualCheckValues = {
  companyId?: string;
  employeeId: string;
  branchId: string;
  method: AttendanceMethod;
  note?: string;
};

export default function ManualCheckPage() {
  const [form] = Form.useForm<ManualCheckValues>();
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [employeeSearch, setEmployeeSearch] = useState("");

  const companiesQuery = useAccessibleCompanies();
  const methodOptions = useLookupOptions("attendanceMethods");
  const manualMethodOptions = methodOptions.options.filter((option) => option.value !== "WEB_CAMERA");

  useEffect(() => {
    if (!companyId && companiesQuery.data?.length) {
      setCompanyId(companiesQuery.data[0].id);
      form.setFieldValue("companyId", companiesQuery.data[0].id);
    }
  }, [companiesQuery.data, companyId, form]);

  const branchesQuery = useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => branchApi.listBranches(companyId!),
    enabled: Boolean(companyId)
  });

  const employeesQuery = useQuery({
    queryKey: ["employees", "manual-check-select", companyId, branchId, employeeSearch],
    queryFn: () => employeeApi.listEmployees({ companyId, branchId, search: employeeSearch, active: true, page: 0, size: 50 }),
    enabled: Boolean(companyId)
  });

  const checkInMutation = useMutation({
    mutationFn: (values: ManualCheckValues) =>
      attendanceApi.checkIn({
        employeeId: values.employeeId,
        branchId: values.branchId,
        method: values.method,
        note: values.note
      }),
    onSuccess: () => message.success("Check-in created"),
    onError: () => message.error("Failed to check in")
  });

  const checkOutMutation = useMutation({
    mutationFn: (values: ManualCheckValues) =>
      attendanceApi.checkOut({
        employeeId: values.employeeId,
        method: values.method,
        note: values.note
      }),
    onSuccess: () => message.success("Check-out saved"),
    onError: () => message.error("Failed to check out")
  });

  const submit = async (action: "in" | "out") => {
    const values = await form.validateFields();
    if (action === "in") {
      checkInMutation.mutate(values);
    } else {
      checkOutMutation.mutate(values);
    }
  };

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div>
        <Typography.Title level={3}>Manual Check</Typography.Title>
        <Typography.Text type="secondary">Create manual check-in or check-out entries for employees.</Typography.Text>
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}
      {employeesQuery.isError ? <Alert type="error" message="Failed to load employees" showIcon /> : null}

      <Card>
        <Form<ManualCheckValues> form={form} layout="vertical" initialValues={{ method: "MANUAL" }}>
          <Form.Item name="companyId" label="Company">
            <Select
              placeholder="Select company"
              loading={companiesQuery.isLoading}
              options={(companiesQuery.data ?? []).map((company) => ({ value: company.id, label: company.name }))}
              onChange={(value) => {
                setCompanyId(value);
                setBranchId(undefined);
                form.setFieldsValue({ branchId: undefined, employeeId: undefined });
              }}
            />
          </Form.Item>

          <Form.Item name="branchId" label="Branch" rules={[{ required: true, message: "Branch is required" }]}>
            <Select
              placeholder="Select branch"
              loading={branchesQuery.isLoading}
              options={(branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name }))}
              onChange={(value) => {
                setBranchId(value);
                form.setFieldValue("employeeId", undefined);
              }}
            />
          </Form.Item>

          <Form.Item name="employeeId" label="Employee" rules={[{ required: true, message: "Employee is required" }]}>
            <Select
              showSearch
              placeholder="Search employee"
              loading={employeesQuery.isLoading}
              filterOption={false}
              onSearch={setEmployeeSearch}
              options={(employeesQuery.data?.content ?? []).map((employee) => ({
                value: employee.id,
                label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
              }))}
            />
          </Form.Item>

          <Form.Item name="method" label="Method" rules={[{ required: true, message: "Method is required" }]}>
            <Select options={manualMethodOptions} loading={methodOptions.isLoading} />
          </Form.Item>

          <Form.Item name="note" label="Note">
            <Input.TextArea rows={3} placeholder="Optional note" />
          </Form.Item>

          <Space>
            <Button
              type="primary"
              icon={<LoginOutlined />}
              loading={checkInMutation.isPending}
              onClick={() => void submit("in")}
            >
              Check In
            </Button>
            <Button icon={<LogoutOutlined />} loading={checkOutMutation.isPending} onClick={() => void submit("out")}>
              Check Out
            </Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
