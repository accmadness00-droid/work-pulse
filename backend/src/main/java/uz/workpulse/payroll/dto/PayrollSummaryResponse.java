package uz.workpulse.payroll.dto;

import java.math.BigDecimal;

public record PayrollSummaryResponse(
        int employeeCount,
        int expectedWorkMinutes,
        int actualWorkMinutes,
        int lateMinutes,
        BigDecimal totalBaseSalary,
        BigDecimal totalGrossPay,
        BigDecimal totalBonusAmount,
        BigDecimal totalPenaltyAmount,
        BigDecimal totalDeductions,
        BigDecimal totalNetPay,
        BigDecimal totalNetDifferenceAmount,
        BigDecimal totalIncreaseAmount,
        BigDecimal totalDecreaseAmount
) {
}
