package uz.workpulse.employee.application;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeFacade {

    Optional<UUID> findEmployeeIdByCode(String employeeCode);

    boolean existsAndActive(UUID employeeId);

    Optional<UUID> findBranchIdByEmployeeId(UUID employeeId);

    Optional<EmployeeScope> findEmployeeScope(UUID employeeId);

    Optional<UUID> findEmployeeIdByUserId(UUID userId);

    record EmployeeScope(
            UUID employeeId,
            UUID companyId,
            UUID branchId,
            UUID userId
    ) {
    }
}
