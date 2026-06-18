package uz.workpulse.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.attendance.domain.Leave;

public record LeaveResponse(
        UUID id,
        UUID employeeId,
        Leave.LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        Leave.Status status,
        String reason,
        UUID reviewedBy,
        Instant createdAt
) {

    public static LeaveResponse from(Leave leave) {
        return new LeaveResponse(
                leave.getId(),
                leave.getEmployeeId(),
                leave.getType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getStatus(),
                leave.getReason(),
                leave.getReviewedBy(),
                leave.getCreatedAt()
        );
    }
}
