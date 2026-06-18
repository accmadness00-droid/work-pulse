package uz.workpulse.company.dto;

import java.math.BigDecimal;
import java.util.UUID;
import uz.workpulse.company.domain.CompanySettings;

public record CompanySettingsResponse(
        UUID id,
        UUID companyId,
        String timezone,
        String locale,
        String plan,
        boolean payrollOvertimeBonusEnabled,
        boolean payrollLatePenaltyEnabled,
        BigDecimal payrollOvertimeMultiplier,
        BigDecimal payrollLatePenaltyMultiplier
) {

    public static CompanySettingsResponse from(CompanySettings settings, String plan) {
        return new CompanySettingsResponse(
                settings.getId(),
                settings.getCompanyId(),
                settings.getTimezone(),
                settings.getLocale(),
                plan,
                settings.isPayrollOvertimeBonusEnabled(),
                settings.isPayrollLatePenaltyEnabled(),
                settings.getPayrollOvertimeMultiplier(),
                settings.getPayrollLatePenaltyMultiplier()
        );
    }
}
