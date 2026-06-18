package uz.workpulse.shared.security;

import java.util.UUID;
import uz.workpulse.auth.domain.User;

public record AuthPrincipal(
        UUID userId,
        String email,
        User.Role role,
        UUID companyId,
        UUID branchId
) {

    public AuthPrincipal(UUID userId, String email, User.Role role) {
        this(userId, email, role, null, null);
    }
}
