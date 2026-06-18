package uz.workpulse.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReportExportRequest(
        ExportType type,
        UUID companyId,
        UUID branchId,
        UUID employeeId,
        LocalDate from,
        LocalDate to,
        LocalDate date,
        Integer year,
        Integer month
) {

    public enum ExportType {
        DAILY,
        MONTHLY,
        EMPLOYEE,
        BRANCH
    }
}
