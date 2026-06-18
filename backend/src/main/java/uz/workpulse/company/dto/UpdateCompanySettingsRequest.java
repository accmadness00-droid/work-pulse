package uz.workpulse.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateCompanySettingsRequest(
        @NotBlank String timezone,
        @NotBlank String locale,
        @NotBlank String plan,
        Boolean payrollOvertimeBonusEnabled,
        Boolean payrollLatePenaltyEnabled,
        @DecimalMin(value = "1.00") BigDecimal payrollOvertimeMultiplier,
        @DecimalMin(value = "0.00") BigDecimal payrollLatePenaltyMultiplier
) {
}
