package uz.workpulse.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "leaves")
public class Leave extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private String reason;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    protected Leave() {
    }

    public Leave(UUID employeeId, LocalDate startDate, LocalDate endDate, LeaveType type, String reason) {
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
        this.status = Status.PENDING;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LeaveType getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public void approve(UUID reviewerId) {
        this.status = Status.APPROVED;
        this.reviewedBy = reviewerId;
    }

    public void reject(UUID reviewerId) {
        this.status = Status.REJECTED;
        this.reviewedBy = reviewerId;
    }

    public enum LeaveType {
        ANNUAL,
        SICK,
        UNPAID,
        OTHER
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
