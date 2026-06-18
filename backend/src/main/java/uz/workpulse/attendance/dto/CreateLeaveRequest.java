package uz.workpulse.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import uz.workpulse.attendance.domain.Leave;

public record CreateLeaveRequest(
        @NotNull java.util.UUID employeeId,
        @NotNull Leave.LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {
}
