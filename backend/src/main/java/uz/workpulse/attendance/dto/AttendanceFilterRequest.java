package uz.workpulse.attendance.dto;

import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.attendance.domain.AttendanceSession;

public record AttendanceFilterRequest(
        UUID employeeId,
        UUID branchId,
        LocalDate date,
        LocalDate from,
        LocalDate to,
        AttendanceSession.Status status,
        AttendanceSession.Method method
) {
}
