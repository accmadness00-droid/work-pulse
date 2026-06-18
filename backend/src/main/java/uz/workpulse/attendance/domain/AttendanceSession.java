package uz.workpulse.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "check_in_lat", precision = 9, scale = 6)
    private BigDecimal checkInLat;

    @Column(name = "check_in_lng", precision = 9, scale = 6)
    private BigDecimal checkInLng;

    @Column(name = "check_out_lat", precision = 9, scale = 6)
    private BigDecimal checkOutLat;

    @Column(name = "check_out_lng", precision = 9, scale = 6)
    private BigDecimal checkOutLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PRESENT;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes = 0;

    @Column(name = "work_minutes", nullable = false)
    private int workMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;

    @Column(name = "source_device_id")
    private UUID sourceDeviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType = SessionType.REGULAR;

    @Column(name = "check_in_photo_url", length = 1024)
    private String checkInPhotoUrl;

    @Column(name = "check_out_photo_url", length = 1024)
    private String checkOutPhotoUrl;

    @Column(name = "check_in_location_verified")
    private Boolean checkInLocationVerified = false;

    @Column(name = "check_out_location_verified")
    private Boolean checkOutLocationVerified = false;

    @Column(name = "check_in_face_verified")
    private Boolean checkInFaceVerified = false;

    @Column(name = "check_out_face_verified")
    private Boolean checkOutFaceVerified = false;

    @Column(name = "check_in_face_distance", precision = 8, scale = 5)
    private BigDecimal checkInFaceDistance;

    @Column(name = "check_out_face_distance", precision = 8, scale = 5)
    private BigDecimal checkOutFaceDistance;

    private String note;

    protected AttendanceSession() {
    }

    public AttendanceSession(UUID employeeId, UUID branchId, LocalDate date, Instant checkInTime, Method method) {
        this.employeeId = employeeId;
        this.branchId = branchId;
        this.date = date;
        this.checkInTime = checkInTime;
        this.method = method;
        this.sessionType = SessionType.REGULAR;
        this.status = Status.PRESENT;
    }

    public enum Status {
        PRESENT,
        LATE,
        ABSENT,
        LEAVE,
        HOLIDAY
    }

    public enum Method {
        GPS,
        FACE_ID,
        MANUAL,
        PIN,
        DEVICE,

        WEB_CAMERA
    }

    public enum SessionType {
        REGULAR,
        OVERTIME,
        BREAK
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public BigDecimal getCheckInLat() {
        return checkInLat;
    }

    public void setCheckInLat(BigDecimal checkInLat) {
        this.checkInLat = checkInLat;
    }

    public BigDecimal getCheckInLng() {
        return checkInLng;
    }

    public void setCheckInLng(BigDecimal checkInLng) {
        this.checkInLng = checkInLng;
    }

    public BigDecimal getCheckOutLat() {
        return checkOutLat;
    }

    public void setCheckOutLat(BigDecimal checkOutLat) {
        this.checkOutLat = checkOutLat;
    }

    public BigDecimal getCheckOutLng() {
        return checkOutLng;
    }

    public void setCheckOutLng(BigDecimal checkOutLng) {
        this.checkOutLng = checkOutLng;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(int lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public int getWorkMinutes() {
        return workMinutes;
    }

    public void setWorkMinutes(int workMinutes) {
        this.workMinutes = workMinutes;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public UUID getSourceDeviceId() {
        return sourceDeviceId;
    }

    public void setSourceDeviceId(UUID sourceDeviceId) {
        this.sourceDeviceId = sourceDeviceId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getCheckInPhotoUrl() {
        return checkInPhotoUrl;
    }

    public void setCheckInPhotoUrl(String checkInPhotoUrl) {
        this.checkInPhotoUrl = checkInPhotoUrl;
    }

    public String getCheckOutPhotoUrl() {
        return checkOutPhotoUrl;
    }

    public void setCheckOutPhotoUrl(String checkOutPhotoUrl) {
        this.checkOutPhotoUrl = checkOutPhotoUrl;
    }

    public Boolean getCheckInLocationVerified() {
        return checkInLocationVerified;
    }

    public void setCheckInLocationVerified(Boolean checkInLocationVerified) {
        this.checkInLocationVerified = checkInLocationVerified;
    }

    public Boolean getCheckOutLocationVerified() {
        return checkOutLocationVerified;
    }

    public void setCheckOutLocationVerified(Boolean checkOutLocationVerified) {
        this.checkOutLocationVerified = checkOutLocationVerified;
    }

    public Boolean getCheckInFaceVerified() {
        return checkInFaceVerified;
    }

    public void setCheckInFaceVerified(Boolean checkInFaceVerified) {
        this.checkInFaceVerified = checkInFaceVerified;
    }

    public Boolean getCheckOutFaceVerified() {
        return checkOutFaceVerified;
    }

    public void setCheckOutFaceVerified(Boolean checkOutFaceVerified) {
        this.checkOutFaceVerified = checkOutFaceVerified;
    }

    public BigDecimal getCheckInFaceDistance() {
        return checkInFaceDistance;
    }

    public void setCheckInFaceDistance(BigDecimal checkInFaceDistance) {
        this.checkInFaceDistance = checkInFaceDistance;
    }

    public BigDecimal getCheckOutFaceDistance() {
        return checkOutFaceDistance;
    }

    public void setCheckOutFaceDistance(BigDecimal checkOutFaceDistance) {
        this.checkOutFaceDistance = checkOutFaceDistance;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
