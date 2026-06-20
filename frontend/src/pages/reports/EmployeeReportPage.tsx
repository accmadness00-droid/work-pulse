import { DownloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DatePicker, Select, Space, Typography, message } from "antd";
import dayjs, { Dayjs } from "dayjs";
import { useState } from "react";
import { employeeApi } from "../../features/employee/api/employeeApi";
import { reportApi } from "../../features/report/api/reportApi";
import { ReportNavigation, ReportSessionTable, SummaryCards } from "./reportComponents";

export default function EmployeeReportPage() {
  const [employeeSearch, setEmployeeSearch] = useState("");
  const [employeeId, setEmployeeId] = useState<string>();
  const [range, setRange] = useState<[Dayjs | null, Dayjs | null]>([dayjs().startOf("month"), dayjs()]);

  const employeesQuery = useQuery({
    queryKey: ["employees", "report-select", employeeSearch],
    queryFn: () => employeeApi.listEmployees({ search: employeeSearch, active: true, page: 0, size: 50 })
  });

  const from = range[0]?.format("YYYY-MM-DD");
  const to = range[1]?.format("YYYY-MM-DD");

  const reportQuery = useQuery({
    queryKey: ["reports", "employee", employeeId, from, to],
    queryFn: () => reportApi.getEmployeeReport(employeeId!, from, to),
    enabled: Boolean(employeeId)
  });

  const exportMutation = useMutation({
    mutationFn: () => reportApi.exportExcel({ type: "EMPLOYEE", employeeId, from, to }),
    onSuccess: () => message.success("Employee Excel export downloaded"),
    onError: () => message.error("Failed to export employee report")
  });

  return (
    <Space direction="vertical" size={16} className="page-stack">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Employee Report</Typography.Title>
          <Typography.Text type="secondary">One employee's attendance sessions and totals.</Typography.Text>
        </div>
        <Button icon={<DownloadOutlined />} disabled={!employeeId} loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
          Export Employee Excel
        </Button>
      </div>

      <ReportNavigation />

      <Card>
        <div className="filter-bar wrap">
          <Select
            showSearch
            allowClear
            placeholder="Employee"
            value={employeeId}
            filterOption={false}
            onSearch={setEmployeeSearch}
            onChange={setEmployeeId}
            loading={employeesQuery.isLoading}
            options={(employeesQuery.data?.content ?? []).map((employee) => ({
              value: employee.id,
              label: `${employee.firstName} ${employee.lastName} (${employee.employeeCode})`
            }))}
            className="company-filter"
          />
          <DatePicker.RangePicker value={range} onChange={(value) => setRange(value ?? [null, null])} />
        </div>
      </Card>

      {reportQuery.isError ? <Alert type="error" message="Failed to load employee report" showIcon /> : null}

      <SummaryCards summary={reportQuery.data?.summary} />
      <ReportSessionTable rows={reportQuery.data?.sessions ?? []} loading={reportQuery.isLoading} />
    </Space>
  );
}
