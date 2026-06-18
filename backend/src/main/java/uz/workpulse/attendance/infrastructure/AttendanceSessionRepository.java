package uz.workpulse.attendance.infrastructure;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uz.workpulse.attendance.domain.AttendanceSession;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID>, JpaSpecificationExecutor<AttendanceSession> {

    Optional<AttendanceSession> findByEmployeeIdAndCheckOutTimeIsNull(UUID employeeId);

    boolean existsByEmployeeIdAndCheckOutTimeIsNull(UUID employeeId);

    java.util.List<AttendanceSession> findAllByEmployeeIdAndDateBetweenOrderByDateDesc(UUID employeeId, LocalDate from, LocalDate to);
}
