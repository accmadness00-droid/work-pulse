package uz.workpulse.branch.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.workpulse.branch.domain.Branch;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByCompanyIdAndActiveTrue(UUID companyId);

    @Query("select b.id from Branch b where b.companyId = :companyId and b.active = true")
    List<UUID> findIdsByCompanyId(@Param("companyId") UUID companyId);
}
