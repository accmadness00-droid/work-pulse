package uz.workpulse.report.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "report_snapshots")
public class ReportSnapshot extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType = PeriodType.DAILY;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_employees", nullable = false)
    private int totalEmployees = 0;

    @Column(name = "present_count", nullable = false)
    private int presentCount = 0;

    @Column(name = "late_count", nullable = false)
    private int lateCount = 0;

    @Column(name = "absent_count", nullable = false)
    private int absentCount = 0;

    @Column(name = "leave_count", nullable = false)
    private int leaveCount = 0;

    @Column(name = "avg_work_minutes", nullable = false)
    private int avgWorkMinutes = 0;

    @Column(name = "total_work_minutes", nullable = false)
    private int totalWorkMinutes = 0;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false)
    private GeneratedBy generatedBy = GeneratedBy.SCHEDULER;

    protected ReportSnapshot() {
    }

    public ReportSnapshot(UUID companyId, UUID branchId, PeriodType periodType, LocalDate periodStart, LocalDate periodEnd) {
        this.companyId = companyId;
        this.branchId = branchId;
        this.periodType = periodType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getLateCount() {
        return lateCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public int getLeaveCount() {
        return leaveCount;
    }

    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }

    public int getAvgWorkMinutes() {
        return avgWorkMinutes;
    }

    public void setAvgWorkMinutes(int avgWorkMinutes) {
        this.avgWorkMinutes = avgWorkMinutes;
    }

    public int getTotalWorkMinutes() {
        return totalWorkMinutes;
    }

    public void setTotalWorkMinutes(int totalWorkMinutes) {
        this.totalWorkMinutes = totalWorkMinutes;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public GeneratedBy getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(GeneratedBy generatedBy) {
        this.generatedBy = generatedBy;
    }

    public enum PeriodType {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    public enum GeneratedBy {
        SCHEDULER,
        MANUAL
    }
}
