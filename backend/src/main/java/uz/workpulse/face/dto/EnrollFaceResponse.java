package uz.workpulse.face.dto;

import java.util.UUID;

public class EnrollFaceResponse {
    private UUID employeeId;
    private UUID faceProfileId;
    private String photoUrl;
    private String modelName;
    private boolean active;

    public EnrollFaceResponse(UUID employeeId, UUID faceProfileId, String photoUrl, String modelName, boolean active) {
        this.employeeId = employeeId;
        this.faceProfileId = faceProfileId;
        this.photoUrl = photoUrl;
        this.modelName = modelName;
        this.active = active;
    }

    public UUID getEmployeeId() { return employeeId; }
    public UUID getFaceProfileId() { return faceProfileId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getModelName() { return modelName; }
    public boolean isActive() { return active; }
}
