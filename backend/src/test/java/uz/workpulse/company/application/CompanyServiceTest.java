package uz.workpulse.company.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.company.domain.Company;
import uz.workpulse.company.domain.CompanySettings;
import uz.workpulse.company.dto.CreateCompanyRequest;
import uz.workpulse.company.infrastructure.CompanyRepository;
import uz.workpulse.company.infrastructure.CompanySettingsRepository;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySettingsRepository companySettingsRepository;

    @Mock
    private AccessControlService accessControlService;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(companyRepository, companySettingsRepository, accessControlService);
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCompanyCreatesDefaultSettings() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            ReflectionTestUtils.setField(company, "id", companyId);
            return company;
        });
        when(companySettingsRepository.save(any(CompanySettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        companyService.createCompany(new CreateCompanyRequest("Work Pulse", null, "123456789", null, null, null, null, null));

        ArgumentCaptor<CompanySettings> settingsCaptor = ArgumentCaptor.forClass(CompanySettings.class);
        verify(companySettingsRepository).save(settingsCaptor.capture());
        CompanySettings settings = settingsCaptor.getValue();
        assertThat(settings.getCompanyId()).isEqualTo(companyId);
        assertThat(settings.getTimezone()).isEqualTo("Asia/Tashkent");
        assertThat(settings.getLocale()).isEqualTo("uz-UZ");
    }

    @Test
    void deleteCompanySetsActiveFalse() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company("Work Pulse", "123456789");
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        companyService.deleteCompany(companyId);

        assertThat(company.isActive()).isFalse();
        verify(companyRepository).save(company);
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
