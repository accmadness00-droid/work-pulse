package uz.workpulse.report.infrastructure;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.report.domain.ReportSnapshot;

public interface ReportSnapshotRepository extends JpaRepository<ReportSnapshot, UUID> {

    Optional<ReportSnapshot> findByCompanyIdAndBranchIdAndPeriodTypeAndPeriodStartAndPeriodEnd(
            UUID companyId,
            UUID branchId,
            ReportSnapshot.PeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd
    );
}
