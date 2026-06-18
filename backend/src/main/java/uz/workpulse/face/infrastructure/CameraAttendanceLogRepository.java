package uz.workpulse.face.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.face.domain.CameraAttendanceLog;

public interface CameraAttendanceLogRepository extends JpaRepository<CameraAttendanceLog, UUID> {
}
