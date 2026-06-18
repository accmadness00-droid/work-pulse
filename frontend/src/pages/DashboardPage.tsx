import {
  ApartmentOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DesktopOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  WarningOutlined
} from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Col, Row, Space, Statistic, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import { dashboardApi, minutesToHours } from "../features/dashboard/api/dashboardApi";

export default function DashboardPage() {
  const navigate = useNavigate();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", "stats"],
    queryFn: dashboardApi.getDashboardStats
  });

  const stats = dashboardQuery.data;

  const cards = [
    { title: "Total Companies", value: stats?.companiesCount ?? 0, icon: <ApartmentOutlined /> },
    { title: "Total Branches", value: stats?.branchesCount ?? 0, icon: <ApartmentOutlined /> },
    { title: "Total Employees", value: stats?.employeesCount ?? 0, icon: <TeamOutlined /> },
    { title: "Active Devices", value: stats?.activeDevicesCount ?? 0, icon: <DesktopOutlined /> },
    { title: "Present Today", value: stats?.presentToday ?? 0, icon: <CheckCircleOutlined />, color: "green" },
    { title: "Late Today", value: stats?.lateToday ?? 0, icon: <ClockCircleOutlined />, color: "red" },
    { title: "Absent Today", value: stats?.absentToday ?? 0, icon: <WarningOutlined />, color: "orange" },
    { title: "Unprocessed Events", value: stats?.unprocessedEventsCount ?? 0, icon: <WarningOutlined />, color: "red" },
    { title: "Monthly Work Time", value: minutesToHours(stats?.monthlyWorkMinutes), icon: <CalendarOutlined /> },
    { title: "Monthly Late Time", value: minutesToHours(stats?.monthlyLateMinutes), icon: <ClockCircleOutlined />, color: "red" }
  ];

  return (
    <Space direction="vertical" size={24} className="dashboard-page">
      <div className="page-toolbar">
        <div>
          <Typography.Title level={3}>Overview</Typography.Title>
          <Typography.Text type="secondary">
            Today is {dayjs().format("DD MMM YYYY")}. Live MVP statistics are aggregated from existing APIs.
          </Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} loading={dashboardQuery.isFetching} onClick={() => dashboardQuery.refetch()}>
          Refresh
        </Button>
      </div>

      {dashboardQuery.isError ? <Alert type="error" message="Failed to load dashboard statistics" showIcon /> : null}
      {stats?.warnings.length ? (
        <Alert
          type="warning"
          showIcon
          message="Dashboard loaded with partial data"
          description={stats.warnings.join(". ")}
        />
      ) : null}

      <Row gutter={[16, 16]}>
        {cards.map((card) => (
          <Col xs={24} sm={12} lg={6} xl={6} key={card.title}>
            <Card loading={dashboardQuery.isLoading}>
              <Space align="start" className="metric-card">
                <div className={card.color ? `metric-icon ${card.color}` : "metric-icon"}>{card.icon}</div>
                <Statistic title={card.title} value={card.value} />
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Card title="Quick Actions">
        <Space wrap>
          <Button icon={<ApartmentOutlined />} onClick={() => navigate("/companies")}>
            Companies
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/companies/new")}>
            Add Company
          </Button>
          <Button icon={<TeamOutlined />} onClick={() => navigate("/employees")}>
            Employees
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/employees/new")}>
            Add Employee
          </Button>
          <Button icon={<CalendarOutlined />} onClick={() => navigate("/attendance/manual")}>
            Manual Check
          </Button>
          <Button icon={<DesktopOutlined />} onClick={() => navigate("/device-events")}>
            Device Events
          </Button>
          <Button icon={<BarChartOutlined />} onClick={() => navigate("/reports")}>
            Reports
          </Button>
          {stats?.companyId ? <Tag color="blue">Company scope: {stats.companyId}</Tag> : null}
        </Space>
      </Card>
    </Space>
  );
}
