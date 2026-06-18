package uz.workpulse.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import uz.workpulse.device.domain.EmployeeCredential;

public record CreateEmployeeCredentialRequest(
        @NotNull UUID employeeId,
        @NotNull EmployeeCredential.CredentialType credentialType,
        @NotBlank String externalId
) {
}
