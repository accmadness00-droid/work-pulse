package uz.workpulse.employee.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.employee.domain.EmployeeSchedule;

public interface EmployeeScheduleRepository extends JpaRepository<EmployeeSchedule, UUID> {

    List<EmployeeSchedule> findAllByEmployeeIdOrderByDayOfWeek(UUID employeeId);

    Optional<EmployeeSchedule> findByEmployeeIdAndDayOfWeek(UUID employeeId, short dayOfWeek);
}
