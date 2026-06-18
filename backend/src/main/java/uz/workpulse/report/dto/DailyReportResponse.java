package uz.workpulse.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record DailyReportResponse(
        UUID companyId,
        UUID branchId,
        LocalDate date,
        AttendanceSummaryResponse summary
) {
}
