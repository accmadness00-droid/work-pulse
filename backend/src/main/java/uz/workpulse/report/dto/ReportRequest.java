package uz.workpulse.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReportRequest(
        UUID companyId,
        UUID branchId,
        UUID employeeId,
        LocalDate from,
        LocalDate to,
        LocalDate date,
        Integer month,
        Integer year
) {
}
