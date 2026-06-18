import { CalculatorOutlined, SettingOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Descriptions, Select, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { companyApi } from "../../features/company/api/companyApi";
import { payrollApi, PayrollEmployeeRow } from "../../features/payroll/api/payrollApi";
import { formatMinutes } from "../../features/report/api/reportApi";

function money(value?: number | null) {
  return new Intl.NumberFormat("uz-UZ", {
    maximumFractionDigits: 2
  }).format(value ?? 0);
}

function percent(value?: number | null) {
  return `${Math.round((value ?? 0) * 100)}%`;
}

function changeTag(value: number) {
  if (value > 0) {
    return <Tag color="green">+{money(value)}</Tag>;
  }
  if (value < 0) {
    return <Tag color="red">-{money(Math.abs(value))}</Tag>;
  }
  return <Tag>0</Tag>;
}

export default function PayrollPage() {
  const navigate = useNavigate();
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [month, setMonth] = useState(dayjs());

  const companiesQuery = useQuery({ queryKey: ["companies"], queryFn: companyApi.listCompanies });

  useEffect(() => {
    if (!companyId && companiesQuery.data?.length) {
      setCompanyId(companiesQuery.data[0].id);
    }
  }, [companiesQuery.data, companyId]);

  const branchesQuery = useQuery({
    queryKey: ["branches", companyId],
    queryFn: () => branchApi.listBranches(companyId!),
    enabled: Boolean(companyId)
  });

  const payrollQuery = useQuery({
    queryKey: ["payroll", "monthly", companyId, branchId, month.year(), month.month()],
    queryFn: () => payrollApi.getMonthlyPayroll(companyId!, month.year(), month.month() + 1, branchId),
    enabled: Boolean(companyId)
  });

  const columns: ColumnsType<PayrollEmployeeRow> = [
    {
      title: "Employee",
      key: "employee",
      render: (_, row) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>
            {row.firstName} {row.lastName}
          </Typography.Text>
          <Typography.Text type="secondary">{row.employeeCode}</Typography.Text>
        </Space>
      )
    },
    {
      title: "Position",
      dataIndex: "position",
      key: "position",
      render: (value?: string | null) => value || "-"
    },
    {
      title: "Base Salary",
      dataIndex: "baseSalary",
      key: "baseSalary",
      align: "right",
      render: money
    },
    {
      title: "Worked",
      dataIndex: "actualWorkMinutes",
      key: "actualWorkMinutes",
      render: formatMinutes
    },
    {
      title: "Overtime",
      dataIndex: "overtimeMinutes",
      key: "overtimeMinutes",
      render: formatMinutes
    },
    {
      title: "Attendance",
      dataIndex: "attendanceRate",
      key: "attendanceRate",
      render: (value: number) => <Tag color={value >= 0.9 ? "green" : value > 0 ? "orange" : "default"}>{percent(value)}</Tag>
    },
    {
      title: "Late",
      dataIndex: "lateMinutes",
      key: "lateMinutes",
      render: (value: number) => <Tag color={value > 0 ? "red" : "default"}>{value} min</Tag>
    },
    {
      title: "Gross",
      dataIndex: "grossPay",
      key: "grossPay",
      align: "right",
      render: money
    },
    {
      title: "Attendance Change",
      dataIndex: "attendanceAdjustmentAmount",
      key: "attendanceAdjustmentAmount",
      align: "right",
      render: changeTag
    },
    {
      title: "Auto Bonus",
      dataIndex: "automaticBonusAmount",
      key: "automaticBonusAmount",
      align: "right",
      render: money
    },
    {
      title: "Auto Penalty",
      dataIndex: "automaticPenaltyAmount",
      key: "automaticPenaltyAmount",
      align: "right",
      render: money
    },
    {
      title: "Net Pay",
      dataIndex: "netPay",
      key: "netPay",
      align: "right",
      render: (value: number) => <Typography.Text strong>{money(value)}</Typography.Text>
    },
    {
      title: "Change",
      dataIndex: "netDifferenceAmount",
      key: "netDifferenceAmount",
      align: "right",
      render: changeTag
    },
    {
      title: "Reason",
      dataIndex: "explanation",
      key: "explanation",
      ellipsis: true,
      width: 320
    }
  ];

  const summary = payrollQuery.data?.summary;
  const policy = payrollQuery.data?.policy;
  const summaryItems = [
    { label: "Employees", value: summary?.employeeCount ?? 0 },
    { label: "Base Salary", value: money(summary?.totalBaseSalary) },
    { label: "Gross Pay", value: money(summary?.totalGrossPay) },
    { label: "Bonus", value: money(summary?.totalBonusAmount) },
    { label: "Penalty", value: money(summary?.totalPenaltyAmount), danger: true },
    { label: "Net Pay", value: money(summary?.totalNetPay) },
    { label: "Total Increase", value: money(summary?.totalIncreaseAmount) },
    { label: "Total Decrease", value: money(summary?.totalDecreaseAmount), danger: true },
    { label: "Net Change", value: money(summary?.totalNetDifferenceAmount), danger: (summary?.totalNetDifferenceAmount ?? 0) < 0 },
    { label: "Expected Work", value: formatMinutes(summary?.expectedWorkMinutes ?? 0) },
    { label: "Actual Work", value: formatMinutes(summary?.actualWorkMinutes ?? 0) },
    { label: "Late", value: `${summary?.lateMinutes ?? 0} min`, danger: true }
  ];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Payroll Report</Typography.Title>
          <Typography.Text type="secondary">Monthly salary report with schedule, attendance, bonus, penalty, and change reasons.</Typography.Text>
        </div>
        <Button
          icon={<SettingOutlined />}
          disabled={!companyId}
          onClick={() => companyId && navigate(`/companies/${companyId}/settings`)}
        >
          Payroll Settings
        </Button>
      </div>

      <Card>
        <div className="filter-bar wrap">
          <Select
            placeholder="Company"
            loading={companiesQuery.isLoading}
            value={companyId}
            onChange={(value) => {
              setCompanyId(value);
              setBranchId(undefined);
            }}
            options={(companiesQuery.data ?? []).map((company) => ({ value: company.id, label: company.name }))}
            className="company-filter"
          />
          <Select
            allowClear
            placeholder="Branch"
            loading={branchesQuery.isLoading}
            value={branchId}
            onChange={setBranchId}
            options={(branchesQuery.data ?? []).map((branch) => ({ value: branch.id, label: branch.name }))}
            className="company-filter"
          />
          <DatePicker picker="month" value={month} onChange={(value) => value && setMonth(value)} />
        </div>
      </Card>

      {payrollQuery.isError ? <Alert type="error" message="Failed to load payroll calculation" showIcon /> : null}

      {policy ? (
        <Alert
          type="info"
          showIcon
          message={`Overtime bonus: ${
            policy.overtimeBonusEnabled ? `enabled (${policy.overtimeMultiplier}x)` : "disabled"
          }. Late penalty: ${policy.latePenaltyEnabled ? `enabled (${policy.latePenaltyMultiplier}x)` : "disabled"}.`}
          description="Bonus and penalty are calculated automatically from Payroll Settings. This report does not require saving."
        />
      ) : null}

      <div className="summary-grid payroll-summary-grid">
        {summaryItems.map((item) => (
          <Card key={item.label}>
            <div className="summary-label">{item.label}</div>
            <div className={item.danger ? "summary-value danger" : "summary-value"}>
              {item.label === "Net Pay" ? <CalculatorOutlined /> : null} {item.value}
            </div>
          </Card>
        ))}
      </div>

      <Table
        rowKey="employeeId"
        columns={columns}
        dataSource={payrollQuery.data?.rows ?? []}
        loading={payrollQuery.isLoading}
        pagination={{ pageSize: 10 }}
        scroll={{ x: 1800 }}
        expandable={{
          expandedRowRender: (row) => (
            <Space direction="vertical" size={12} className="full-width">
              <Descriptions size="small" bordered column={4}>
                <Descriptions.Item label="Base Salary">{money(row.baseSalary)}</Descriptions.Item>
                <Descriptions.Item label="Gross Pay">{money(row.grossPay)}</Descriptions.Item>
                <Descriptions.Item label="Net Pay">{money(row.netPay)}</Descriptions.Item>
                <Descriptions.Item label="Net Change">{changeTag(row.netDifferenceAmount)}</Descriptions.Item>
                <Descriptions.Item label="Expected Work">{formatMinutes(row.expectedWorkMinutes)}</Descriptions.Item>
                <Descriptions.Item label="Actual Work">{formatMinutes(row.actualWorkMinutes)}</Descriptions.Item>
                <Descriptions.Item label="Overtime">{formatMinutes(row.overtimeMinutes)}</Descriptions.Item>
                <Descriptions.Item label="Late">{row.lateMinutes} min</Descriptions.Item>
                <Descriptions.Item label="Attendance Change">{changeTag(row.attendanceAdjustmentAmount)}</Descriptions.Item>
                <Descriptions.Item label="Auto Bonus">{money(row.automaticBonusAmount)}</Descriptions.Item>
                <Descriptions.Item label="Total Bonus">{money(row.bonusAmount)}</Descriptions.Item>
                <Descriptions.Item label="Auto Penalty">{money(row.automaticPenaltyAmount)}</Descriptions.Item>
                <Descriptions.Item label="Total Penalty">{money(row.penaltyAmount)}</Descriptions.Item>
                <Descriptions.Item label="Worked Days">{row.workedDays}</Descriptions.Item>
              </Descriptions>
              <Alert type="info" showIcon message="Calculation reason" description={row.explanation} />
            </Space>
          )
        }}
      />
    </Space>
  );
}
