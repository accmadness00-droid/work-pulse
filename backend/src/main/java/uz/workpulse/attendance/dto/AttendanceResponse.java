package uz.workpulse.attendance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.attendance.domain.AttendanceSession;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        UUID branchId,
        LocalDate date,
        Instant checkInTime,
        Instant checkOutTime,
        BigDecimal checkInLat,
        BigDecimal checkInLng,
        BigDecimal checkOutLat,
        BigDecimal checkOutLng,
        AttendanceSession.Status status,
        int lateMinutes,
        int workMinutes,
        AttendanceSession.Method method,
        UUID sourceDeviceId,
        AttendanceSession.SessionType sessionType,
        String note
) {

    public static AttendanceResponse from(AttendanceSession session) {
        return new AttendanceResponse(
                session.getId(),
                session.getEmployeeId(),
                session.getBranchId(),
                session.getDate(),
                session.getCheckInTime(),
                session.getCheckOutTime(),
                session.getCheckInLat(),
                session.getCheckInLng(),
                session.getCheckOutLat(),
                session.getCheckOutLng(),
                session.getStatus(),
                session.getLateMinutes(),
                session.getWorkMinutes(),
                session.getMethod(),
                session.getSourceDeviceId(),
                session.getSessionType(),
                session.getNote()
        );
    }
}
