package uz.workpulse.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "device_events")
public class DeviceEvent extends BaseEntity {

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "external_event_id")
    private String externalEventId;

    @Column(name = "event_hash", nullable = false, length = 128)
    private String eventHash;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "credential_value")
    private String credentialValue;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction = Direction.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    private AuthType authType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "processing_error")
    private String processingError;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    protected DeviceEvent() {
    }

    public DeviceEvent(UUID deviceId, String externalEventId, String eventHash, Instant eventTime, Direction direction) {
        this.deviceId = deviceId;
        this.externalEventId = externalEventId;
        this.eventHash = eventHash;
        this.eventTime = eventTime;
        this.direction = direction == null ? Direction.UNKNOWN : direction;
        this.processed = false;
        this.retryCount = 0;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public String getEventHash() {
        return eventHash;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getCredentialValue() {
        return credentialValue;
    }

    public void setCredentialValue(String credentialValue) {
        this.credentialValue = credentialValue;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Direction getDirection() {
        return direction;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void markProcessed() {
        this.processed = true;
        this.processingError = null;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
        this.processed = false;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public enum Direction {
        IN,
        OUT,
        UNKNOWN
    }

    public enum AuthType {
        CARD,
        FACE,
        FINGERPRINT,
        QR
    }
}
