package uz.workpulse.branch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "branch_schedules")
public class BranchSchedule extends BaseEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "late_threshold_min", nullable = false)
    private int lateThresholdMin = 15;

    @Column(name = "is_workday", nullable = false)
    private boolean workday = true;

    protected BranchSchedule() {
    }

    public BranchSchedule(UUID branchId, short dayOfWeek, LocalTime startTime, LocalTime endTime, int lateThresholdMin, boolean workday) {
        this.branchId = branchId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lateThresholdMin = lateThresholdMin;
        this.workday = workday;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public short getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public int getLateThresholdMin() {
        return lateThresholdMin;
    }

    public void setLateThresholdMin(int lateThresholdMin) {
        this.lateThresholdMin = lateThresholdMin;
    }

    public boolean isWorkday() {
        return workday;
    }

    public void setWorkday(boolean workday) {
        this.workday = workday;
    }
}
