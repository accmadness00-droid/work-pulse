package uz.workpulse.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.attendance.domain.AttendanceSession;
import uz.workpulse.attendance.dto.CheckInRequest;
import uz.workpulse.attendance.dto.CheckOutRequest;
import uz.workpulse.attendance.dto.UpdateAttendanceRequest;
import uz.workpulse.attendance.infrastructure.AttendanceSessionRepository;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.branch.dto.BranchScheduleInfo;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.employee.application.EmployeeScheduleFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceSessionRepository attendanceRepository;

    @Mock
    private EmployeeFacade employeeFacade;

    @Mock
    private EmployeeScheduleFacade employeeScheduleFacade;

    @Mock
    private BranchFacade branchFacade;

    @Mock
    private GeofenceService geofenceService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                attendanceRepository,
                employeeFacade,
                employeeScheduleFacade,
                branchFacade,
                geofenceService,
                accessControlService,
                eventPublisher
        );
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        lenient().when(employeeScheduleFacade.getScheduleForDate(any(), any())).thenReturn(Optional.empty());
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkInHappyPathPresent() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        mockEmployeeAndBranch(employeeId, branchId);
        when(attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(false);
        when(branchFacade.getScheduleForDate(eq(branchId), any(LocalDate.class)))
                .thenReturn(new BranchScheduleInfo(LocalTime.of(0, 0), LocalTime.of(23, 59), 2_000, true));
        when(attendanceRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        doNothing().when(geofenceService).validateCheckIn(any(), any(), any(), any());

        var response = attendanceService.checkIn(new CheckInRequest(employeeId, branchId, null, null, AttendanceSession.Method.MANUAL, null));

        assertThat(response.status()).isEqualTo(AttendanceSession.Status.PRESENT);
        assertThat(response.lateMinutes()).isZero();
    }

    @Test
    void checkInLateStatusLate() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        mockEmployeeAndBranch(employeeId, branchId);
        when(attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(false);
        when(branchFacade.getScheduleForDate(eq(branchId), any(LocalDate.class)))
                .thenReturn(new BranchScheduleInfo(LocalTime.now(), LocalTime.of(23, 59), -10, true));
        when(attendanceRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));
        doNothing().when(geofenceService).validateCheckIn(any(), any(), any(), any());

        var response = attendanceService.checkIn(new CheckInRequest(employeeId, branchId, null, null, AttendanceSession.Method.MANUAL, null));

        assertThat(response.status()).isEqualTo(AttendanceSession.Status.LATE);
        assertThat(response.lateMinutes()).isPositive();
    }

    @Test
    void duplicateOpenSessionThrowsError() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        mockEmployeeAndBranch(employeeId, branchId);
        when(attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(true);
        doNothing().when(geofenceService).validateCheckIn(any(), any(), any(), any());

        assertThatThrownBy(() -> attendanceService.checkIn(new CheckInRequest(employeeId, branchId, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_OPEN_SESSION_EXISTS);
    }

    @Test
    void checkOutHappyPathCalculatesWorkMinutes() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        AttendanceSession session = openSession(employeeId, branchId, Instant.now().minusSeconds(7_200));
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(scope(employeeId, branchId)));
        when(attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(Optional.of(session));
        when(attendanceRepository.save(session)).thenReturn(session);

        var response = attendanceService.checkOut(new CheckOutRequest(employeeId, null, null, AttendanceSession.Method.MANUAL, null));

        assertThat(response.checkOutTime()).isNotNull();
        assertThat(response.workMinutes()).isGreaterThanOrEqualTo(119);
    }

    @Test
    void checkOutWithoutOpenSessionThrowsError() {
        UUID employeeId = UUID.randomUUID();
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(scope(employeeId, UUID.randomUUID())));
        when(attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkOut(new CheckOutRequest(employeeId, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_OPEN_SESSION_NOT_FOUND);
    }

    @Test
    void invalidCheckoutTimeThrowsError() {
        UUID employeeId = UUID.randomUUID();
        AttendanceSession session = openSession(employeeId, UUID.randomUUID(), Instant.now().plusSeconds(3_600));
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(scope(employeeId, UUID.randomUUID())));
        when(attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> attendanceService.checkOut(new CheckOutRequest(employeeId, null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTENDANCE_INVALID_CHECKOUT_TIME);
    }

    @Test
    void processDeviceEventInAndOutWorks() {
        UUID employeeId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(employeeFacade.existsAndActive(employeeId)).thenReturn(true);
        when(employeeFacade.findBranchIdByEmployeeId(employeeId)).thenReturn(Optional.of(branchId));
        when(attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(false);
        when(branchFacade.getScheduleForDate(eq(branchId), any(LocalDate.class)))
                .thenReturn(new BranchScheduleInfo(LocalTime.of(9, 0), LocalTime.of(18, 0), 15, true));
        when(attendanceRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> withId(invocation.getArgument(0)));

        assertThat(attendanceService.processDeviceEvent(
                employeeId,
                Instant.now().minusSeconds(600),
                AttendanceFacade.Direction.IN,
                deviceId,
                branchId
        )).isEqualTo(AttendanceFacade.ProcessDeviceEventResult.CHECKED_IN);

        AttendanceSession open = openSession(employeeId, branchId, Instant.now().minusSeconds(600));
        when(attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(Optional.of(open));
        when(attendanceRepository.save(open)).thenReturn(open);

        assertThat(attendanceService.processDeviceEvent(
                employeeId,
                Instant.now(),
                AttendanceFacade.Direction.OUT,
                deviceId,
                branchId
        )).isEqualTo(AttendanceFacade.ProcessDeviceEventResult.CHECKED_OUT);

        assertThat(open.getCheckOutTime()).isNotNull();
        assertThat(open.getWorkMinutes()).isPositive();
        assertThat(open.getSourceDeviceId()).isEqualTo(deviceId);
    }

    @Test
    void processDeviceEventDuplicateInIsIdempotent() {
        UUID employeeId = UUID.randomUUID();
        when(employeeFacade.existsAndActive(employeeId)).thenReturn(true);
        when(attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)).thenReturn(true);

        assertThat(attendanceService.processDeviceEvent(
                employeeId,
                Instant.now(),
                AttendanceFacade.Direction.IN,
                UUID.randomUUID(),
                UUID.randomUUID()
        )).isEqualTo(AttendanceFacade.ProcessDeviceEventResult.DUPLICATE_IN);
    }

    @Test
    void adminUpdateRecalculatesWorkMinutes() {
        UUID sessionId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        AttendanceSession session = openSession(employeeId, branchId, Instant.parse("2026-06-13T04:00:00Z"));
        ReflectionTestUtils.setField(session, "id", sessionId);
        when(attendanceRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(scope(employeeId, branchId)));
        when(branchFacade.getScheduleForDate(eq(branchId), any(LocalDate.class)))
                .thenReturn(new BranchScheduleInfo(LocalTime.of(9, 0), LocalTime.of(18, 0), 15, true));
        when(attendanceRepository.save(session)).thenReturn(session);

        var response = attendanceService.adminUpdate(
                sessionId,
                new UpdateAttendanceRequest(
                        Instant.parse("2026-06-13T04:00:00Z"),
                        Instant.parse("2026-06-13T12:00:00Z"),
                        null,
                        "fixed"
                )
        );

        assertThat(response.workMinutes()).isEqualTo(480);
        assertThat(response.note()).isEqualTo("fixed");
    }

    private void mockEmployeeAndBranch(UUID employeeId, UUID branchId) {
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(scope(employeeId, branchId)));
        doNothing().when(branchFacade).ensureActiveBranch(branchId);
    }

    private EmployeeFacade.EmployeeScope scope(UUID employeeId, UUID branchId) {
        return new EmployeeFacade.EmployeeScope(employeeId, UUID.randomUUID(), branchId, null);
    }

    private AttendanceSession openSession(UUID employeeId, UUID branchId, Instant checkInTime) {
        AttendanceSession session = new AttendanceSession(
                employeeId,
                branchId,
                LocalDate.ofInstant(checkInTime, java.time.ZoneId.systemDefault()),
                checkInTime,
                AttendanceSession.Method.MANUAL
        );
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }

    private AttendanceSession withId(AttendanceSession session) {
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
