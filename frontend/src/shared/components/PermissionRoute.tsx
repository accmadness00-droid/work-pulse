import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import type { Permission } from "../auth/authorization";
import { defaultPathForRole, hasPermission } from "../auth/authorization";
import { useAuth } from "../auth/useAuth";
import { useTranslation } from "../i18n/I18nProvider";

type PermissionRouteProps = {
  permission: Permission;
  children: ReactNode;
};

export default function PermissionRoute({ permission, children }: PermissionRouteProps) {
  const { user, isLoadingUser } = useAuth();
  const { t } = useTranslation();

  if (isLoadingUser) {
    return <div className="route-loading">{t("app.loading")}</div>;
  }

  if (!hasPermission(user, permission)) {
    return <Navigate to={defaultPathForRole(user?.role)} replace />;
  }

  return children;
}
