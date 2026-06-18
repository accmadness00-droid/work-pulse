package uz.workpulse.attendance.application;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.attendance.domain.Leave;
import uz.workpulse.attendance.dto.CreateLeaveRequest;
import uz.workpulse.attendance.dto.LeaveResponse;
import uz.workpulse.attendance.dto.ReviewLeaveRequest;
import uz.workpulse.attendance.infrastructure.LeaveRepository;
import uz.workpulse.auth.domain.User;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;

    public LeaveService(
            LeaveRepository leaveRepository,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService
    ) {
        this.leaveRepository = leaveRepository;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public LeaveResponse create(CreateLeaveRequest request) {
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(request.employeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        requireLeaveWriteAccess(scope);
        validateDateRange(request.startDate(), request.endDate());

        Leave leave = new Leave(request.employeeId(), request.startDate(), request.endDate(), request.type(), request.reason());
        return LeaveResponse.from(leaveRepository.save(leave));
    }

    @Transactional
    public LeaveResponse review(UUID leaveId, ReviewLeaveRequest request) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(leave.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        requireLeaveAdminAccess(scope);

        if (leave.getStatus() != Leave.Status.PENDING) {
            throw new BusinessException(ErrorCode.LEAVE_ALREADY_REVIEWED);
        }
        if (request.status() == Leave.Status.PENDING) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Review status must be APPROVED or REJECTED");
        }

        UUID reviewerId = accessControlService.currentUser().userId();
        if (request.status() == Leave.Status.APPROVED) {
            leave.approve(reviewerId);
        } else {
            leave.reject(reviewerId);
        }
        return LeaveResponse.from(leaveRepository.save(leave));
    }

    @Transactional(readOnly = true)
    public LeaveResponse getById(UUID id) {
        Leave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LEAVE_NOT_FOUND));
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(leave.getEmployeeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        requireLeaveReadAccess(scope);
        return LeaveResponse.from(leave);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.LEAVE_INVALID_DATE_RANGE);
        }
    }

    private void requireLeaveWriteAccess(EmployeeFacade.EmployeeScope scope) {
        if (accessControlService.currentUser().role() == User.Role.EMPLOYEE) {
            if (scope.userId() == null || !scope.userId().equals(accessControlService.currentUser().userId())) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return;
        }
        accessControlService.requireBranchAccess(accessControlService.currentUser(), scope.companyId(), scope.branchId());
    }

    private void requireLeaveReadAccess(EmployeeFacade.EmployeeScope scope) {
        requireLeaveWriteAccess(scope);
    }

    private void requireLeaveAdminAccess(EmployeeFacade.EmployeeScope scope) {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN || role == User.Role.BRANCH_MANAGER) {
            accessControlService.requireBranchAccess(accessControlService.currentUser(), scope.companyId(), scope.branchId());
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
