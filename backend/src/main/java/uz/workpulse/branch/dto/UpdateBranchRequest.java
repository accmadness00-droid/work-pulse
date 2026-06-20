package uz.workpulse.branch.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import uz.workpulse.shared.validation.ValidPhoneNumber;

public record UpdateBranchRequest(
        @NotBlank String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer geofenceRadiusMeters,
        @ValidPhoneNumber String phone
) {
}
