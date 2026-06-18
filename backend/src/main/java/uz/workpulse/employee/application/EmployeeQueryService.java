package uz.workpulse.employee.application;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.dto.EmployeeFilterRequest;
import uz.workpulse.employee.infrastructure.EmployeeRepository;

@Service
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;

    public EmployeeQueryService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> findEmployees(EmployeeFilterRequest filter, Pageable pageable) {
        return employeeRepository.findAll(toSpecification(filter), pageable);
    }

    private Specification<Employee> toSpecification(EmployeeFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.companyId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("companyId"), filter.companyId()));
            }
            if (filter.branchId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("branchId"), filter.branchId()));
            }
            if (StringUtils.hasText(filter.position())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("position")),
                        filter.position().trim().toLowerCase()
                ));
            }
            if (filter.active() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), filter.active()));
            }
            if (StringUtils.hasText(filter.search())) {
                String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeCode")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
