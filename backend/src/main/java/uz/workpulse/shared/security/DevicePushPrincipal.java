package uz.workpulse.shared.security;

import java.util.UUID;

public record DevicePushPrincipal(
        UUID deviceId,
        UUID branchId,
        String serialNumber
) {
}
