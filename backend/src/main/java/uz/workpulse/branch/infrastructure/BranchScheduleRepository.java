package uz.workpulse.branch.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.branch.domain.BranchSchedule;

public interface BranchScheduleRepository extends JpaRepository<BranchSchedule, UUID> {

    List<BranchSchedule> findAllByBranchIdOrderByDayOfWeek(UUID branchId);

    Optional<BranchSchedule> findByBranchIdAndDayOfWeek(UUID branchId, short dayOfWeek);
}
