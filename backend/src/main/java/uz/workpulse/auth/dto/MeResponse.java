package uz.workpulse.auth.dto;

import java.util.UUID;
import java.util.Set;
import uz.workpulse.auth.application.PermissionCatalog;
import uz.workpulse.auth.domain.Permission;
import uz.workpulse.auth.domain.User;

public record MeResponse(
        UUID id,
        String email,
        User.Role role,
        UUID companyId,
        UUID branchId,
        UUID employeeId,
        Set<Permission> permissions
) {

    public static MeResponse from(User user) {
        return from(user, null);
    }

    public static MeResponse from(User user, UUID employeeId) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCompanyId(),
                user.getBranchId(),
                employeeId,
                PermissionCatalog.permissionsFor(user.getRole())
        );
    }
}
