package uz.workpulse.payroll.dto;

import java.util.List;
import java.util.UUID;

public record PayrollResponse(
        UUID companyId,
        UUID branchId,
        int year,
        int month,
        PayrollPolicyResponse policy,
        PayrollSummaryResponse summary,
        List<PayrollEmployeeRow> rows
) {
}
