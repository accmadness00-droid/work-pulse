package uz.workpulse.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdatePayrollAdjustmentRequest(
        @DecimalMin(value = "0.00")
        BigDecimal bonusAmount,

        @DecimalMin(value = "0.00")
        BigDecimal penaltyAmount,

        @Size(max = 500)
        String note
) {
}
