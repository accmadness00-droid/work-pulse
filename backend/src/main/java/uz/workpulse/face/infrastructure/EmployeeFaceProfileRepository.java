package uz.workpulse.face.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.face.domain.EmployeeFaceProfile;

public interface EmployeeFaceProfileRepository extends JpaRepository<EmployeeFaceProfile, UUID> {
    List<EmployeeFaceProfile> findByActiveTrue();
    List<EmployeeFaceProfile> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
