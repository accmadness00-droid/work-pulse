package uz.workpulse.company.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateCompanyRequest(
        @NotBlank String name,
        String legalName,
        String inn,
        String phone,
        String email,
        String logoUrl,
        String plan,
        UUID ownerId
) {
}
