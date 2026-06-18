package uz.workpulse.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportSessionRow(
        UUID employeeId,
        UUID branchId,
        LocalDate date,
        Instant checkInTime,
        Instant checkOutTime,
        String status,
        int lateMinutes,
        int workMinutes,
        String method
) {
}
