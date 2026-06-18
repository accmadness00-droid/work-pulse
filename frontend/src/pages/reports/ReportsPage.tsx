import { DownloadOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Select, Space, Tabs, Typography, message } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { branchApi } from "../../features/branch/api/branchApi";
import { companyApi } from "../../features/company/api/companyApi";
import { reportApi } from "../../features/report/api/reportApi";
import { SummaryCards } from "./reportComponents";

export default function ReportsPage() {
  const navigate = useNavigate();
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null]>([dayjs().subtract(7, "day"), dayjs()]);

  const companiesQuery = useQuery({
    queryKey: ["companies"],
    queryFn: companyApi.listCompanies
  });

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

  const from = range[0]?.format("YYYY-MM-DD");
  const to = range[1]?.format("YYYY-MM-DD");

  const summaryQuery = useQuery({
    queryKey: ["reports", "summary", companyId, branchId, from, to],
    queryFn: () => reportApi.getSummary(companyId!, from!, to!, branchId),
    enabled: Boolean(companyId && from && to)
  });

  const exportMutation = useMutation({
    mutationFn: () =>
      branchId
        ? reportApi.exportExcel({ type: "BRANCH", branchId, from, to })
        : reportApi.exportExcel({ type: "DAILY", companyId, branchId, date: to }),
    onSuccess: () => message.success("Excel export downloaded"),
    onError: () => message.error("Failed to export Excel")
  });

  const dailySnapshotMutation = useMutation({
    mutationFn: () => reportApi.generateDailySnapshot(to ?? dayjs().format("YYYY-MM-DD")),
    onSuccess: () => message.success("Daily snapshot generated"),
    onError: () => message.error("Failed to generate daily snapshot")
  });

  const monthlySnapshotMutation = useMutation({
    mutationFn: () => {
      const base = range[1] ?? dayjs();
      return reportApi.generateMonthlySnapshot(base.year(), base.month() + 1);
    },
    onSuccess: () => message.success("Monthly snapshot generated"),
    onError: () => message.error("Failed to generate monthly snapshot")
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Reports</Typography.Title>
          <Typography.Text type="secondary">Summary report and export actions.</Typography.Text>
        </div>
        <Space>
          <Button icon={<ThunderboltOutlined />} loading={dailySnapshotMutation.isPending} onClick={() => dailySnapshotMutation.mutate()}>
            Generate Daily Snapshot
          </Button>
          <Button icon={<ThunderboltOutlined />} loading={monthlySnapshotMutation.isPending} onClick={() => monthlySnapshotMutation.mutate()}>
            Generate Monthly Snapshot
          </Button>
          <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
            Export Excel
          </Button>
        </Space>
      </div>

      <Card>
        <Space direction="vertical" size={16} className="page-stack">
          <Tabs
            items={[
              { key: "summary", label: "Summary" },
              { key: "daily", label: "Daily" },
              { key: "monthly", label: "Monthly" },
              { key: "employee", label: "Employee" },
              { key: "branch", label: "Branch" },
              { key: "payroll", label: "Payroll" }
            ]}
            activeKey="summary"
            onChange={(key) => key !== "summary" && navigate(`/reports/${key}`)}
          />

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
            <DatePicker.RangePicker value={range} onChange={(value) => setRange(value ?? [null, null])} />
          </div>
        </Space>
      </Card>

      {summaryQuery.isError ? <Alert type="error" message="Failed to load summary report" showIcon /> : null}

      <SummaryCards summary={summaryQuery.data} />
    </Space>
  );
}
