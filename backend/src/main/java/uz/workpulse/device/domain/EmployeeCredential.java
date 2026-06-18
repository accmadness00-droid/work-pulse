package uz.workpulse.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import uz.workpulse.shared.domain.BaseEntity;

@Entity
@Table(name = "employee_credentials")
public class EmployeeCredential extends BaseEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false)
    private CredentialType credentialType;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private boolean active = true;

    protected EmployeeCredential() {
    }

    public EmployeeCredential(UUID employeeId, CredentialType credentialType, String externalId) {
        this.employeeId = employeeId;
        this.credentialType = credentialType;
        this.externalId = externalId;
        this.active = true;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(UUID employeeId) {
        this.employeeId = employeeId;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public enum CredentialType {
        CARD,
        FACE,
        FINGERPRINT,
        QR
    }
}
