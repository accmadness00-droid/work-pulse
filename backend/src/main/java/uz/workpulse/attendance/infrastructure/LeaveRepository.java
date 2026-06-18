package uz.workpulse.attendance.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.attendance.domain.Leave;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {
}
