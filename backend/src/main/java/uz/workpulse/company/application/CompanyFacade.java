package uz.workpulse.company.application;

import java.util.UUID;

public interface CompanyFacade {

    void ensureActiveCompany(UUID companyId);
}
