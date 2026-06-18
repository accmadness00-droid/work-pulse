package uz.workpulse.shared.security;

import java.util.UUID;
import org.springframework.stereotype.Service;
import uz.workpulse.auth.domain.User;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class AccessControlService {

    public void requireSuperAdmin(AuthPrincipal principal) {
        if (principal.role() != User.Role.SUPER_ADMIN) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void requireCompanyAccess(AuthPrincipal principal, UUID companyId) {
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN && companyId != null && companyId.equals(principal.companyId())) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    public void requireBranchAccess(AuthPrincipal principal, UUID companyId, UUID branchId) {
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN && companyId != null && companyId.equals(principal.companyId())) {
            return;
        }
        if (principal.role() == User.Role.BRANCH_MANAGER
                && branchId != null
                && branchId.equals(principal.branchId())
                && (companyId == null || companyId.equals(principal.companyId()))) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    public void requireEmployeeSelfOrAdmin(AuthPrincipal principal, UUID employeeUserId, UUID companyId, UUID branchId) {
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN && companyId != null && companyId.equals(principal.companyId())) {
            return;
        }
        if (principal.role() == User.Role.BRANCH_MANAGER
                && branchId != null
                && branchId.equals(principal.branchId())) {
            return;
        }
        if (principal.role() == User.Role.EMPLOYEE && employeeUserId != null && employeeUserId.equals(principal.userId())) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    public boolean isDevicePush() {
        return SecurityContextHelper.currentAuthentication()
                .map(auth -> auth.getPrincipal() instanceof DevicePushPrincipal)
                .orElse(false);
    }

    public DevicePushPrincipal currentDevicePush() {
        return SecurityContextHelper.currentAuthentication()
                .map(auth -> auth.getPrincipal())
                .filter(DevicePushPrincipal.class::isInstance)
                .map(DevicePushPrincipal.class::cast)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
    }

    public AuthPrincipal currentUser() {
        return SecurityContextHelper.currentAuthentication()
                .map(auth -> auth.getPrincipal())
                .filter(AuthPrincipal.class::isInstance)
                .map(AuthPrincipal.class::cast)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_JWT));
    }
}
