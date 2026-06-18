package uz.workpulse.payroll.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.payroll.domain.PayrollAdjustment;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {

    List<PayrollAdjustment> findByYearAndMonthAndEmployeeIdIn(int year, int month, List<UUID> employeeIds);

    Optional<PayrollAdjustment> findByEmployeeIdAndYearAndMonth(UUID employeeId, int year, int month);
}
