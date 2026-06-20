import { DownloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Select, Space, Typography, message } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useEffect, useState } from "react";
import { branchApi } from "../../features/branch/api/branchApi";
import { reportApi } from "../../features/report/api/reportApi";
import { useAccessibleCompanies } from "../../shared/hooks/useAccessibleCompanies";
import { ReportNavigation, ReportSessionTable, SummaryCards } from "./reportComponents";

export default function BranchReportPage() {
  const [companyId, setCompanyId] = useState<string>();
  const [branchId, setBranchId] = useState<string>();
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null]>([dayjs().startOf("month"), dayjs()]);

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

  const from = range[0]?.format("YYYY-MM-DD");
  const to = range[1]?.format("YYYY-MM-DD");

  const reportQuery = useQuery({
    queryKey: ["reports", "branch", branchId, from, to],
    queryFn: () => reportApi.getBranchReport(branchId!, from, to),
    enabled: Boolean(branchId)
  });

  const exportMutation = useMutation({
    mutationFn: () => reportApi.exportExcel({ type: "BRANCH", branchId, from, to }),
    onSuccess: () => message.success("Branch Excel export downloaded"),
    onError: () => message.error("Failed to export branch report")
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Branch Report</Typography.Title>
          <Typography.Text type="secondary">Branch attendance sessions and totals.</Typography.Text>
        </div>
        <Button icon={<DownloadOutlined />} disabled={!branchId} loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
          Export Branch Excel
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
          <DatePicker.RangePicker value={range} onChange={(value) => setRange(value ?? [null, null])} />
        </div>
      </Card>

      {reportQuery.isError ? <Alert type="error" message="Failed to load branch report" showIcon /> : null}

      <SummaryCards summary={reportQuery.data?.summary} />
      <ReportSessionTable rows={reportQuery.data?.sessions ?? []} loading={reportQuery.isLoading} />
    </Space>
  );
}
