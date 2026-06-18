package uz.workpulse.device.dto;

import java.util.UUID;
import uz.workpulse.device.domain.EmployeeCredential;

public record EmployeeCredentialResponse(
        UUID id,
        UUID employeeId,
        EmployeeCredential.CredentialType credentialType,
        String externalId,
        boolean active
) {

    public static EmployeeCredentialResponse from(EmployeeCredential credential) {
        return new EmployeeCredentialResponse(
                credential.getId(),
                credential.getEmployeeId(),
                credential.getCredentialType(),
                credential.getExternalId(),
                credential.isActive()
        );
    }
}
