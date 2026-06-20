import { CheckCircleFilled, LockOutlined, MailOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { Navigate } from "react-router-dom";
import { tokenStorage } from "../shared/auth/tokenStorage";
import { useAuth } from "../shared/auth/useAuth";
import { LanguageSwitcher } from "../shared/i18n/LanguageSwitcher";
import { useTranslation } from "../shared/i18n/I18nProvider";

type LoginFormValues = {
  email: string;
  password: string;
};

export default function LoginPage() {
  const { login, isLoggingIn, loginError, homePath, isLoadingUser } = useAuth();
  const { t } = useTranslation();

  if (tokenStorage.hasAccessToken()) {
    if (isLoadingUser) {
      return null;
    }
    return <Navigate to={homePath} replace />;
  }

  const errorMessage =
    loginError?.response?.data?.message ?? loginError?.message ?? t("login.errorFallback");

  return (
    <div className="login-page">
      <section className="login-brand-panel">
        <div className="login-brand">
          <div className="brand-mark login-brand-mark">W</div>
          <Typography.Text>WorkPulse</Typography.Text>
        </div>
        <div className="login-brand-content">
          <Typography.Title>{t("login.heroTitle")}</Typography.Title>
          <Typography.Paragraph>
            {t("login.heroDescription")}
          </Typography.Paragraph>
          <div className="login-benefits">
            <span><CheckCircleFilled /> {t("login.benefit.attendance")}</span>
            <span><CheckCircleFilled /> {t("login.benefit.devices")}</span>
            <span><CheckCircleFilled /> {t("login.benefit.reports")}</span>
          </div>
        </div>
        <Typography.Text className="login-brand-footer">{t("login.footer")}</Typography.Text>
      </section>

      <main className="login-form-panel">
        <div className="login-language">
          <LanguageSwitcher />
        </div>
        <Card className="login-card">
          <div className="login-heading">
            <Typography.Title level={2}>{t("login.welcome")}</Typography.Title>
            <Typography.Text type="secondary">{t("login.subtitle")}</Typography.Text>
          </div>

          {loginError ? <Alert type="error" message={errorMessage} showIcon className="login-alert" /> : null}

          <Form<LoginFormValues> layout="vertical" requiredMark={false} onFinish={(values) => login(values)}>
            <Form.Item
              name="email"
              label={t("login.email")}
              rules={[
                { required: true, message: t("login.emailRequired") },
                { type: "email", message: t("login.emailInvalid") }
              ]}
            >
              <Input
                prefix={<MailOutlined />}
                placeholder="name@company.com"
                size="large"
                autoComplete="email"
                autoFocus
              />
            </Form.Item>

            <Form.Item
              name="password"
              label={t("login.password")}
              rules={[{ required: true, message: t("login.passwordRequired") }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder={t("login.passwordPlaceholder")}
                size="large"
                autoComplete="current-password"
              />
            </Form.Item>

            <Button type="primary" htmlType="submit" size="large" loading={isLoggingIn} block>
              {t("login.submit")}
            </Button>
          </Form>
          <Typography.Text type="secondary" className="login-support">
            {t("login.support")}
          </Typography.Text>
        </Card>
      </main>
    </div>
  );
}
