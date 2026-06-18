package uz.workpulse.report.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import uz.workpulse.attendance.domain.AttendanceSession;

public interface ReportAttendanceReadRepository extends Repository<AttendanceSession, UUID> {

    // Reporting read model — direct SQL over attendance_sessions until a dedicated projection is introduced.
    @Query(value = """
            SELECT
                COUNT(DISTINCT a.employee_id) AS "totalEmployees",
                COALESCE(SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END), 0) AS "presentCount",
                COALESCE(SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END), 0) AS "lateCount",
                COALESCE(SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END), 0) AS "absentCount",
                COALESCE(SUM(CASE WHEN a.status = 'LEAVE' THEN 1 ELSE 0 END), 0) AS "leaveCount",
                COALESCE(SUM(a.work_minutes), 0) AS "totalWorkMinutes",
                COALESCE(SUM(a.late_minutes), 0) AS "totalLateMinutes",
                COUNT(a.id) AS "sessionsCount"
            FROM attendance_sessions a
            JOIN employees e ON e.id = a.employee_id
            WHERE (:companyId IS NULL OR e.company_id = :companyId)
              AND (:branchId IS NULL OR a.branch_id = :branchId)
              AND (:employeeId IS NULL OR a.employee_id = :employeeId)
              AND a.date BETWEEN :from AND :to
            """, nativeQuery = true)
    SummaryProjection summarize(
            @Param("companyId") UUID companyId,
            @Param("branchId") UUID branchId,
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query(value = """
            SELECT
                a.employee_id AS "employeeId",
                a.branch_id AS "branchId",
                a.date AS date,
                a.check_in_time AS "checkInTime",
                a.check_out_time AS "checkOutTime",
                a.status AS status,
                a.late_minutes AS "lateMinutes",
                a.work_minutes AS "workMinutes",
                a.method AS method
            FROM attendance_sessions a
            JOIN employees e ON e.id = a.employee_id
            WHERE (:companyId IS NULL OR e.company_id = :companyId)
              AND (:branchId IS NULL OR a.branch_id = :branchId)
              AND (:employeeId IS NULL OR a.employee_id = :employeeId)
              AND a.date BETWEEN :from AND :to
            ORDER BY a.date, a.check_in_time
            """, nativeQuery = true)
    List<SessionRowProjection> findRows(
            @Param("companyId") UUID companyId,
            @Param("branchId") UUID branchId,
            @Param("employeeId") UUID employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query(value = """
            SELECT DISTINCT e.company_id AS "companyId", a.branch_id AS "branchId"
            FROM attendance_sessions a
            JOIN employees e ON e.id = a.employee_id
            WHERE a.date = :date
              AND a.branch_id IS NOT NULL
            """, nativeQuery = true)
    List<SnapshotScopeProjection> findSnapshotScopes(@Param("date") LocalDate date);

    interface SummaryProjection {
        Number getTotalEmployees();
        Number getPresentCount();
        Number getLateCount();
        Number getAbsentCount();
        Number getLeaveCount();
        Number getTotalWorkMinutes();
        Number getTotalLateMinutes();
        Number getSessionsCount();
    }

    interface SessionRowProjection {
        UUID getEmployeeId();
        UUID getBranchId();
        LocalDate getDate();
        Instant getCheckInTime();
        Instant getCheckOutTime();
        String getStatus();
        Number getLateMinutes();
        Number getWorkMinutes();
        String getMethod();
    }

    interface SnapshotScopeProjection {
        UUID getCompanyId();
        UUID getBranchId();
    }
}
