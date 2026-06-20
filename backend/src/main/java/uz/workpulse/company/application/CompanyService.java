package uz.workpulse.company.application;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.company.domain.Company;
import uz.workpulse.company.domain.CompanySettings;
import uz.workpulse.company.dto.CompanyResponse;
import uz.workpulse.company.dto.CompanySettingsResponse;
import uz.workpulse.company.dto.CreateCompanyRequest;
import uz.workpulse.company.dto.UpdateCompanyRequest;
import uz.workpulse.company.dto.UpdateCompanySettingsRequest;
import uz.workpulse.company.infrastructure.CompanyRepository;
import uz.workpulse.company.infrastructure.CompanySettingsRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;

@Service
public class CompanyService implements CompanyFacade {

    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final AccessControlService accessControlService;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanySettingsRepository companySettingsRepository,
            AccessControlService accessControlService
    ) {
        this.companyRepository = companyRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> listCompanies() {
        accessControlService.requireSuperAdmin(accessControlService.currentUser());
        return companyRepository.findAllByActiveTrue().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        accessControlService.requireSuperAdmin(accessControlService.currentUser());
        String inn = normalizeInn(request.inn());
        ensureInnUnique(inn);

        Company company = new Company(request.name().trim(), inn);
        company.setLegalName(request.legalName());
        company.setPhone(normalizePhone(request.phone()));
        company.setEmail(request.email());
        company.setLogoUrl(request.logoUrl());
        if (StringUtils.hasText(request.plan())) {
            company.setPlan(request.plan().trim());
        }
        company.setOwnerId(request.ownerId());
        Company saved = companyRepository.save(company);
        companySettingsRepository.save(new CompanySettings(saved.getId()));
        return CompanyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompany(UUID id) {
        requireCompanyReadAccess(id);
        return CompanyResponse.from(getActiveCompanyOrThrow(id));
    }

    @Transactional
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        requireCompanyWriteAccess(id);
        Company company = getActiveCompanyOrThrow(id);
        String inn = normalizeInn(request.inn());
        ensureInnUniqueForUpdate(inn, id);

        company.setName(request.name().trim());
        company.setInn(inn);
        company.setLegalName(request.legalName());
        company.setPhone(normalizePhone(request.phone()));
        company.setEmail(request.email());
        company.setLogoUrl(request.logoUrl());
        if (StringUtils.hasText(request.plan())) {
            company.setPlan(request.plan().trim());
        }
        company.setOwnerId(request.ownerId());
        return CompanyResponse.from(companyRepository.save(company));
    }

    @Transactional
    public void deleteCompany(UUID id) {
        requireCompanyWriteAccess(id);
        Company company = getActiveCompanyOrThrow(id);
        company.deactivate();
        companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public CompanySettingsResponse getSettings(UUID companyId) {
        requireCompanyReadAccess(companyId);
        Company company = getActiveCompanyOrThrow(companyId);
        CompanySettings settings = companySettingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Company settings not found"));
        return CompanySettingsResponse.from(settings, company.getPlan());
    }

    @Transactional
    public CompanySettingsResponse updateSettings(UUID companyId, UpdateCompanySettingsRequest request) {
        requireCompanyWriteAccess(companyId);
        Company company = getActiveCompanyOrThrow(companyId);
        CompanySettings settings = companySettingsRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Company settings not found"));

        settings.setTimezone(request.timezone().trim());
        settings.setLocale(request.locale().trim());
        settings.setPayrollOvertimeBonusEnabled(Boolean.TRUE.equals(request.payrollOvertimeBonusEnabled()));
        settings.setPayrollLatePenaltyEnabled(Boolean.TRUE.equals(request.payrollLatePenaltyEnabled()));
        settings.setPayrollOvertimeMultiplier(policyMultiplier(request.payrollOvertimeMultiplier(), new BigDecimal("1.50")));
        settings.setPayrollLatePenaltyMultiplier(policyMultiplier(request.payrollLatePenaltyMultiplier(), BigDecimal.ONE));
        if (StringUtils.hasText(request.plan())) {
            company.setPlan(request.plan().trim());
            companyRepository.save(company);
        }
        return CompanySettingsResponse.from(companySettingsRepository.save(settings), company.getPlan());
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureActiveCompany(UUID companyId) {
        getActiveCompanyOrThrow(companyId);
    }

    @Transactional(readOnly = true)
    public Company getActiveCompanyOrThrow(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
        if (!company.isActive()) {
            throw new BusinessException(ErrorCode.COMPANY_INACTIVE);
        }
        return company;
    }

    private void ensureInnUnique(String inn) {
        if (inn != null && companyRepository.existsByInn(inn)) {
            throw new BusinessException(ErrorCode.COMPANY_INN_ALREADY_EXISTS);
        }
    }

    private void ensureInnUniqueForUpdate(String inn, UUID companyId) {
        if (inn != null && companyRepository.existsByInnAndIdNot(inn, companyId)) {
            throw new BusinessException(ErrorCode.COMPANY_INN_ALREADY_EXISTS);
        }
    }

    private String normalizeInn(String inn) {
        if (!StringUtils.hasText(inn)) {
            return null;
        }
        return inn.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal policyMultiplier(BigDecimal value, BigDecimal defaultValue) {
        return (value == null ? defaultValue : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePhone(String phone) {
        return StringUtils.hasText(phone) ? phone : null;
    }

    private void requireCompanyReadAccess(UUID companyId) {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN) {
            return;
        }
        accessControlService.requireCompanyAccess(accessControlService.currentUser(), companyId);
    }

    private void requireCompanyWriteAccess(UUID companyId) {
        requireCompanyReadAccess(companyId);
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.EMPLOYEE || role == User.Role.BRANCH_MANAGER) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
