package uz.workpulse.payroll.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollAdjustmentResponse(
        UUID employeeId,
        int year,
        int month,
        BigDecimal bonusAmount,
        BigDecimal penaltyAmount,
        String note
) {
}
