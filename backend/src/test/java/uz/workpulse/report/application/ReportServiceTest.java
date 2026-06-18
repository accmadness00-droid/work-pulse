package uz.workpulse.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.company.application.CompanyFacade;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.report.domain.ReportSnapshot;
import uz.workpulse.report.infrastructure.ReportAttendanceReadRepository;
import uz.workpulse.report.infrastructure.ReportSnapshotRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportAttendanceReadRepository attendanceReadRepository;

    @Mock
    private ReportSnapshotRepository snapshotRepository;

    @Mock
    private CompanyFacade companyFacade;

    @Mock
    private BranchFacade branchFacade;

    @Mock
    private EmployeeFacade employeeFacade;

    @Mock
    private AccessControlService accessControlService;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                attendanceReadRepository,
                snapshotRepository,
                companyFacade,
                branchFacade,
                employeeFacade,
                accessControlService
        );
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dailyReportCalculatesPresentLateAndWorkMinutes() {
        UUID companyId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 13);
        when(attendanceReadRepository.summarize(companyId, null, null, date, date))
                .thenReturn(new Summary(2, 1, 1, 0, 0, 960, 30, 2));

        var response = reportService.getDailyReport(companyId, null, date);

        assertThat(response.summary().presentCount()).isEqualTo(1);
        assertThat(response.summary().lateCount()).isEqualTo(1);
        assertThat(response.summary().totalWorkMinutes()).isEqualTo(960);
        assertThat(response.summary().averageWorkMinutes()).isEqualTo(480);
        verify(companyFacade).ensureActiveCompany(companyId);
    }

    @Test
    void monthlyReportAggregatesDailyData() {
        UUID companyId = UUID.randomUUID();
        when(attendanceReadRepository.summarize(companyId, null, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(new Summary(5, 20, 3, 1, 2, 10_000, 90, 26));

        var response = reportService.getMonthlyReport(companyId, null, 2026, 6);

        assertThat(response.summary().sessionsCount()).isEqualTo(26);
        assertThat(response.summary().leaveCount()).isEqualTo(2);
    }

    @Test
    void employeeReportReturnsEmployeeSessions() {
        UUID employeeId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 13);
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(
                new EmployeeFacade.EmployeeScope(employeeId, UUID.randomUUID(), UUID.randomUUID(), null)
        ));
        when(attendanceReadRepository.summarize(null, null, employeeId, from, to)).thenReturn(new Summary(1, 5, 1, 0, 0, 2_400, 15, 6));
        when(attendanceReadRepository.findRows(null, null, employeeId, from, to)).thenReturn(List.of(row(employeeId, UUID.randomUUID(), from)));

        var response = reportService.getEmployeeReport(employeeId, from, to);

        assertThat(response.sessions()).hasSize(1);
        assertThat(response.summary().lateCount()).isEqualTo(1);
    }

    @Test
    void branchReportFiltersByBranch() {
        UUID branchId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 13);
        when(branchFacade.getCompanyId(branchId)).thenReturn(companyId);
        when(attendanceReadRepository.summarize(null, branchId, null, from, to)).thenReturn(new Summary(3, 10, 2, 0, 1, 5_000, 20, 13));
        when(attendanceReadRepository.findRows(null, branchId, null, from, to)).thenReturn(List.of(row(UUID.randomUUID(), branchId, from)));

        var response = reportService.getBranchReport(branchId, from, to);

        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.sessions()).allMatch(row -> row.branchId().equals(branchId));
    }

    @Test
    void invalidDateRangeThrowsBusinessException() {
        UUID companyId = UUID.randomUUID();

        assertThatThrownBy(() -> reportService.getSummary(companyId, null, LocalDate.of(2026, 6, 13), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPORT_INVALID_DATE_RANGE);
    }

    @Test
    void dailySnapshotGenerateUpdateWorks() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 13);
        ReportSnapshot existing = new ReportSnapshot(
                companyId,
                branchId,
                ReportSnapshot.PeriodType.DAILY,
                date,
                date
        );
        when(attendanceReadRepository.findSnapshotScopes(date)).thenReturn(List.of(new Scope(companyId, branchId)));
        when(attendanceReadRepository.summarize(companyId, branchId, null, date, date)).thenReturn(new Summary(2, 1, 1, 0, 0, 960, 30, 2));
        when(snapshotRepository.findByCompanyIdAndBranchIdAndPeriodTypeAndPeriodStartAndPeriodEnd(
                companyId,
                branchId,
                ReportSnapshot.PeriodType.DAILY,
                date,
                date
        )).thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any(ReportSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var snapshots = reportService.generateDailySnapshot(date);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getTotalWorkMinutes()).isEqualTo(960);
        verify(snapshotRepository).save(existing);
    }

    private ReportAttendanceReadRepository.SessionRowProjection row(UUID employeeId, UUID branchId, LocalDate date) {
        return new RowProjection(employeeId, branchId, date);
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));
    }

    private record Summary(
            Number getTotalEmployees,
            Number getPresentCount,
            Number getLateCount,
            Number getAbsentCount,
            Number getLeaveCount,
            Number getTotalWorkMinutes,
            Number getTotalLateMinutes,
            Number getSessionsCount
    ) implements ReportAttendanceReadRepository.SummaryProjection {
    }

    private record Scope(UUID getCompanyId, UUID getBranchId) implements ReportAttendanceReadRepository.SnapshotScopeProjection {
    }

    private record RowProjection(UUID getEmployeeId, UUID getBranchId, LocalDate getDate) implements ReportAttendanceReadRepository.SessionRowProjection {
        @Override
        public Instant getCheckInTime() {
            return Instant.parse("2026-06-13T04:00:00Z");
        }

        @Override
        public Instant getCheckOutTime() {
            return Instant.parse("2026-06-13T12:00:00Z");
        }

        @Override
        public String getStatus() {
            return "PRESENT";
        }

        @Override
        public Number getLateMinutes() {
            return 0;
        }

        @Override
        public Number getWorkMinutes() {
            return 480;
        }

        @Override
        public String getMethod() {
            return "MANUAL";
        }
    }
}
