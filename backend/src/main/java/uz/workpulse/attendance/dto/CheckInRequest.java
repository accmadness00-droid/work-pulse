package uz.workpulse.attendance.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import uz.workpulse.attendance.domain.AttendanceSession;

public record CheckInRequest(
        @NotNull UUID employeeId,
        @NotNull UUID branchId,
        BigDecimal latitude,
        BigDecimal longitude,
        AttendanceSession.Method method,
        String note
) {
}
