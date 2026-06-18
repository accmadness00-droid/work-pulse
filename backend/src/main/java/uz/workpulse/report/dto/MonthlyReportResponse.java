package uz.workpulse.report.dto;

import java.util.UUID;

public record MonthlyReportResponse(
        UUID companyId,
        UUID branchId,
        int year,
        int month,
        AttendanceSummaryResponse summary
) {
}
