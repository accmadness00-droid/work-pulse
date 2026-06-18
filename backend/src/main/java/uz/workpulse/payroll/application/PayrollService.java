package uz.workpulse.payroll.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.branch.domain.BranchSchedule;
import uz.workpulse.branch.infrastructure.BranchScheduleRepository;
import uz.workpulse.company.application.CompanyFacade;
import uz.workpulse.company.domain.CompanySettings;
import uz.workpulse.company.infrastructure.CompanySettingsRepository;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.domain.EmployeeSchedule;
import uz.workpulse.employee.infrastructure.EmployeeRepository;
import uz.workpulse.employee.infrastructure.EmployeeScheduleRepository;
import uz.workpulse.payroll.domain.PayrollAdjustment;
import uz.workpulse.payroll.dto.PayrollAdjustmentResponse;
import uz.workpulse.payroll.dto.PayrollEmployeeRow;
import uz.workpulse.payroll.dto.PayrollPolicyResponse;
import uz.workpulse.payroll.dto.PayrollResponse;
import uz.workpulse.payroll.dto.PayrollSummaryResponse;
import uz.workpulse.payroll.dto.UpdatePayrollAdjustmentRequest;
import uz.workpulse.payroll.infrastructure.PayrollAdjustmentRepository;
import uz.workpulse.payroll.infrastructure.PayrollReadRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;

@Service
public class PayrollService {

    private final PayrollReadRepository payrollReadRepository;
    private final PayrollAdjustmentRepository adjustmentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeScheduleRepository employeeScheduleRepository;
    private final BranchScheduleRepository branchScheduleRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final CompanyFacade companyFacade;
    private final BranchFacade branchFacade;
    private final AccessControlService accessControlService;

    public PayrollService(
            PayrollReadRepository payrollReadRepository,
            PayrollAdjustmentRepository adjustmentRepository,
            EmployeeRepository employeeRepository,
            EmployeeScheduleRepository employeeScheduleRepository,
            BranchScheduleRepository branchScheduleRepository,
            CompanySettingsRepository companySettingsRepository,
            CompanyFacade companyFacade,
            BranchFacade branchFacade,
            AccessControlService accessControlService
    ) {
        this.payrollReadRepository = payrollReadRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.employeeRepository = employeeRepository;
        this.employeeScheduleRepository = employeeScheduleRepository;
        this.branchScheduleRepository = branchScheduleRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.companyFacade = companyFacade;
        this.branchFacade = branchFacade;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public PayrollResponse calculateMonthlyPayroll(UUID companyId, UUID branchId, int year, int month) {
        requirePayrollAccess(companyId, branchId);
        YearMonth yearMonth = validateYearMonth(year, month);
        CompanySettings settings = companySettingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Company settings not found"));
        PayrollPolicyResponse policy = toPolicy(settings);

        List<PayrollReadRepository.PayrollEmployeeProjection> payrollRows = payrollReadRepository.findEmployeePayrollRows(
                        companyId,
                        branchId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth()
                );

        List<PayrollEmployeeRow> rows = payrollRows.stream()
                .map(row -> toPayrollRow(row, settings, yearMonth))
                .toList();

        return new PayrollResponse(companyId, branchId, year, month, policy, summarize(rows), rows);
    }

    @Transactional
    public PayrollAdjustmentResponse updateAdjustment(UUID employeeId, int year, int month, UpdatePayrollAdjustmentRequest request) {
        YearMonth yearMonth = validateYearMonth(year, month);
        Employee employee = employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        requirePayrollAccess(employee.getCompanyId(), employee.getBranchId());

        PayrollAdjustment adjustment = adjustmentRepository.findByEmployeeIdAndYearAndMonth(
                        employeeId,
                        yearMonth.getYear(),
                        yearMonth.getMonthValue()
                )
                .orElseGet(() -> new PayrollAdjustment(employeeId, yearMonth.getYear(), yearMonth.getMonthValue()));
        adjustment.setBonusAmount(nonNegativeMoney(request.bonusAmount()));
        adjustment.setPenaltyAmount(nonNegativeMoney(request.penaltyAmount()));
        adjustment.setNote(request.note());
        PayrollAdjustment saved = adjustmentRepository.save(adjustment);
        return toAdjustmentResponse(saved);
    }

    private PayrollEmployeeRow toPayrollRow(
            PayrollReadRepository.PayrollEmployeeProjection row,
            CompanySettings settings,
            YearMonth yearMonth
    ) {
        BigDecimal baseSalary = money(row.getBaseSalary());
        int expectedWorkMinutes = expectedWorkMinutes(row.getEmployeeId(), row.getBranchId(), yearMonth);
        int actualWorkMinutes = number(row.getActualWorkMinutes());
        int lateMinutes = number(row.getLateMinutes());
        int overtimeMinutes = Math.max(actualWorkMinutes - expectedWorkMinutes, 0);
        BigDecimal attendanceRate = ratio(actualWorkMinutes, expectedWorkMinutes);
        BigDecimal grossPay = money(baseSalary.multiply(attendanceRate));
        BigDecimal hourlyRate = hourlyRate(baseSalary, expectedWorkMinutes);
        BigDecimal automaticBonusAmount = automaticOvertimeBonus(overtimeMinutes, hourlyRate, settings);
        BigDecimal automaticPenaltyAmount = automaticLatePenalty(lateMinutes, hourlyRate, settings);
        BigDecimal manualBonusAmount = money(BigDecimal.ZERO);
        BigDecimal manualPenaltyAmount = money(BigDecimal.ZERO);
        BigDecimal bonusAmount = money(automaticBonusAmount.add(manualBonusAmount));
        BigDecimal penaltyAmount = money(automaticPenaltyAmount.add(manualPenaltyAmount));
        BigDecimal deductions = penaltyAmount;
        BigDecimal netPay = grossPay.add(bonusAmount).subtract(deductions).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal attendanceAdjustmentAmount = money(grossPay.subtract(baseSalary));
        BigDecimal netDifferenceAmount = money(netPay.subtract(baseSalary));
        BigDecimal increaseAmount = netDifferenceAmount.signum() > 0 ? netDifferenceAmount : money(BigDecimal.ZERO);
        BigDecimal decreaseAmount = netDifferenceAmount.signum() < 0 ? money(netDifferenceAmount.abs()) : money(BigDecimal.ZERO);
        String explanation = explanation(
                expectedWorkMinutes,
                actualWorkMinutes,
                attendanceAdjustmentAmount,
                automaticBonusAmount,
                automaticPenaltyAmount,
                manualBonusAmount,
                manualPenaltyAmount,
                netDifferenceAmount
        );

        return new PayrollEmployeeRow(
                row.getEmployeeId(),
                row.getBranchId(),
                row.getEmployeeCode(),
                row.getFirstName(),
                row.getLastName(),
                row.getPosition(),
                baseSalary,
                expectedWorkMinutes,
                actualWorkMinutes,
                lateMinutes,
                overtimeMinutes,
                number(row.getWorkedDays()),
                attendanceRate,
                grossPay,
                attendanceAdjustmentAmount,
                automaticBonusAmount,
                automaticPenaltyAmount,
                manualBonusAmount,
                manualPenaltyAmount,
                bonusAmount,
                penaltyAmount,
                deductions,
                netPay,
                netDifferenceAmount,
                increaseAmount,
                decreaseAmount,
                explanation,
                null
        );
    }

    private PayrollSummaryResponse summarize(List<PayrollEmployeeRow> rows) {
        int expectedWorkMinutes = rows.stream().mapToInt(PayrollEmployeeRow::expectedWorkMinutes).sum();
        int actualWorkMinutes = rows.stream().mapToInt(PayrollEmployeeRow::actualWorkMinutes).sum();
        int lateMinutes = rows.stream().mapToInt(PayrollEmployeeRow::lateMinutes).sum();
        BigDecimal totalBaseSalary = rows.stream().map(PayrollEmployeeRow::baseSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalGrossPay = rows.stream().map(PayrollEmployeeRow::grossPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBonusAmount = rows.stream().map(PayrollEmployeeRow::bonusAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPenaltyAmount = rows.stream().map(PayrollEmployeeRow::penaltyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductions = rows.stream().map(PayrollEmployeeRow::deductions).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetPay = rows.stream().map(PayrollEmployeeRow::netPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetDifferenceAmount = rows.stream().map(PayrollEmployeeRow::netDifferenceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncreaseAmount = rows.stream().map(PayrollEmployeeRow::increaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDecreaseAmount = rows.stream().map(PayrollEmployeeRow::decreaseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PayrollSummaryResponse(
                rows.size(),
                expectedWorkMinutes,
                actualWorkMinutes,
                lateMinutes,
                money(totalBaseSalary),
                money(totalGrossPay),
                money(totalBonusAmount),
                money(totalPenaltyAmount),
                money(totalDeductions),
                money(totalNetPay),
                money(totalNetDifferenceAmount),
                money(totalIncreaseAmount),
                money(totalDecreaseAmount)
        );
    }

    private String explanation(
            int expectedWorkMinutes,
            int actualWorkMinutes,
            BigDecimal attendanceAdjustmentAmount,
            BigDecimal automaticBonusAmount,
            BigDecimal automaticPenaltyAmount,
            BigDecimal manualBonusAmount,
            BigDecimal manualPenaltyAmount,
            BigDecimal netDifferenceAmount
    ) {
        List<String> reasons = new java.util.ArrayList<>();
        if (actualWorkMinutes > expectedWorkMinutes) {
            reasons.add("Worked " + formatMinutes(actualWorkMinutes - expectedWorkMinutes) + " more than schedule");
        } else if (actualWorkMinutes < expectedWorkMinutes) {
            reasons.add("Worked " + formatMinutes(expectedWorkMinutes - actualWorkMinutes) + " less than schedule");
        } else {
            reasons.add("Worked exactly the scheduled time");
        }
        if (attendanceAdjustmentAmount.signum() != 0) {
            reasons.add("Attendance changed base salary by " + money(attendanceAdjustmentAmount));
        }
        if (automaticBonusAmount.signum() > 0) {
            reasons.add("Automatic overtime bonus added " + money(automaticBonusAmount));
        }
        if (automaticPenaltyAmount.signum() > 0) {
            reasons.add("Automatic late penalty deducted " + money(automaticPenaltyAmount));
        }
        if (manualBonusAmount.signum() > 0) {
            reasons.add("Manual bonus added " + money(manualBonusAmount));
        }
        if (manualPenaltyAmount.signum() > 0) {
            reasons.add("Manual penalty deducted " + money(manualPenaltyAmount));
        }
        if (netDifferenceAmount.signum() > 0) {
            reasons.add("Final salary increased by " + money(netDifferenceAmount));
        } else if (netDifferenceAmount.signum() < 0) {
            reasons.add("Final salary decreased by " + money(netDifferenceAmount.abs()));
        } else {
            reasons.add("Final salary is unchanged");
        }
        return String.join(". ", reasons);
    }

    private String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + "h " + minutes + "m";
    }

    private void requirePayrollAccess(UUID companyId, UUID branchId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "companyId is required");
        }
        companyFacade.ensureActiveCompany(companyId);
        if (branchId != null) {
            UUID branchCompanyId = branchFacade.getCompanyId(branchId);
            if (!companyId.equals(branchCompanyId)) {
                throw new BusinessException(ErrorCode.EMPLOYEE_BRANCH_COMPANY_MISMATCH);
            }
            branchFacade.ensureActiveBranch(branchId);
            accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, branchId);
            return;
        }
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(accessControlService.currentUser(), companyId);
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private YearMonth validateYearMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.REPORT_INVALID_MONTH);
        }
        return YearMonth.of(year, month);
    }

    private int expectedWorkMinutes(UUID employeeId, UUID branchId, YearMonth yearMonth) {
        Map<Short, EmployeeSchedule> employeeSchedules = employeeScheduleRepository.findAllByEmployeeIdOrderByDayOfWeek(employeeId).stream()
                .collect(Collectors.toMap(EmployeeSchedule::getDayOfWeek, Function.identity()));
        Map<Short, BranchSchedule> branchSchedules = branchId == null
                ? Map.of()
                : branchScheduleRepository.findAllByBranchIdOrderByDayOfWeek(branchId).stream()
                        .collect(Collectors.toMap(BranchSchedule::getDayOfWeek, Function.identity()));
        int minutes = 0;
        for (LocalDate date = yearMonth.atDay(1); !date.isAfter(yearMonth.atEndOfMonth()); date = date.plusDays(1)) {
            short day = (short) date.getDayOfWeek().getValue();
            EmployeeSchedule employeeSchedule = employeeSchedules.get(day);
            if (employeeSchedule != null) {
                minutes += employeeSchedule.isWorkday()
                        ? minutesBetween(employeeSchedule.getStartTime(), employeeSchedule.getEndTime())
                        : 0;
                continue;
            }
            BranchSchedule branchSchedule = branchSchedules.get(day);
            minutes += branchSchedule != null && branchSchedule.isWorkday()
                    ? minutesBetween(branchSchedule.getStartTime(), branchSchedule.getEndTime())
                    : 0;
        }
        return minutes;
    }

    private int minutesBetween(java.time.LocalTime startTime, java.time.LocalTime endTime) {
        return (int) Math.max(0, Duration.between(startTime, endTime).toMinutes());
    }

    private BigDecimal ratio(int actualWorkMinutes, int expectedWorkMinutes) {
        if (expectedWorkMinutes <= 0 || actualWorkMinutes <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(actualWorkMinutes)
                .divide(BigDecimal.valueOf(expectedWorkMinutes), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal hourlyRate(BigDecimal baseSalary, int expectedWorkMinutes) {
        if (expectedWorkMinutes <= 0) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return baseSalary.divide(BigDecimal.valueOf(expectedWorkMinutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal automaticOvertimeBonus(int overtimeMinutes, BigDecimal hourlyRate, CompanySettings settings) {
        if (!settings.isPayrollOvertimeBonusEnabled() || overtimeMinutes <= 0) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal premiumMultiplier = settings.getPayrollOvertimeMultiplier()
                .subtract(BigDecimal.ONE)
                .max(BigDecimal.ZERO);
        BigDecimal overtimeHours = BigDecimal.valueOf(overtimeMinutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return money(overtimeHours.multiply(hourlyRate).multiply(premiumMultiplier));
    }

    private BigDecimal automaticLatePenalty(int lateMinutes, BigDecimal hourlyRate, CompanySettings settings) {
        if (!settings.isPayrollLatePenaltyEnabled() || lateMinutes <= 0) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal lateHours = BigDecimal.valueOf(lateMinutes).divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return money(lateHours.multiply(hourlyRate).multiply(settings.getPayrollLatePenaltyMultiplier()));
    }

    private PayrollPolicyResponse toPolicy(CompanySettings settings) {
        return new PayrollPolicyResponse(
                settings.isPayrollOvertimeBonusEnabled(),
                settings.isPayrollLatePenaltyEnabled(),
                settings.getPayrollOvertimeMultiplier(),
                settings.getPayrollLatePenaltyMultiplier()
        );
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegativeMoney(BigDecimal value) {
        BigDecimal amount = money(value);
        if (amount.signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "amount must be positive");
        }
        return amount;
    }

    private PayrollAdjustmentResponse toAdjustmentResponse(PayrollAdjustment adjustment) {
        return new PayrollAdjustmentResponse(
                adjustment.getEmployeeId(),
                adjustment.getYear(),
                adjustment.getMonth(),
                money(adjustment.getBonusAmount()),
                money(adjustment.getPenaltyAmount()),
                adjustment.getNote()
        );
    }

    private int number(Number number) {
        return number == null ? 0 : number.intValue();
    }
}
