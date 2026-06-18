package uz.workpulse.attendance.application;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.attendance.application.event.AttendanceSessionCreatedEvent;
import uz.workpulse.attendance.domain.AttendanceSession;
import uz.workpulse.attendance.dto.AttendanceFilterRequest;
import uz.workpulse.attendance.dto.AttendanceResponse;
import uz.workpulse.attendance.dto.CheckInRequest;
import uz.workpulse.attendance.dto.CheckOutRequest;
import uz.workpulse.attendance.dto.UpdateAttendanceRequest;
import uz.workpulse.attendance.infrastructure.AttendanceSessionRepository;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.branch.dto.BranchScheduleInfo;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.employee.application.EmployeeScheduleFacade;
import uz.workpulse.employee.dto.EmployeeScheduleInfo;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class AttendanceService implements AttendanceFacade {

    private final AttendanceSessionRepository attendanceRepository;
    private final EmployeeFacade employeeFacade;
    private final EmployeeScheduleFacade employeeScheduleFacade;
    private final BranchFacade branchFacade;
    private final GeofenceService geofenceService;
    private final AccessControlService accessControlService;
    private final ApplicationEventPublisher eventPublisher;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public AttendanceService(
            AttendanceSessionRepository attendanceRepository,
            EmployeeFacade employeeFacade,
            EmployeeScheduleFacade employeeScheduleFacade,
            BranchFacade branchFacade,
            GeofenceService geofenceService,
            AccessControlService accessControlService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.attendanceRepository = attendanceRepository;
        this.employeeFacade = employeeFacade;
        this.employeeScheduleFacade = employeeScheduleFacade;
        this.branchFacade = branchFacade;
        this.geofenceService = geofenceService;
        this.accessControlService = accessControlService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public AttendanceResponse checkIn(CheckInRequest request) {
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(request.employeeId());
        requireAttendanceWriteAccess(scope);
        branchFacade.ensureActiveBranch(request.branchId());
        geofenceService.validateCheckIn(request.branchId(), request.latitude(), request.longitude(), defaultMethod(request.method()));

        if (attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(request.employeeId())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_OPEN_SESSION_EXISTS);
        }

        Instant now = Instant.now();
        AttendanceSession session = new AttendanceSession(
                request.employeeId(),
                request.branchId(),
                LocalDate.ofInstant(now, zoneId),
                now,
                defaultMethod(request.method())
        );
        session.setCheckInLat(request.latitude());
        session.setCheckInLng(request.longitude());
        session.setNote(request.note());
        applyLateStatus(session);

        try {
            AttendanceSession saved = attendanceRepository.save(session);
            publishCreated(saved);
            return AttendanceResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.ATTENDANCE_OPEN_SESSION_EXISTS);
        }
    }

    @Transactional
    @Override
    public AttendanceResponse checkOut(CheckOutRequest request) {
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(request.employeeId());
        requireAttendanceWriteAccess(scope);
        AttendanceSession session = attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(request.employeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_OPEN_SESSION_NOT_FOUND));

        Instant now = Instant.now();
        applyCheckOut(session, now, request.latitude(), request.longitude(), defaultMethod(request.method()), request.note());
        return AttendanceResponse.from(attendanceRepository.save(session));
    }

    @Override
    @Transactional
    public AttendanceResponse markCameraVerification(
            UUID attendanceId,
            AttendanceFacade.CameraVerificationAction action,
            String photoUrl,
            BigDecimal faceDistance
    ) {
        AttendanceSession session = getSessionOrThrow(attendanceId);
        if (action == AttendanceFacade.CameraVerificationAction.CHECK_IN) {
            session.setCheckInPhotoUrl(photoUrl);
            session.setCheckInLocationVerified(true);
            session.setCheckInFaceVerified(true);
            session.setCheckInFaceDistance(faceDistance);
        } else {
            session.setCheckOutPhotoUrl(photoUrl);
            session.setCheckOutLocationVerified(true);
            session.setCheckOutFaceVerified(true);
            session.setCheckOutFaceDistance(faceDistance);
        }
        return AttendanceResponse.from(attendanceRepository.save(session));
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getById(UUID id) {
        AttendanceSession session = getSessionOrThrow(id);
        requireAttendanceReadAccess(requireEmployeeScope(session.getEmployeeId()));
        return AttendanceResponse.from(session);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> list(AttendanceFilterRequest filter, Pageable pageable) {
        requireAttendanceListAccess();
        validateDateRange(filter.from(), filter.to());
        return attendanceRepository.findAll(toSpecification(filter), pageable)
                .map(AttendanceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getToday(UUID companyId, UUID branchId, Pageable pageable) {
        accessControlService.requireCompanyAccess(accessControlService.currentUser(), companyId);
        if (branchId != null) {
            accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, branchId);
        }
        AttendanceFilterRequest filter = new AttendanceFilterRequest(null, branchId, LocalDate.now(zoneId), null, null, null, null);
        return list(filter, pageable);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getEmployeeHistory(UUID employeeId, LocalDate from, LocalDate to) {
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(employeeId);
        requireAttendanceReadAccess(scope);
        validateDateRange(from, to);
        LocalDate effectiveFrom = from == null ? LocalDate.now(zoneId).minusMonths(1) : from;
        LocalDate effectiveTo = to == null ? LocalDate.now(zoneId) : to;
        return attendanceRepository.findAllByEmployeeIdAndDateBetweenOrderByDateDesc(employeeId, effectiveFrom, effectiveTo)
                .stream()
                .map(AttendanceResponse::from)
                .toList();
    }

    @Transactional
    public AttendanceResponse adminUpdate(UUID id, UpdateAttendanceRequest request) {
        AttendanceSession session = getSessionOrThrow(id);
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(session.getEmployeeId());
        requireAttendanceAdminAccess(scope);
        if (request.checkInTime() != null) {
            session.setCheckInTime(request.checkInTime());
            session.setDate(LocalDate.ofInstant(request.checkInTime(), zoneId));
        }
        if (request.checkOutTime() != null) {
            session.setCheckOutTime(request.checkOutTime());
        }
        if (request.note() != null) {
            session.setNote(request.note());
        }
        recalculate(session);
        if (request.status() != null) {
            session.setStatus(request.status());
        }
        return AttendanceResponse.from(attendanceRepository.save(session));
    }

    @Override
    @Transactional
    public ProcessDeviceEventResult processDeviceEvent(
            UUID employeeId,
            Instant eventTime,
            Direction direction,
            UUID deviceId,
            UUID branchId
    ) {
        validateEmployee(employeeId);
        if (direction == Direction.IN) {
            return checkInFromDevice(employeeId, eventTime, deviceId, branchId);
        }
        return checkOutFromDevice(employeeId, eventTime, deviceId);
    }

    private ProcessDeviceEventResult checkInFromDevice(UUID employeeId, Instant eventTime, UUID deviceId, UUID branchId) {
        if (attendanceRepository.existsByEmployeeIdAndCheckOutTimeIsNull(employeeId)) {
            return ProcessDeviceEventResult.DUPLICATE_IN;
        }

        UUID effectiveBranchId = branchId != null
                ? branchId
                : employeeFacade.findBranchIdByEmployeeId(employeeId).orElse(null);

        AttendanceSession session = new AttendanceSession(
                employeeId,
                effectiveBranchId,
                LocalDate.ofInstant(eventTime, zoneId),
                eventTime,
                AttendanceSession.Method.DEVICE
        );
        session.setSourceDeviceId(deviceId);
        applyLateStatus(session);

        try {
            AttendanceSession saved = attendanceRepository.save(session);
            publishCreated(saved);
            return ProcessDeviceEventResult.CHECKED_IN;
        } catch (DataIntegrityViolationException ex) {
            return ProcessDeviceEventResult.DUPLICATE_IN;
        }
    }

    private ProcessDeviceEventResult checkOutFromDevice(UUID employeeId, Instant eventTime, UUID deviceId) {
        AttendanceSession session = attendanceRepository.findByEmployeeIdAndCheckOutTimeIsNull(employeeId).orElse(null);
        if (session == null) {
            return ProcessDeviceEventResult.ORPHAN_OUT;
        }
        applyCheckOut(session, eventTime, null, null, AttendanceSession.Method.DEVICE, session.getNote());
        session.setSourceDeviceId(deviceId);
        attendanceRepository.save(session);
        return ProcessDeviceEventResult.CHECKED_OUT;
    }

    private void publishCreated(AttendanceSession session) {
        eventPublisher.publishEvent(new AttendanceSessionCreatedEvent(
                session.getId(),
                session.getEmployeeId(),
                session.getBranchId(),
                session.getDate()
        ));
    }

    private void applyCheckOut(
            AttendanceSession session,
            Instant checkOutTime,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            AttendanceSession.Method method,
            String note
    ) {
        if (session.getCheckInTime() != null && checkOutTime.isBefore(session.getCheckInTime())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_CHECKOUT_TIME);
        }
        session.setCheckOutTime(checkOutTime);
        session.setCheckOutLat(latitude);
        session.setCheckOutLng(longitude);
        session.setMethod(method);
        if (note != null) {
            session.setNote(note);
        }
        recalculateWorkMinutes(session);
    }

    private void recalculate(AttendanceSession session) {
        if (session.getCheckOutTime() != null && session.getCheckInTime() != null && session.getCheckOutTime().isBefore(session.getCheckInTime())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_CHECKOUT_TIME);
        }
        applyLateStatus(session);
        recalculateWorkMinutes(session);
    }

    private void applyLateStatus(AttendanceSession session) {
        if (session.getBranchId() == null || session.getCheckInTime() == null) {
            session.setLateMinutes(0);
            if (session.getStatus() == null) {
                session.setStatus(AttendanceSession.Status.PRESENT);
            }
            return;
        }

        WorkSchedule schedule = getSchedule(session.getEmployeeId(), session.getBranchId(), session.getDate());
        if (!schedule.isWorkday()) {
            session.setLateMinutes(0);
            session.setStatus(AttendanceSession.Status.PRESENT);
            return;
        }

        LocalDateTime checkIn = LocalDateTime.ofInstant(session.getCheckInTime(), zoneId);
        LocalDateTime scheduledStart = LocalDateTime.of(session.getDate(), schedule.startTime());
        long minutes = Duration.between(scheduledStart, checkIn).toMinutes() - schedule.lateThresholdMin();
        int lateMinutes = (int) Math.max(0, minutes);
        session.setLateMinutes(lateMinutes);
        session.setStatus(lateMinutes > 0 ? AttendanceSession.Status.LATE : AttendanceSession.Status.PRESENT);
    }

    private WorkSchedule getSchedule(UUID employeeId, UUID branchId, LocalDate date) {
        return employeeScheduleFacade.getScheduleForDate(employeeId, date)
                .map(WorkSchedule::fromEmployee)
                .orElseGet(() -> getBranchSchedule(branchId, date));
    }

    private WorkSchedule getBranchSchedule(UUID branchId, LocalDate date) {
        try {
            return WorkSchedule.fromBranch(branchFacade.getScheduleForDate(branchId, date));
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.BRANCH_SCHEDULE_NOT_FOUND) {
                throw new BusinessException(ErrorCode.ATTENDANCE_SCHEDULE_NOT_FOUND);
            }
            throw ex;
        }
    }

    private record WorkSchedule(
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            int lateThresholdMin,
            boolean isWorkday
    ) {
        private static WorkSchedule fromEmployee(EmployeeScheduleInfo schedule) {
            return new WorkSchedule(
                    schedule.startTime(),
                    schedule.endTime(),
                    schedule.lateThresholdMin(),
                    schedule.isWorkday()
            );
        }

        private static WorkSchedule fromBranch(BranchScheduleInfo schedule) {
            return new WorkSchedule(
                    schedule.startTime(),
                    schedule.endTime(),
                    schedule.lateThresholdMin(),
                    schedule.isWorkday()
            );
        }
    }

    private void recalculateWorkMinutes(AttendanceSession session) {
        if (session.getCheckInTime() == null || session.getCheckOutTime() == null) {
            session.setWorkMinutes(0);
            return;
        }
        session.setWorkMinutes((int) Duration.between(session.getCheckInTime(), session.getCheckOutTime()).toMinutes());
    }

    private AttendanceSession getSessionOrThrow(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTENDANCE_NOT_FOUND));
    }

    private EmployeeFacade.EmployeeScope requireEmployeeScope(UUID employeeId) {
        return employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private void validateEmployee(UUID employeeId) {
        if (!employeeFacade.existsAndActive(employeeId)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
    }

    private AttendanceSession.Method defaultMethod(AttendanceSession.Method method) {
        return method == null ? AttendanceSession.Method.MANUAL : method;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.ATTENDANCE_INVALID_DATE_RANGE);
        }
    }

    private Specification<AttendanceSession> toSpecification(AttendanceFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.employeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.employeeId()));
            }
            if (filter.branchId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("branchId"), filter.branchId()));
            }
            if (filter.date() != null) {
                predicates.add(criteriaBuilder.equal(root.get("date"), filter.date()));
            }
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), filter.to()));
            }
            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }
            if (filter.method() != null) {
                predicates.add(criteriaBuilder.equal(root.get("method"), filter.method()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void requireAttendanceListAccess() {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN || role == User.Role.BRANCH_MANAGER) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireAttendanceReadAccess(EmployeeFacade.EmployeeScope scope) {
        accessControlService.requireEmployeeSelfOrAdmin(
                accessControlService.currentUser(),
                scope.userId(),
                scope.companyId(),
                scope.branchId()
        );
    }

    private void requireAttendanceWriteAccess(EmployeeFacade.EmployeeScope scope) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.EMPLOYEE) {
            if (scope.userId() == null || !scope.userId().equals(principal.userId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        accessControlService.requireBranchAccess(principal, scope.companyId(), scope.branchId());
    }

    private void requireAttendanceAdminAccess(EmployeeFacade.EmployeeScope scope) {
        accessControlService.requireBranchAccess(accessControlService.currentUser(), scope.companyId(), scope.branchId());
    }
}
