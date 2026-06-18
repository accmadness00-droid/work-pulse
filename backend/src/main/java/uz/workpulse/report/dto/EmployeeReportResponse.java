package uz.workpulse.report.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EmployeeReportResponse(
        UUID employeeId,
        LocalDate from,
        LocalDate to,
        AttendanceSummaryResponse summary,
        List<ReportSessionRow> sessions
) {
}
