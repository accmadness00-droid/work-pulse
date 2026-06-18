package uz.workpulse.payroll.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PayrollEmployeeRow(
        UUID employeeId,
        UUID branchId,
        String employeeCode,
        String firstName,
        String lastName,
        String position,
        BigDecimal baseSalary,
        int expectedWorkMinutes,
        int actualWorkMinutes,
        int lateMinutes,
        int overtimeMinutes,
        int workedDays,
        BigDecimal attendanceRate,
        BigDecimal grossPay,
        BigDecimal attendanceAdjustmentAmount,
        BigDecimal automaticBonusAmount,
        BigDecimal automaticPenaltyAmount,
        BigDecimal manualBonusAmount,
        BigDecimal manualPenaltyAmount,
        BigDecimal bonusAmount,
        BigDecimal penaltyAmount,
        BigDecimal deductions,
        BigDecimal netPay,
        BigDecimal netDifferenceAmount,
        BigDecimal increaseAmount,
        BigDecimal decreaseAmount,
        String explanation,
        String adjustmentNote
) {
}
