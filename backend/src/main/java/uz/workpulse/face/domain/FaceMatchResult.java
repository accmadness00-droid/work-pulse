package uz.workpulse.face.domain;

import java.util.UUID;

public class FaceMatchResult {
    private final UUID employeeId;
    private final UUID faceProfileId;
    private final double distance;
    private final String modelName;

    public FaceMatchResult(UUID employeeId, UUID faceProfileId, double distance, String modelName) {
        this.employeeId = employeeId;
        this.faceProfileId = faceProfileId;
        this.distance = distance;
        this.modelName = modelName;
    }

    public UUID getEmployeeId() { return employeeId; }
    public UUID getFaceProfileId() { return faceProfileId; }
    public double getDistance() { return distance; }
    public String getModelName() { return modelName; }
}
