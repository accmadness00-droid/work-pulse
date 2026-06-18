import { LockOutlined, MailOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { Navigate } from "react-router-dom";
import { tokenStorage } from "../shared/auth/tokenStorage";
import { useAuth } from "../shared/auth/useAuth";

type LoginFormValues = {
  email: string;
  password: string;
};

export default function LoginPage() {
  const { login, isLoggingIn, loginError, homePath, isLoadingUser } = useAuth();

  if (tokenStorage.hasAccessToken()) {
    if (isLoadingUser) {
      return null;
    }
    return <Navigate to={homePath} replace />;
  }

  const errorMessage =
    loginError?.response?.data?.message ?? loginError?.message ?? "Login failed. Please try again.";

  return (
    <div className="login-page">
      <Card className="login-card">
        <div className="login-heading">
          <Typography.Title level={2}>WorkPulse</Typography.Title>
          <Typography.Text type="secondary">Sign in to the admin panel</Typography.Text>
        </div>

        {loginError ? <Alert type="error" message={errorMessage} showIcon className="login-alert" /> : null}

        <Form<LoginFormValues>
          layout="vertical"
          initialValues={{ email: "admin@workpulse.uz", password: "admin123" }}
          onFinish={(values) => login(values)}
        >
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: "Email is required" },
              { type: "email", message: "Enter a valid email" }
            ]}
          >
            <Input prefix={<MailOutlined />} placeholder="admin@workpulse.uz" size="large" />
          </Form.Item>

          <Form.Item name="password" label="Password" rules={[{ required: true, message: "Password is required" }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="admin123" size="large" />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" loading={isLoggingIn} block>
            Sign in
          </Button>
        </Form>
      </Card>
    </div>
  );
}
