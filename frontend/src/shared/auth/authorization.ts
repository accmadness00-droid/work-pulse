export type UserRole = "SUPER_ADMIN" | "COMPANY_ADMIN" | "BRANCH_MANAGER" | "EMPLOYEE";

export type Permission =
  | "VIEW_DASHBOARD"
  | "MANAGE_COMPANIES"
  | "VIEW_COMPANY_SETTINGS"
  | "MANAGE_BRANCHES"
  | "VIEW_EMPLOYEES"
  | "CREATE_EMPLOYEES"
  | "VIEW_DEVICES"
  | "MANAGE_DEVICES"
  | "MANAGE_CREDENTIALS"
  | "VIEW_ATTENDANCE"
  | "CAMERA_ATTENDANCE"
  | "VIEW_DEVICE_EVENTS"
  | "VIEW_REPORTS"
  | "VIEW_PAYROLL";

type PermissionSubject = {
  permissions?: Permission[] | null;
};

export function hasPermission(subject: PermissionSubject | null | undefined, permission: Permission) {
  return subject?.permissions?.includes(permission) ?? false;
}

export function defaultPathForRole(role: UserRole | null | undefined) {
  return role === "EMPLOYEE" ? "/attendance/camera" : "/dashboard";
}
