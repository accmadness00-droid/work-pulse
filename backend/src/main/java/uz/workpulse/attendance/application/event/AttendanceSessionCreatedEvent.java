package uz.workpulse.attendance.application.event;

import java.time.LocalDate;
import java.util.UUID;

public record AttendanceSessionCreatedEvent(
        UUID sessionId,
        UUID employeeId,
        UUID branchId,
        LocalDate date
) {
}
