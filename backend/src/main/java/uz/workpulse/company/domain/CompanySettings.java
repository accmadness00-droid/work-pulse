package uz.workpulse.company.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "company_settings")
public class CompanySettings extends BaseEntity {

    @Column(name = "company_id", nullable = false, unique = true)
    private UUID companyId;

    @Column(nullable = false)
    private String timezone = "Asia/Tashkent";

    @Column(nullable = false)
    private String locale = "uz-UZ";

    @Column(name = "payroll_overtime_bonus_enabled", nullable = false)
    private boolean payrollOvertimeBonusEnabled = false;

    @Column(name = "payroll_late_penalty_enabled", nullable = false)
    private boolean payrollLatePenaltyEnabled = false;

    @Column(name = "payroll_overtime_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal payrollOvertimeMultiplier = new BigDecimal("1.50");

    @Column(name = "payroll_late_penalty_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal payrollLatePenaltyMultiplier = BigDecimal.ONE;

    protected CompanySettings() {
    }

    public CompanySettings(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isPayrollOvertimeBonusEnabled() {
        return payrollOvertimeBonusEnabled;
    }

    public void setPayrollOvertimeBonusEnabled(boolean payrollOvertimeBonusEnabled) {
        this.payrollOvertimeBonusEnabled = payrollOvertimeBonusEnabled;
    }

    public boolean isPayrollLatePenaltyEnabled() {
        return payrollLatePenaltyEnabled;
    }

    public void setPayrollLatePenaltyEnabled(boolean payrollLatePenaltyEnabled) {
        this.payrollLatePenaltyEnabled = payrollLatePenaltyEnabled;
    }

    public BigDecimal getPayrollOvertimeMultiplier() {
        return payrollOvertimeMultiplier;
    }

    public void setPayrollOvertimeMultiplier(BigDecimal payrollOvertimeMultiplier) {
        this.payrollOvertimeMultiplier = payrollOvertimeMultiplier;
    }

    public BigDecimal getPayrollLatePenaltyMultiplier() {
        return payrollLatePenaltyMultiplier;
    }

    public void setPayrollLatePenaltyMultiplier(BigDecimal payrollLatePenaltyMultiplier) {
        this.payrollLatePenaltyMultiplier = payrollLatePenaltyMultiplier;
    }
}
