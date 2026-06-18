package uz.workpulse.attendance.dto;

import jakarta.validation.constraints.NotNull;
import uz.workpulse.attendance.domain.Leave;

public record ReviewLeaveRequest(
        @NotNull Leave.Status status
) {
}
