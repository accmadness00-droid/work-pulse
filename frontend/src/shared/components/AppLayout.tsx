import {
  ApartmentOutlined,
  BarChartOutlined,
  CalendarOutlined,
  DashboardOutlined,
  DesktopOutlined,
  LogoutOutlined,
  TeamOutlined,
  UserOutlined,
  VideoCameraOutlined
} from "@ant-design/icons";
import { Button, Layout, Menu, Space, Typography } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

const { Header, Sider, Content } = Layout;

function selectedMenuKey(pathname: string) {
  if (pathname.startsWith("/companies")) {
    return "/companies";
  }
  if (pathname.startsWith("/branches")) {
    return "/branches";
  }
  if (pathname.startsWith("/employees")) {
    return "/employees";
  }
  if (pathname.startsWith("/devices")) {
    return "/devices";
  }
  if (pathname.startsWith("/credentials")) {
    return "/credentials";
  }
  if (pathname.startsWith("/attendance/camera")) {
    return "/attendance/camera";
  }
  if (pathname.startsWith("/attendance")) {
    return "/attendance";
  }
  if (pathname.startsWith("/device-events")) {
    return "/device-events";
  }
  if (pathname.startsWith("/reports")) {
    return "/reports";
  }
  return pathname;
}

function pageTitle(pathname: string) {
  if (pathname.startsWith("/companies")) {
    return "Companies";
  }
  if (pathname.startsWith("/branches")) {
    return "Branches";
  }
  if (pathname.startsWith("/employees")) {
    return "Employees";
  }
  if (pathname.startsWith("/devices")) {
    return "Devices";
  }
  if (pathname.startsWith("/credentials")) {
    return "Credentials";
  }
  if (pathname.startsWith("/attendance/camera")) {
    return "Camera Check";
  }
  if (pathname.startsWith("/attendance")) {
    return "Attendance";
  }
  if (pathname.startsWith("/device-events")) {
    return "Device Events";
  }
  if (pathname.startsWith("/reports")) {
    return "Reports";
  }
  return "Dashboard";
}

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout, user } = useAuth();

  return (
    <Layout className="app-shell">
      <Sider width={248} className="app-sider">
        <div className="brand">
          <div className="brand-mark">WP</div>
          <div>
            <Typography.Text className="brand-name">WorkPulse</Typography.Text>
            <Typography.Text className="brand-subtitle">Admin Panel</Typography.Text>
          </div>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[selectedMenuKey(location.pathname)]}
          onClick={({ key }) => navigate(key)}
          items={[
            { key: "/dashboard", icon: <DashboardOutlined />, label: "Dashboard" },
            { key: "/companies", icon: <ApartmentOutlined />, label: "Companies" },
            { key: "/branches", icon: <ApartmentOutlined />, label: "Branches" },
            { key: "/employees", icon: <TeamOutlined />, label: "Employees" },
            { key: "/devices", icon: <DesktopOutlined />, label: "Devices" },
            { key: "/credentials", icon: <UserOutlined />, label: "Credentials" },
            { key: "/attendance", icon: <CalendarOutlined />, label: "Attendance" },
            { key: "/attendance/camera", icon: <VideoCameraOutlined />, label: "Camera Check" },
            { key: "/device-events", icon: <DesktopOutlined />, label: "Device Events" },
            { key: "/reports", icon: <BarChartOutlined />, label: "Reports" }
          ]}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <div className="app-header-title">
            <Typography.Title level={4} className="page-title">
              {pageTitle(location.pathname)}
            </Typography.Title>
            <Typography.Text type="secondary">MVP admin workspace</Typography.Text>
          </div>
          <Space>
            <Space className="user-chip">
              <UserOutlined />
              <span>{user?.email ?? "Admin"}</span>
            </Space>
            <Button icon={<LogoutOutlined />} onClick={logout}>
              Logout
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
