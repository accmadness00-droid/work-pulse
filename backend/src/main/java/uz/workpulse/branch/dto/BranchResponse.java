package uz.workpulse.branch.dto;

import java.math.BigDecimal;
import java.util.UUID;
import uz.workpulse.branch.domain.Branch;

public record BranchResponse(
        UUID id,
        UUID companyId,
        String name,
        String address,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer geofenceRadiusMeters,
        boolean active
) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getCompanyId(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone(),
                branch.getLatitude(),
                branch.getLongitude(),
                branch.getGeofenceRadiusMeters(),
                branch.isActive()
        );
    }
}
