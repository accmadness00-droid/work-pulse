package uz.workpulse.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.employee.domain.Employee;

public record UpdateEmployeeRequest(
        UUID userId,
        @NotNull UUID companyId,
        @NotNull UUID branchId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String middleName,
        String phone,
        String photoUrl,
        String position,
        @NotBlank String employeeCode,
        LocalDate hiredDate,
        LocalDate birthDate,
        Employee.EmploymentType employmentType,
        BigDecimal salary
) {
}
