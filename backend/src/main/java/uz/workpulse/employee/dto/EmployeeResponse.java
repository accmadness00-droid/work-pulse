package uz.workpulse.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.employee.domain.Employee;

public record EmployeeResponse(
        UUID id,
        UUID userId,
        String email,
        UUID companyId,
        UUID branchId,
        String firstName,
        String lastName,
        String middleName,
        String phone,
        String photoUrl,
        String position,
        String employeeCode,
        LocalDate hiredDate,
        LocalDate birthDate,
        Employee.EmploymentType employmentType,
        BigDecimal salary,
        boolean active
) {

    public static EmployeeResponse from(Employee employee) {
        return from(employee, null);
    }

    public static EmployeeResponse from(Employee employee, String email) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUserId(),
                email,
                employee.getCompanyId(),
                employee.getBranchId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getMiddleName(),
                employee.getPhone(),
                employee.getPhotoUrl(),
                employee.getPosition(),
                employee.getEmployeeCode(),
                employee.getHiredDate(),
                employee.getBirthDate(),
                employee.getEmploymentType(),
                employee.getSalary(),
                employee.isActive()
        );
    }
}
