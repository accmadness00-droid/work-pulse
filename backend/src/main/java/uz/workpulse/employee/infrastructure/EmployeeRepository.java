package uz.workpulse.employee.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.workpulse.employee.domain.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    Optional<Employee> findByEmployeeCodeAndActiveTrue(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, UUID id);

    Optional<Employee> findByUserIdAndActiveTrue(UUID userId);

    @Query("select e.employeeCode from Employee e where upper(e.employeeCode) like concat(:prefix, '%')")
    List<String> findEmployeeCodesByPrefix(@Param("prefix") String prefix);
}
