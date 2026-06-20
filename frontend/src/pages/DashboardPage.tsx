import {
  ApartmentOutlined,
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DesktopOutlined,
  FieldTimeOutlined,
  PlusOutlined,
  ReloadOutlined,
  TeamOutlined,
  WarningOutlined
} from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, Col, Row, Space, Statistic, Typography } from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import { dashboardApi, minutesToHours } from "../features/dashboard/api/dashboardApi";
import { hasPermission } from "../shared/auth/authorization";
import { useAuth } from "../shared/auth/useAuth";
import { useTranslation } from "../shared/i18n/I18nProvider";

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t, language } = useTranslation();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard", "stats", user?.role, user?.companyId, user?.branchId],
    queryFn: () => dashboardApi.getDashboardStats(user)
  });

  const stats = dashboardQuery.data;

  const attendanceCards = [
    {
      title: t("dashboard.presentToday"),
      value: stats?.presentToday ?? 0,
      icon: <CheckCircleOutlined />,
      tone: "success",
      hint: t("dashboard.checkedIn")
    },
    {
      title: t("dashboard.lateToday"),
      value: stats?.lateToday ?? 0,
      icon: <ClockCircleOutlined />,
      tone: "danger",
      hint: t("dashboard.needsAttention")
    },
    {
      title: t("dashboard.absentToday"),
      value: stats?.absentToday ?? 0,
      icon: <WarningOutlined />,
      tone: "warning",
      hint: t("dashboard.noAttendance")
    },
    {
      title: t("dashboard.pendingEvents"),
      value: stats?.unprocessedEventsCount ?? 0,
      icon: <FieldTimeOutlined />,
      tone: (stats?.unprocessedEventsCount ?? 0) > 0 ? "danger" : "default",
      hint: t("dashboard.awaitingProcessing")
    }
  ];

  const organizationCards = [
    { title: t("dashboard.companies"), value: stats?.companiesCount ?? 0, icon: <ApartmentOutlined /> },
    { title: t("dashboard.branches"), value: stats?.branchesCount ?? 0, icon: <ApartmentOutlined /> },
    { title: t("dashboard.employees"), value: stats?.employeesCount ?? 0, icon: <TeamOutlined /> },
    { title: t("dashboard.activeDevices"), value: stats?.activeDevicesCount ?? 0, icon: <DesktopOutlined /> }
  ];

  return (
    <Space direction="vertical" size={24} className="dashboard-page">
      <div className="page-toolbar dashboard-toolbar">
        <div>
          <Typography.Title level={3}>{t("dashboard.goodDay")}</Typography.Title>
          <Typography.Text type="secondary">
            {t("dashboard.dateLine", { date: dayjs().locale(language === "uz" ? "uz-latn" : language).format("dddd, DD MMMM YYYY") })}
          </Typography.Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={dashboardQuery.isFetching}
          onClick={() => dashboardQuery.refetch()}
        >
          {t("dashboard.refresh")}
        </Button>
      </div>

      {dashboardQuery.isError ? <Alert type="error" message={t("dashboard.loadError")} showIcon /> : null}
      {stats?.warnings.length ? (
        <Alert
          type="warning"
          showIcon
          message={t("dashboard.partialData")}
          description={stats.warnings.join(". ")}
        />
      ) : null}

      <section>
        <div className="section-heading">
          <div>
            <Typography.Title level={5}>{t("dashboard.todayAttendance")}</Typography.Title>
            <Typography.Text type="secondary">{t("dashboard.todayAttendanceSubtitle")}</Typography.Text>
          </div>
          <Button type="link" onClick={() => navigate("/attendance")}>{t("dashboard.viewAttendance")}</Button>
        </div>
        <Row gutter={[16, 16]}>
          {attendanceCards.map((card) => (
            <Col xs={24} sm={12} xl={6} key={card.title}>
              <Card loading={dashboardQuery.isLoading} className={`metric-card metric-card-${card.tone}`}>
                <div className="metric-card-top">
                  <div className={`metric-icon ${card.tone}`}>{card.icon}</div>
                  <Typography.Text type="secondary">{card.hint}</Typography.Text>
                </div>
                <Statistic title={card.title} value={card.value} />
              </Card>
            </Col>
          ))}
        </Row>
      </section>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={16}>
          <Card
            title={t("dashboard.organizationOverview")}
            extra={<Button type="link" onClick={() => navigate("/employees")}>{t("dashboard.manageEmployees")}</Button>}
            className="dashboard-panel"
          >
            <Row gutter={[12, 20]}>
              {organizationCards.map((card) => (
                <Col xs={12} md={6} key={card.title}>
                  <div className="organization-metric">
                    <div className="metric-icon">{card.icon}</div>
                    <Statistic title={card.title} value={card.value} loading={dashboardQuery.isLoading} />
                  </div>
                </Col>
              ))}
            </Row>
            <div className="monthly-summary">
              <div>
                <Typography.Text type="secondary">{t("dashboard.monthlyWorkTime")}</Typography.Text>
                <Typography.Title level={4}>{minutesToHours(stats?.monthlyWorkMinutes)}</Typography.Title>
              </div>
              <div>
                <Typography.Text type="secondary">{t("dashboard.monthlyLateTime")}</Typography.Text>
                <Typography.Title level={4} className="danger-text">
                  {minutesToHours(stats?.monthlyLateMinutes)}
                </Typography.Title>
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card title={t("dashboard.quickActions")} className="dashboard-panel">
            <div className="quick-action-grid">
              {hasPermission(user, "CREATE_EMPLOYEES") ? (
                <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate("/employees/new")}>
                  {t("dashboard.addEmployee")}
                </Button>
              ) : null}
              <Button icon={<CalendarOutlined />} onClick={() => navigate("/attendance/manual")}>
                {t("dashboard.manualCheck")}
              </Button>
              <Button icon={<DesktopOutlined />} onClick={() => navigate("/device-events")}>
                {t("dashboard.deviceEvents")}
              </Button>
              <Button icon={<BarChartOutlined />} onClick={() => navigate("/reports")}>
                {t("dashboard.openReports")}
              </Button>
            </div>
            <div className="quick-action-footer">
              <Typography.Text type="secondary">
                {t("dashboard.quickActionsHint")}
              </Typography.Text>
            </div>
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
