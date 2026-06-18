package uz.workpulse.face.domain;

import java.util.List;

public class FaceEmbeddingResponse {
    private boolean success;
    private String modelName;
    private List<Double> embedding;
    private boolean faceDetected;
    private String error;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    public boolean isFaceDetected() { return faceDetected; }
    public void setFaceDetected(boolean faceDetected) { this.faceDetected = faceDetected; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
