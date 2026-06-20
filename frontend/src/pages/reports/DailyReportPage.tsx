import { DownloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Select, Space, Typography, message } from "antd";
import dayjs from "dayjs";
import { useEffect, useState } from "react";
import { branchApi } from "../../features/branch/api/branchApi";
import { reportApi } from "../../features/report/api/reportApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { ReportNavigation, ReportSessionTable, SummaryCards } from "./reportComponents";

export default function DailyReportPage() {
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [date, setDate] = useState(dayjs());

  const companiesQuery = useAccessibleCompanies();

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

  const reportQuery = useQuery({
    queryKey: ["reports", "daily", companyId, branchId, date.format("YYYY-MM-DD")],
    queryFn: () => reportApi.getDailyReport(companyId!, date.format("YYYY-MM-DD"), branchId),
    enabled: Boolean(companyId)
  });

  const exportMutation = useMutation({
    mutationFn: () => reportApi.exportExcel({ type: "DAILY", companyId, branchId, date: date.format("YYYY-MM-DD") }),
    onSuccess: () => message.success("Daily Excel export downloaded"),
    onError: () => message.error("Failed to export daily report")
  });

  const rows = reportQuery.data?.sessions ?? reportQuery.data?.rows ?? [];

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Daily Report</Typography.Title>
          <Typography.Text type="secondary">Daily attendance summary and export.</Typography.Text>
        </div>
        <Button icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
          Export Daily Excel
        </Button>
      </div>

      <ReportNavigation />

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
          <DatePicker value={date} onChange={(value) => value && setDate(value)} />
        </div>
      </Card>

      {reportQuery.isError ? <Alert type="error" message="Failed to load daily report" showIcon /> : null}

      <SummaryCards summary={reportQuery.data?.summary} />
      <ReportSessionTable rows={rows} loading={reportQuery.isLoading} />
    </Space>
  );
}
