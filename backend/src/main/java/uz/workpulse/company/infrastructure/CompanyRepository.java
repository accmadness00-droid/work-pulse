package uz.workpulse.company.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uz.workpulse.company.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findAllByActiveTrue();

    boolean existsByInn(String inn);

    boolean existsByInnAndIdNot(String inn, UUID id);
}
