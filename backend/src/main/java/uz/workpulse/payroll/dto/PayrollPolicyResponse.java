package uz.workpulse.payroll.dto;

import java.math.BigDecimal;

public record PayrollPolicyResponse(
        boolean overtimeBonusEnabled,
        boolean latePenaltyEnabled,
        BigDecimal overtimeMultiplier,
        BigDecimal latePenaltyMultiplier
) {
}
