package uz.workpulse.company.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import uz.workpulse.shared.validation.ValidPhoneNumber;

public record UpdateCompanyRequest(
        @NotBlank String name,
        String legalName,
        String inn,
        @ValidPhoneNumber String phone,
        String email,
        String logoUrl,
        String plan,
        UUID ownerId
) {
}
