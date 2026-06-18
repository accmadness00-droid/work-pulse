package uz.workpulse.branch.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateBranchRequest(
        @NotBlank String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer geofenceRadiusMeters
) {
}
