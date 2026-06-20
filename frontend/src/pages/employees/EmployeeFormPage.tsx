import { ReloadOutlined, SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Switch,
  Typography,
  message
} from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { useLookupOptions } from "../../shared/hooks/useLookups";
import { PHONE_NUMBER_PLACEHOLDER, phoneNumberRules } from "../../shared/validation/phoneNumber";
import {
  CreateEmployeeRequest,
  EmployeeResponse,
  EmploymentType,
  UpdateEmployeeRequest,
  employeeApi
} from "../../features/employee/api/employeeApi";

type EmployeeFormValues = {
  companyId: string;
  branchId: string;
  firstName: string;
  lastName: string;
  middleName?: string;
  employeeCode: string;
  position?: string;
  phone?: string;
  email?: string;
  password?: string;
  hiredDate?: Dayjs;
  employmentType?: EmploymentType;
  salary?: number;
  active?: boolean;
};

function cleanText(value?: string) {
  const next = value?.trim();
  return next ? next : undefined;
}

function toCreateRequest(values: EmployeeFormValues): CreateEmployeeRequest {
  return {
    companyId: values.companyId,
    branchId: values.branchId,
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    middleName: cleanText(values.middleName),
    employeeCode: values.employeeCode.trim(),
    position: cleanText(values.position),
    phone: cleanText(values.phone),
    email: cleanText(values.email),
    password: cleanText(values.password),
    hiredDate: values.hiredDate?.format("YYYY-MM-DD"),
    employmentType: values.employmentType ?? "FULL_TIME",
    salary: values.salary,
    active: values.active
  };
}

function toUpdateRequest(values: EmployeeFormValues): UpdateEmployeeRequest {
  return {
    companyId: values.companyId,
    branchId: values.branchId,
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    middleName: cleanText(values.middleName),
    employeeCode: values.employeeCode.trim(),
    position: cleanText(values.position),
    phone: cleanText(values.phone),
    hiredDate: values.hiredDate?.format("YYYY-MM-DD"),
    employmentType: values.employmentType ?? "FULL_TIME",
    salary: values.salary
  };
}

export default function EmployeeFormPage() {
  const [form] = Form.useForm<EmployeeFormValues>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const isEdit = Boolean(id);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>();

  const companiesQuery = useAccessibleCompanies();
  const employmentTypeOptions = useLookupOptions("employeeEmploymentTypes");

  const employeeQuery = useQuery({
    queryKey: ["employees", "detail", id],
    queryFn: () => employeeApi.getEmployee(id!),
    enabled: isEdit
  });

  const generatedCodeQuery = useQuery({
    queryKey: ["employees", "next-code"],
    queryFn: () => employeeApi.generateEmployeeCode(),
    enabled: !isEdit,
    staleTime: 0
  });

  useEffect(() => {
    if (!isEdit) {
      const initialCompanyId = searchParams.get("companyId") ?? companiesQuery.data?.[0]?.id;
      if (initialCompanyId) {
        setSelectedCompanyId(initialCompanyId);
        form.setFieldValue("companyId", initialCompanyId);
      }
    }
  }, [companiesQuery.data, form, isEdit, searchParams]);

  useEffect(() => {
    if (employeeQuery.data) {
      const hiredDate = employeeQuery.data.hiredDate ?? employeeQuery.data.hireDate;
      setSelectedCompanyId(employeeQuery.data.companyId);
      form.setFieldsValue({
        companyId: employeeQuery.data.companyId,
        branchId: employeeQuery.data.branchId,
        firstName: employeeQuery.data.firstName,
        lastName: employeeQuery.data.lastName,
        middleName: employeeQuery.data.middleName ?? undefined,
        employeeCode: employeeQuery.data.employeeCode,
        position: employeeQuery.data.position ?? undefined,
        phone: employeeQuery.data.phone ?? undefined,
        email: employeeQuery.data.email ?? undefined,
        hiredDate: hiredDate ? dayjs(hiredDate) : undefined,
        employmentType: employeeQuery.data.employmentType ?? "FULL_TIME",
        salary: employeeQuery.data.salary ?? undefined,
        active: employeeQuery.data.active
      });
    }
  }, [employeeQuery.data, form]);

  useEffect(() => {
    if (!isEdit && generatedCodeQuery.data?.employeeCode && !form.getFieldValue("employeeCode")) {
      form.setFieldValue("employeeCode", generatedCodeQuery.data.employeeCode);
    }
  }, [form, generatedCodeQuery.data?.employeeCode, isEdit]);

  const branchesQuery = useQuery({
    queryKey: ["branches", selectedCompanyId],
    queryFn: () => branchApi.listBranches(selectedCompanyId!),
    enabled: Boolean(selectedCompanyId)
  });

  const submitMutation = useMutation({
    mutationFn: async (values: EmployeeFormValues) => {
      let employee: EmployeeResponse;
      if (isEdit) {
        employee = await employeeApi.updateEmployee(id!, toUpdateRequest(values));
      } else {
        employee = await employeeApi.createEmployee(toCreateRequest(values));
      }

      if (values.active !== undefined && values.active !== employee.active) {
        employee = values.active
          ? await employeeApi.activateEmployee(employee.id)
          : await employeeApi.deactivateEmployee(employee.id);
      }

      return employee;
    },
    onSuccess: () => {
      message.success(isEdit ? "Employee updated" : "Employee created");
      queryClient.invalidateQueries({ queryKey: ["employees"] });
      navigate("/employees");
    },
    onError: () => {
      message.error(isEdit ? "Failed to update employee" : "Failed to create employee");
    }
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
        <Typography.Title level={3}>{isEdit ? "Edit Employee Profile" : "Create Employee"}</Typography.Title>
        <Typography.Text type="secondary">
          {isEdit
            ? "Update master profile data only. Schedule, Face ID, credentials, and attendance are managed from the employee profile tabs."
            : "Create the employee account and core profile. Extra setup is available after saving."}
        </Typography.Text>
      </div>

      {companiesQuery.isError ? <Alert type="error" message="Failed to load companies" showIcon /> : null}
      {branchesQuery.isError ? <Alert type="error" message="Failed to load branches" showIcon /> : null}
      {employeeQuery.isError ? <Alert type="error" message="Failed to load employee" showIcon /> : null}
      {generatedCodeQuery.isError && !isEdit ? (
        <Alert type="warning" message="Employee number could not be generated. You can enter it manually." showIcon />
      ) : null}

      <Card loading={(employeeQuery.isLoading && isEdit) || companiesQuery.isLoading}>
        <Form<EmployeeFormValues>
          form={form}
          layout="vertical"
          initialValues={{ employmentType: "FULL_TIME", active: true }}
          onFinish={(values) => submitMutation.mutate(values)}
        >
          <div className="form-grid two">
            <Form.Item name="companyId" label="Company" rules={[{ required: true, message: "Company is required" }]}>
              <Select
                placeholder="Select company"
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
            <Form.Item name="firstName" label="First name" rules={[{ required: true, message: "First name is required" }]}>
              <Input placeholder="Ali" />
            </Form.Item>

            <Form.Item name="lastName" label="Last name" rules={[{ required: true, message: "Last name is required" }]}>
              <Input placeholder="Valiyev" />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="middleName" label="Middle name">
              <Input placeholder="Optional" />
            </Form.Item>

            <Form.Item
              name="employeeCode"
              label="Employee number"
              rules={[{ required: true, message: "Employee number is required" }]}
            >
              <Input
                placeholder="EMP0001"
                disabled={!isEdit && generatedCodeQuery.isLoading}
                addonAfter={
                  isEdit ? null : (
                    <Button
                      type="text"
                      size="small"
                      icon={<ReloadOutlined />}
                      loading={generatedCodeQuery.isFetching}
                      onClick={async () => {
                        const result = await generatedCodeQuery.refetch();
                        if (result.data?.employeeCode) {
                          form.setFieldValue("employeeCode", result.data.employeeCode);
                        }
                      }}
                    >
                      Generate
                    </Button>
                  )
                }
              />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="position" label="Position">
              <Input placeholder="Engineer" />
            </Form.Item>

            <Form.Item name="phone" label="Phone" rules={phoneNumberRules()}>
              <Input
                placeholder={PHONE_NUMBER_PLACEHOLDER}
                inputMode="tel"
                autoComplete="tel"
                maxLength={16}
              />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item
              name="email"
              label="Login email"
              rules={[
                { required: !isEdit, message: "Login email is required" },
                { type: "email", message: "Enter a valid email" }
              ]}
            >
              <Input placeholder="employee@workpulse.uz" disabled={isEdit} />
            </Form.Item>

            {!isEdit ? (
              <Form.Item
                name="password"
                label="Login password"
                rules={[
                  { required: true, message: "Login password is required" },
                  { min: 6, message: "Password must be at least 6 characters" }
                ]}
              >
                <Input.Password placeholder="Temporary password" autoComplete="new-password" />
              </Form.Item>
            ) : (
              <Form.Item name="hiredDate" label="Hire date">
                <DatePicker className="full-width" />
              </Form.Item>
            )}
          </div>

          {!isEdit ? (
            <div className="form-grid two">
              <Form.Item name="hiredDate" label="Hire date">
                <DatePicker className="full-width" />
              </Form.Item>
            </div>
          ) : null}

          {isEdit ? null : (
            <Alert
              className="modal-alert"
              type="info"
              showIcon
              message="Employee will use this email and password to sign in from the camera check page."
            />
          )}

          <div className="form-grid two">
            <Form.Item name="employmentType" label="Employment type">
              <Select options={employmentTypeOptions.options} loading={employmentTypeOptions.isLoading} />
            </Form.Item>

            <Form.Item name="salary" label="Salary">
              <InputNumber min={0} step={100000} className="full-width" placeholder="5000000" />
            </Form.Item>
          </div>

          <div className="form-grid two">
            <Form.Item name="active" label="Active" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>

          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={submitMutation.isPending}>
              Save
            </Button>
            <Button onClick={() => navigate("/employees")}>Cancel</Button>
          </Space>
        </Form>
      </Card>
    </Space>
  );
}
