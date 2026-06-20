import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { tokenStorage } from "../auth/tokenStorage";
import { useAuth } from "../auth/useAuth";
import { useTranslation } from "../i18n/I18nProvider";

type ProtectedRouteProps = {
  children: ReactNode;
};

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const location = useLocation();
  const { isLoadingUser } = useAuth();
  const { t } = useTranslation();

  if (!tokenStorage.hasAccessToken()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (isLoadingUser) {
    return <div className="route-loading">{t("app.loading")}</div>;
  }

  return children;
}
