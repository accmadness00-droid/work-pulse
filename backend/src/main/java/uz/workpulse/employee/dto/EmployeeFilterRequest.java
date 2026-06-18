package uz.workpulse.employee.dto;

import java.util.UUID;

public record EmployeeFilterRequest(
        UUID companyId,
        UUID branchId,
        String position,
        Boolean active,
        String search
) {
}
