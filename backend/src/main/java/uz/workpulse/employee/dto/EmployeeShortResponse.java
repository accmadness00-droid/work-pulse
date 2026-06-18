package uz.workpulse.employee.dto;

import java.util.UUID;
import uz.workpulse.employee.domain.Employee;

public record EmployeeShortResponse(
        UUID id,
        String firstName,
        String lastName,
        String employeeCode,
        String position,
        boolean active
) {

    public static EmployeeShortResponse from(Employee employee) {
        return new EmployeeShortResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmployeeCode(),
                employee.getPosition(),
                employee.isActive()
        );
    }
}
