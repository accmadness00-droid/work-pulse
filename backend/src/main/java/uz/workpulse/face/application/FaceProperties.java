package uz.workpulse.face.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.face")
public class FaceProperties {
    private String serviceUrl = "http://localhost:5001";
    private double threshold = 0.45;
    private double maxLocationAccuracyMeters = 100;

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public double getMaxLocationAccuracyMeters() { return maxLocationAccuracyMeters; }
    public void setMaxLocationAccuracyMeters(double maxLocationAccuracyMeters) { this.maxLocationAccuracyMeters = maxLocationAccuracyMeters; }
}
