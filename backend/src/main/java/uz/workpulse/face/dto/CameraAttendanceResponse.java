package uz.workpulse.face.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CameraAttendanceResponse {
    private UUID attendanceId;
    private UUID employeeId;
    private UUID branchId;
    private String action;
    private String status;
    private String method;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Integer workMinutes;
    private boolean faceVerified;
    private double faceDistance;
    private boolean locationVerified;
    private String photoUrl;

    public UUID getAttendanceId() { return attendanceId; }
    public void setAttendanceId(UUID attendanceId) { this.attendanceId = attendanceId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public Integer getWorkMinutes() { return workMinutes; }
    public void setWorkMinutes(Integer workMinutes) { this.workMinutes = workMinutes; }
    public boolean isFaceVerified() { return faceVerified; }
    public void setFaceVerified(boolean faceVerified) { this.faceVerified = faceVerified; }
    public double getFaceDistance() { return faceDistance; }
    public void setFaceDistance(double faceDistance) { this.faceDistance = faceDistance; }
    public boolean isLocationVerified() { return locationVerified; }
    public void setLocationVerified(boolean locationVerified) { this.locationVerified = locationVerified; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
