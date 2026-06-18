package uz.workpulse.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "devices")
public class Device extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "serial_number", unique = true)
    private String serialNumber;

    @Column(name = "ip_address")
    private String ipAddress;

    private int port = 80;

    private String username;

    @Column(name = "credentials_secret")
    private String credentialsSecret;

    @Column(name = "api_key_hash")
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_type", nullable = false)
    private ConnectionType connectionType = ConnectionType.PUSH;

    @Column(name = "last_sync_time")
    private Instant lastSyncTime;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    protected Device() {
    }

    public Device(String name, String serialNumber, UUID branchId, DeviceType type, ConnectionType connectionType) {
        this.name = name;
        this.serialNumber = serialNumber;
        this.branchId = branchId;
        this.type = type;
        this.connectionType = connectionType;
        this.status = Status.ACTIVE;
        this.port = 80;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCredentialsSecret() {
        return credentialsSecret;
    }

    public void setCredentialsSecret(String credentialsSecret) {
        this.credentialsSecret = credentialsSecret;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public Instant getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Instant lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
    }

    public enum ConnectionType {
        PUSH,
        POLLING,
        ALERT_STREAM
    }

    public enum DeviceType {
        HIKVISION,
        ZKTECO,
        SUPREMA,
        QR,
        MOBILE
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
