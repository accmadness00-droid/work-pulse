package uz.workpulse.face.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import uz.workpulse.face.domain.EmployeeFaceProfile;

public class EmployeeFaceProfileResponse {
    private UUID id;
    private UUID employeeId;
    private String photoUrl;
    private String modelName;
    private boolean active;
    private LocalDateTime createdAt;

    public static EmployeeFaceProfileResponse from(EmployeeFaceProfile profile) {
        EmployeeFaceProfileResponse r = new EmployeeFaceProfileResponse();
        r.id = profile.getId();
        r.employeeId = profile.getEmployeeId();
        r.photoUrl = profile.getPhotoUrl();
        r.modelName = profile.getModelName();
        r.active = profile.isActive();
        r.createdAt = profile.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getModelName() { return modelName; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
