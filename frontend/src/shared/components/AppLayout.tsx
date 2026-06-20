import {
  ApartmentOutlined,
  BarChartOutlined,
  CalendarOutlined,
  DashboardOutlined,
  DesktopOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MenuOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined,
  VideoCameraOutlined
} from "@ant-design/icons";
import { Avatar, Button, Drawer, Dropdown, Grid, Layout, Menu, Space, Typography } from "antd";
import type { MenuProps } from "antd";
import { useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { hasPermission } from "../auth/authorization";
import type { MeResponse } from "../auth/useAuth";
import { useAuth } from "../auth/useAuth";
import { LanguageSwitcher } from "../i18n/LanguageSwitcher";
import { useTranslation } from "../i18n/I18nProvider";

const { Header, Sider, Content } = Layout;
const { useBreakpoint } = Grid;

type NavigationItem = NonNullable<MenuProps["items"]>[number];
type Translate = ReturnType<typeof useTranslation>["t"];

function buildNavigationItems(user: MeResponse | undefined, t: Translate): MenuProps["items"] {
  const dashboard: NavigationItem[] = hasPermission(user, "VIEW_DASHBOARD")
    ? [{ key: "/dashboard", icon: <DashboardOutlined />, label: t("nav.dashboard") }]
    : [];

  const organization: NavigationItem[] = [];
  if (hasPermission(user, "MANAGE_COMPANIES")) {
    organization.push({ key: "/companies", icon: <ApartmentOutlined />, label: t("nav.companies") });
  } else if (hasPermission(user, "VIEW_COMPANY_SETTINGS") && user?.companyId) {
    organization.push({
      key: `/companies/${user.companyId}/settings`,
      icon: <ApartmentOutlined />,
      label: t("nav.companySettings")
    });
  }
  if (hasPermission(user, "MANAGE_BRANCHES")) {
    organization.push({ key: "/branches", icon: <ApartmentOutlined />, label: t("nav.branches") });
  }
  if (hasPermission(user, "VIEW_EMPLOYEES")) {
    organization.push({ key: "/employees", icon: <TeamOutlined />, label: t("nav.employees") });
  }

  const operations: NavigationItem[] = [];
  if (hasPermission(user, "VIEW_ATTENDANCE")) {
    operations.push({ key: "/attendance", icon: <CalendarOutlined />, label: t("nav.attendance") });
  }
  if (hasPermission(user, "CAMERA_ATTENDANCE")) {
    operations.push({ key: "/attendance/camera", icon: <VideoCameraOutlined />, label: t("nav.cameraCheck") });
  }
  if (hasPermission(user, "VIEW_DEVICES")) {
    operations.push({ key: "/devices", icon: <DesktopOutlined />, label: t("nav.devices") });
  }
  if (hasPermission(user, "MANAGE_CREDENTIALS")) {
    operations.push({ key: "/credentials", icon: <SafetyCertificateOutlined />, label: t("nav.credentials") });
  }
  if (hasPermission(user, "VIEW_DEVICE_EVENTS")) {
    operations.push({ key: "/device-events", icon: <DesktopOutlined />, label: t("nav.deviceEvents") });
  }

  const analytics: NavigationItem[] = hasPermission(user, "VIEW_REPORTS")
    ? [{ key: "/reports", icon: <BarChartOutlined />, label: t("nav.reports") }]
    : [];

  return [
    ...dashboard,
    ...(organization.length ? [{ type: "group" as const, label: t("nav.group.organization"), children: organization }] : []),
    ...(operations.length ? [{ type: "group" as const, label: t("nav.group.operations"), children: operations }] : []),
    ...(analytics.length ? [{ type: "group" as const, label: t("nav.group.analytics"), children: analytics }] : [])
  ];
}

const pageMeta = [
  { prefix: "/companies", title: "page.companies.title", description: "page.companies.description" },
  { prefix: "/branches", title: "page.branches.title", description: "page.branches.description" },
  { prefix: "/employees", title: "page.employees.title", description: "page.employees.description" },
  { prefix: "/devices", title: "page.devices.title", description: "page.devices.description" },
  { prefix: "/credentials", title: "page.credentials.title", description: "page.credentials.description" },
  { prefix: "/attendance/camera", title: "page.cameraCheck.title", description: "page.cameraCheck.description" },
  { prefix: "/attendance", title: "page.attendance.title", description: "page.attendance.description" },
  { prefix: "/device-events", title: "page.deviceEvents.title", description: "page.deviceEvents.description" },
  { prefix: "/reports", title: "page.reports.title", description: "page.reports.description" }
] as const;

function selectedMenuKey(pathname: string, user?: MeResponse) {
  if (pathname.startsWith("/companies") && user?.role === "COMPANY_ADMIN" && user.companyId) {
    return `/companies/${user.companyId}/settings`;
  }
  if (pathname.startsWith("/attendance/camera")) {
    return "/attendance/camera";
  }
  return pageMeta.find((item) => pathname.startsWith(item.prefix))?.prefix ?? pathname;
}

function getPageMeta(pathname: string, t: Translate) {
  const meta =
    pageMeta.find((item) => pathname.startsWith(item.prefix)) ?? {
      title: "page.dashboard.title" as const,
      description: "page.dashboard.description" as const
    };

  return {
    title: t(meta.title),
    description: t(meta.description)
  };
}

function Brand({ compact = false, t }: { compact?: boolean; t: Translate }) {
  return (
    <div className={compact ? "brand compact" : "brand"}>
      <div className="brand-mark">W</div>
      {!compact ? (
        <div className="brand-copy">
          <Typography.Text className="brand-name">WorkPulse</Typography.Text>
          <Typography.Text className="brand-subtitle">{t("brand.subtitle")}</Typography.Text>
        </div>
      ) : null}
    </div>
  );
}

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const screens = useBreakpoint();
  const { logout, user } = useAuth();
  const { t } = useTranslation();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const isDesktop = screens.lg;
  const meta = getPageMeta(location.pathname, t);
  const navigationItems = useMemo(() => buildNavigationItems(user, t), [t, user]);

  const handleNavigate: MenuProps["onClick"] = ({ key }) => {
    navigate(key);
    setMobileMenuOpen(false);
  };

  const userMenu: MenuProps = {
    items: [
      {
        key: "identity",
        disabled: true,
        label: (
          <div className="user-menu-identity">
            <Typography.Text strong>{user?.email ?? t("auth.administrator")}</Typography.Text>
            <Typography.Text type="secondary">{user?.role ?? t("auth.admin")}</Typography.Text>
          </div>
        )
      },
      { type: "divider" },
      { key: "logout", icon: <LogoutOutlined />, label: t("auth.signOut"), danger: true }
    ],
    onClick: ({ key }) => {
      if (key === "logout") {
        void logout();
      }
    }
  };

  const navigation = (
    <Menu
      mode="inline"
      selectedKeys={[selectedMenuKey(location.pathname, user)]}
      onClick={handleNavigate}
      items={navigationItems}
      className="app-menu"
    />
  );

  return (
    <Layout className="app-shell">
      {isDesktop ? (
        <Sider
          width={256}
          collapsedWidth={80}
          collapsed={collapsed}
          trigger={null}
          className="app-sider"
        >
          <Brand compact={collapsed} t={t} />
          <div className="sider-navigation">{navigation}</div>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed((value) => !value)}
            className="sider-collapse"
            aria-label={collapsed ? t("layout.expandNavigation") : t("layout.collapseNavigation")}
          >
            {!collapsed ? t("layout.collapseMenu") : null}
          </Button>
        </Sider>
      ) : null}

      <Drawer
        placement="left"
        width={288}
        open={mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
        className="mobile-navigation"
        title={<Brand t={t} />}
        styles={{ body: { padding: "8px 12px 20px" } }}
      >
        {navigation}
      </Drawer>

      <Layout className="workspace-layout">
        <Header className="app-header">
          <Space size={12} className="header-leading">
            {!isDesktop ? (
              <Button
                type="text"
                icon={<MenuOutlined />}
                onClick={() => setMobileMenuOpen(true)}
                className="mobile-menu-button"
                aria-label={t("layout.openNavigation")}
              />
            ) : null}
            <div className="app-header-title">
              <Typography.Title level={4} className="page-title">
                {meta.title}
              </Typography.Title>
              <Typography.Text type="secondary" className="page-context">
                {meta.description}
              </Typography.Text>
            </div>
          </Space>

          <div className="header-actions">
            <LanguageSwitcher compact />
            <Dropdown menu={userMenu} placement="bottomRight" trigger={["click"]}>
              <Button type="text" className="user-trigger">
                <Avatar size={34} icon={<UserOutlined />} />
                <span className="user-trigger-copy">
                  <Typography.Text strong>{user?.email?.split("@")[0] ?? t("auth.administrator")}</Typography.Text>
                  <Typography.Text type="secondary">{user?.role ?? t("auth.administrator")}</Typography.Text>
                </span>
              </Button>
            </Dropdown>
          </div>
        </Header>
        <Content className="app-content">
          <div className="content-container">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
