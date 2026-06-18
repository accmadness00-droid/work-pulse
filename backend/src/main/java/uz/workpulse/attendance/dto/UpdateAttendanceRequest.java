package uz.workpulse.attendance.dto;

import java.time.Instant;
import uz.workpulse.attendance.domain.AttendanceSession;

public record UpdateAttendanceRequest(
        Instant checkInTime,
        Instant checkOutTime,
        AttendanceSession.Status status,
        String note
) {
}
