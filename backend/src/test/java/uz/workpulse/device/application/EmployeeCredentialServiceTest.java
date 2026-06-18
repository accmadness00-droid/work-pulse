package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.device.domain.EmployeeCredential;
import uz.workpulse.device.dto.CreateEmployeeCredentialRequest;
import uz.workpulse.device.infrastructure.EmployeeCredentialRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class EmployeeCredentialServiceTest {

    @Mock
    private EmployeeCredentialRepository credentialRepository;

    @Mock
    private EmployeeFacade employeeFacade;

    @Mock
    private AccessControlService accessControlService;

    private EmployeeCredentialService credentialService;

    @BeforeEach
    void setUp() {
        credentialService = new EmployeeCredentialService(credentialRepository, employeeFacade, accessControlService);
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEmployeeCredentialHappyPath() {
        UUID employeeId = UUID.randomUUID();
        when(employeeFacade.existsAndActive(employeeId)).thenReturn(true);
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(
                new EmployeeFacade.EmployeeScope(employeeId, UUID.randomUUID(), UUID.randomUUID(), null)
        ));
        when(credentialRepository.existsByCredentialTypeAndExternalId(EmployeeCredential.CredentialType.CARD, "CARD-001"))
                .thenReturn(false);
        when(credentialRepository.save(any(EmployeeCredential.class))).thenAnswer(invocation -> {
            EmployeeCredential credential = invocation.getArgument(0);
            ReflectionTestUtils.setField(credential, "id", UUID.randomUUID());
            return credential;
        });

        var response = credentialService.create(new CreateEmployeeCredentialRequest(
                employeeId,
                EmployeeCredential.CredentialType.CARD,
                "CARD-001"
        ));

        assertThat(response.employeeId()).isEqualTo(employeeId);
        assertThat(response.credentialType()).isEqualTo(EmployeeCredential.CredentialType.CARD);
        assertThat(response.externalId()).isEqualTo("CARD-001");
        assertThat(response.active()).isTrue();
    }

    @Test
    void duplicateCredentialFails() {
        UUID employeeId = UUID.randomUUID();
        when(employeeFacade.existsAndActive(employeeId)).thenReturn(true);
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(
                new EmployeeFacade.EmployeeScope(employeeId, UUID.randomUUID(), UUID.randomUUID(), null)
        ));
        when(credentialRepository.existsByCredentialTypeAndExternalId(EmployeeCredential.CredentialType.CARD, "CARD-001"))
                .thenReturn(true);

        assertThatThrownBy(() -> credentialService.create(new CreateEmployeeCredentialRequest(
                employeeId,
                EmployeeCredential.CredentialType.CARD,
                "CARD-001"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_CREDENTIAL_ALREADY_EXISTS);
    }

    @Test
    void deactivateSetsActiveFalse() {
        UUID credentialId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeCredential credential = new EmployeeCredential(
                employeeId,
                EmployeeCredential.CredentialType.CARD,
                "CARD-001"
        );
        when(credentialRepository.findById(credentialId)).thenReturn(Optional.of(credential));
        when(employeeFacade.findEmployeeScope(employeeId)).thenReturn(Optional.of(
                new EmployeeFacade.EmployeeScope(employeeId, UUID.randomUUID(), UUID.randomUUID(), null)
        ));
        when(credentialRepository.save(credential)).thenReturn(credential);

        var response = credentialService.deactivate(credentialId);

        assertThat(response.active()).isFalse();
        verify(credentialRepository).save(credential);
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
