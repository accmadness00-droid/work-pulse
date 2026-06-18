package uz.workpulse.report.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.company.application.CompanyFacade;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.report.domain.ReportSnapshot;
import uz.workpulse.report.dto.AttendanceSummaryResponse;
import uz.workpulse.report.dto.BranchReportResponse;
import uz.workpulse.report.dto.DailyReportResponse;
import uz.workpulse.report.dto.EmployeeReportResponse;
import uz.workpulse.report.dto.MonthlyReportResponse;
import uz.workpulse.report.dto.ReportRequest;
import uz.workpulse.report.dto.ReportSessionRow;
import uz.workpulse.report.infrastructure.ReportAttendanceReadRepository;
import uz.workpulse.report.infrastructure.ReportSnapshotRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class ReportService {

    private final ReportAttendanceReadRepository attendanceReadRepository;
    private final ReportSnapshotRepository snapshotRepository;
    private final CompanyFacade companyFacade;
    private final BranchFacade branchFacade;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;

    public ReportService(
            ReportAttendanceReadRepository attendanceReadRepository,
            ReportSnapshotRepository snapshotRepository,
            CompanyFacade companyFacade,
            BranchFacade branchFacade,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService
    ) {
        this.attendanceReadRepository = attendanceReadRepository;
        this.snapshotRepository = snapshotRepository;
        this.companyFacade = companyFacade;
        this.branchFacade = branchFacade;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(UUID companyId, UUID branchId, LocalDate from, LocalDate to) {
        requireReportReadAccess();
        requireCompany(companyId);
        validateOptionalBranch(branchId);
        validateDateRange(from, to);
        return toSummary(attendanceReadRepository.summarize(companyId, branchId, null, from, to));
    }

    @Transactional(readOnly = true)
    public DailyReportResponse getDailyReport(UUID companyId, UUID branchId, LocalDate date) {
        requireReportReadAccess();
        requireCompany(companyId);
        validateOptionalBranch(branchId);
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        AttendanceSummaryResponse summary = toSummary(attendanceReadRepository.summarize(companyId, branchId, null, reportDate, reportDate));
        return new DailyReportResponse(companyId, branchId, reportDate, summary);
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(UUID companyId, UUID branchId, int year, int month) {
        requireReportReadAccess();
        requireCompany(companyId);
        validateOptionalBranch(branchId);
        YearMonth yearMonth = validateYearMonth(year, month);
        AttendanceSummaryResponse summary = toSummary(attendanceReadRepository.summarize(
                companyId,
                branchId,
                null,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth()
        ));
        return new MonthlyReportResponse(companyId, branchId, year, month, summary);
    }

    @Transactional(readOnly = true)
    public EmployeeReportResponse getEmployeeReport(UUID employeeId, LocalDate from, LocalDate to) {
        requireEmployeeReportAccess(employeeId);
        validateEmployee(employeeId);
        LocalDate effectiveFrom = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        validateDateRange(effectiveFrom, effectiveTo);
        AttendanceSummaryResponse summary = toSummary(attendanceReadRepository.summarize(null, null, employeeId, effectiveFrom, effectiveTo));
        return new EmployeeReportResponse(employeeId, effectiveFrom, effectiveTo, summary, rows(null, null, employeeId, effectiveFrom, effectiveTo));
    }

    @Transactional(readOnly = true)
    public BranchReportResponse getBranchReport(UUID branchId, LocalDate from, LocalDate to) {
        requireReportReadAccess();
        branchFacade.ensureActiveBranch(branchId);
        UUID companyId = branchFacade.getCompanyId(branchId);
        accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, branchId);
        LocalDate effectiveFrom = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        validateDateRange(effectiveFrom, effectiveTo);
        AttendanceSummaryResponse summary = toSummary(attendanceReadRepository.summarize(null, branchId, null, effectiveFrom, effectiveTo));
        return new BranchReportResponse(branchId, effectiveFrom, effectiveTo, summary, rows(null, branchId, null, effectiveFrom, effectiveTo));
    }

    @Transactional(readOnly = true)
    public List<ReportSessionRow> findExportRows(ReportRequest request) {
        requireReportReadAccess();
        LocalDate from = resolveFrom(request);
        LocalDate to = resolveTo(request);
        validateDateRange(from, to);
        return rows(request.companyId(), request.branchId(), request.employeeId(), from, to);
    }

    @Transactional
    public List<ReportSnapshot> generateDailySnapshot(LocalDate date) {
        requireReportWriteAccess();
        return generateDailySnapshotInternal(date);
    }

    @Transactional
    public List<ReportSnapshot> generateDailySnapshotForScheduler(LocalDate date) {
        return generateDailySnapshotInternal(date);
    }

    private List<ReportSnapshot> generateDailySnapshotInternal(LocalDate date) {
        LocalDate snapshotDate = date == null ? LocalDate.now().minusDays(1) : date;
        List<ReportSnapshot> snapshots = new ArrayList<>();
        for (ReportAttendanceReadRepository.SnapshotScopeProjection scope : attendanceReadRepository.findSnapshotScopes(snapshotDate)) {
            AttendanceSummaryResponse summary = toSummary(attendanceReadRepository.summarize(
                    scope.getCompanyId(),
                    scope.getBranchId(),
                    null,
                    snapshotDate,
                    snapshotDate
            ));
            snapshots.add(upsertSnapshot(scope.getCompanyId(), scope.getBranchId(), snapshotDate, summary));
        }
        return snapshots;
    }

    @Transactional
    public List<ReportSnapshot> generateMonthlySnapshot(int year, int month) {
        requireReportWriteAccess();
        YearMonth yearMonth = validateYearMonth(year, month);
        List<ReportSnapshot> snapshots = new ArrayList<>();
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            snapshots.addAll(generateDailySnapshotInternal(date));
        }
        return snapshots;
    }

    @Transactional
    public List<ReportSnapshot> regenerateSnapshot(ReportRequest request) {
        if (request.date() != null) {
            return generateDailySnapshot(request.date());
        }
        if (request.year() != null && request.month() != null) {
            return generateMonthlySnapshot(request.year(), request.month());
        }
        throw new BusinessException(ErrorCode.REPORT_INVALID_DATE_RANGE);
    }

    private ReportSnapshot upsertSnapshot(UUID companyId, UUID branchId, LocalDate date, AttendanceSummaryResponse summary) {
        ReportSnapshot snapshot = snapshotRepository.findByCompanyIdAndBranchIdAndPeriodTypeAndPeriodStartAndPeriodEnd(
                        companyId,
                        branchId,
                        ReportSnapshot.PeriodType.DAILY,
                        date,
                        date
                )
                .orElseGet(() -> new ReportSnapshot(
                        companyId,
                        branchId,
                        ReportSnapshot.PeriodType.DAILY,
                        date,
                        date
                ));
        snapshot.setTotalEmployees((int) summary.totalEmployees());
        snapshot.setPresentCount((int) summary.presentCount());
        snapshot.setLateCount((int) summary.lateCount());
        snapshot.setAbsentCount((int) summary.absentCount());
        snapshot.setLeaveCount((int) summary.leaveCount());
        snapshot.setAvgWorkMinutes((int) summary.averageWorkMinutes());
        snapshot.setTotalWorkMinutes((int) summary.totalWorkMinutes());
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setGeneratedBy(ReportSnapshot.GeneratedBy.SCHEDULER);
        return snapshotRepository.save(snapshot);
    }

    private List<ReportSessionRow> rows(UUID companyId, UUID branchId, UUID employeeId, LocalDate from, LocalDate to) {
        return attendanceReadRepository.findRows(companyId, branchId, employeeId, from, to).stream()
                .map(row -> new ReportSessionRow(
                        row.getEmployeeId(),
                        row.getBranchId(),
                        row.getDate(),
                        row.getCheckInTime(),
                        row.getCheckOutTime(),
                        row.getStatus(),
                        number(row.getLateMinutes()),
                        number(row.getWorkMinutes()),
                        row.getMethod()
                ))
                .toList();
    }

    private AttendanceSummaryResponse toSummary(ReportAttendanceReadRepository.SummaryProjection projection) {
        long sessionsCount = number(projection.getSessionsCount());
        long totalWorkMinutes = number(projection.getTotalWorkMinutes());
        long averageWorkMinutes = sessionsCount == 0 ? 0 : totalWorkMinutes / sessionsCount;
        return new AttendanceSummaryResponse(
                number(projection.getTotalEmployees()),
                number(projection.getPresentCount()),
                number(projection.getLateCount()),
                number(projection.getAbsentCount()),
                number(projection.getLeaveCount()),
                totalWorkMinutes,
                averageWorkMinutes,
                number(projection.getTotalLateMinutes()),
                sessionsCount
        );
    }

    private int number(Number number) {
        return number == null ? 0 : number.intValue();
    }

    private void requireCompany(UUID companyId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "companyId is required");
        }
        companyFacade.ensureActiveCompany(companyId);
        accessControlService.requireCompanyAccess(accessControlService.currentUser(), companyId);
    }

    private void validateOptionalBranch(UUID branchId) {
        if (branchId != null) {
            UUID companyId = branchFacade.getCompanyId(branchId);
            branchFacade.ensureActiveBranch(branchId);
            accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, branchId);
        }
    }

    private void validateEmployee(UUID employeeId) {
        if (!employeeFacade.existsAndActive(employeeId)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_DATE_RANGE);
        }
    }

    private YearMonth validateYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_MONTH);
        }
        return YearMonth.of(year, month);
    }

    private LocalDate resolveFrom(ReportRequest request) {
        if (request.date() != null) {
            return request.date();
        }
        if (request.year() != null && request.month() != null) {
            return validateYearMonth(request.year(), request.month()).atDay(1);
        }
        return request.from();
    }

    private LocalDate resolveTo(ReportRequest request) {
        if (request.date() != null) {
            return request.date();
        }
        if (request.year() != null && request.month() != null) {
            return validateYearMonth(request.year(), request.month()).atEndOfMonth();
        }
        return request.to();
    }

    private void requireReportReadAccess() {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN || role == User.Role.BRANCH_MANAGER) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireEmployeeReportAccess(UUID employeeId) {
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        accessControlService.requireEmployeeSelfOrAdmin(
                accessControlService.currentUser(),
                scope.userId(),
                scope.companyId(),
                scope.branchId()
        );
    }

    private void requireReportWriteAccess() {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public void invalidateDailySnapshotForDate(UUID branchId, LocalDate date) {
        UUID companyId = branchFacade.getCompanyId(branchId);
        snapshotRepository.findByCompanyIdAndBranchIdAndPeriodTypeAndPeriodStartAndPeriodEnd(
                        companyId,
                        branchId,
                        ReportSnapshot.PeriodType.DAILY,
                        date,
                        date
                )
                .ifPresent(snapshotRepository::delete);
    }

    private AuthPrincipal currentPrincipal() {
        return accessControlService.currentUser();
    }
}
