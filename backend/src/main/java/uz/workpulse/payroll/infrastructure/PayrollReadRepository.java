package uz.workpulse.payroll.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import uz.workpulse.employee.domain.Employee;

public interface PayrollReadRepository extends Repository<Employee, UUID> {

    @Query(value = """
            SELECT
                e.id AS "employeeId",
                e.branch_id AS "branchId",
                e.employee_code AS "employeeCode",
                e.first_name AS "firstName",
                e.last_name AS "lastName",
                e.position AS "position",
                COALESCE(e.salary, 0) AS "baseSalary",
                COALESCE(SUM(a.work_minutes), 0) AS "actualWorkMinutes",
                COALESCE(SUM(a.late_minutes), 0) AS "lateMinutes",
                COUNT(DISTINCT CASE WHEN a.work_minutes > 0 THEN a.date END) AS "workedDays"
            FROM employees e
            LEFT JOIN attendance_sessions a
                ON a.employee_id = e.id
               AND a.date BETWEEN :from AND :to
            WHERE e.company_id = :companyId
              AND (:branchId IS NULL OR e.branch_id = :branchId)
              AND e.is_active = true
            GROUP BY e.id, e.branch_id, e.employee_code, e.first_name, e.last_name, e.position, e.salary
            ORDER BY e.last_name, e.first_name, e.employee_code
            """, nativeQuery = true)
    List<PayrollEmployeeProjection> findEmployeePayrollRows(
            @Param("companyId") UUID companyId,
            @Param("branchId") UUID branchId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    interface PayrollEmployeeProjection {
        UUID getEmployeeId();
        UUID getBranchId();
        String getEmployeeCode();
        String getFirstName();
        String getLastName();
        String getPosition();
        BigDecimal getBaseSalary();
        Number getActualWorkMinutes();
        Number getLateMinutes();
        Number getWorkedDays();
    }
}
