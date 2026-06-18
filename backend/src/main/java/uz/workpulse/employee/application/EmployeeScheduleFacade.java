package uz.workpulse.employee.application;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import uz.workpulse.employee.dto.EmployeeScheduleInfo;

public interface EmployeeScheduleFacade {

    Optional<EmployeeScheduleInfo> getScheduleForDate(UUID employeeId, LocalDate date);
}
