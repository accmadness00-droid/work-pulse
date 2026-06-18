package uz.workpulse.company.dto;

import java.util.UUID;
import uz.workpulse.company.domain.Company;

public record CompanyResponse(
        UUID id,
        String name,
        String legalName,
        String inn,
        String phone,
        String email,
        String logoUrl,
        String plan,
        UUID ownerId,
        boolean active
) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getLegalName(),
                company.getInn(),
                company.getPhone(),
                company.getEmail(),
                company.getLogoUrl(),
                company.getPlan(),
                company.getOwnerId(),
                company.isActive()
        );
    }
}
