package uz.workpulse.employee.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.employee.domain.EmployeeSchedule;
import uz.workpulse.employee.dto.EmployeeScheduleInfo;
import uz.workpulse.employee.dto.EmployeeScheduleResponse;
import uz.workpulse.employee.dto.UpdateEmployeeScheduleRequest;
import uz.workpulse.employee.infrastructure.EmployeeScheduleRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class EmployeeScheduleService implements EmployeeScheduleFacade {

    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;

    public EmployeeScheduleService(
            EmployeeScheduleRepository employeeScheduleRepository,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService
    ) {
        this.employeeScheduleRepository = employeeScheduleRepository;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public List<EmployeeScheduleResponse> getSchedule(UUID employeeId) {
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(employeeId);
        accessControlService.requireEmployeeSelfOrAdmin(
                accessControlService.currentUser(),
                scope.userId(),
                scope.companyId(),
                scope.branchId()
        );
        return employeeScheduleRepository.findAllByEmployeeIdOrderByDayOfWeek(employeeId).stream()
                .map(EmployeeScheduleResponse::from)
                .toList();
    }

    @Transactional
    public List<EmployeeScheduleResponse> updateSchedule(UUID employeeId, UpdateEmployeeScheduleRequest request) {
        EmployeeFacade.EmployeeScope scope = requireEmployeeScope(employeeId);
        requireScheduleWriteAccess(scope);

        for (UpdateEmployeeScheduleRequest.DaySchedule item : request.schedules()) {
            EmployeeSchedule schedule = employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, item.dayOfWeek())
                    .orElseGet(() -> new EmployeeSchedule(
                            employeeId,
                            item.dayOfWeek(),
                            item.startTime(),
                            item.endTime(),
                            item.lateThresholdMin(),
                            item.isWorkday()
                    ));
            schedule.setStartTime(item.startTime());
            schedule.setEndTime(item.endTime());
            schedule.setLateThresholdMin(item.lateThresholdMin());
            schedule.setWorkday(item.isWorkday());
            schedule.setNote(StringUtils.hasText(item.note()) ? item.note().trim() : null);
            employeeScheduleRepository.save(schedule);
        }

        return getSchedule(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeScheduleInfo> getScheduleForDate(UUID employeeId, LocalDate date) {
        if (employeeId == null || date == null) {
            return Optional.empty();
        }
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        return employeeScheduleRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek)
                .map(schedule -> new EmployeeScheduleInfo(
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        schedule.getLateThresholdMin(),
                        schedule.isWorkday()
                ));
    }

    private EmployeeFacade.EmployeeScope requireEmployeeScope(UUID employeeId) {
        return employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private void requireScheduleWriteAccess(EmployeeFacade.EmployeeScope scope) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.EMPLOYEE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        accessControlService.requireBranchAccess(principal, scope.companyId(), scope.branchId());
    }
}
