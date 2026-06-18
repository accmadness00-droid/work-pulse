package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.branch.infrastructure.BranchRepository;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.CreateDeviceRequest;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.config.JwtConfig;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;
import uz.workpulse.shared.security.CredentialsCryptoService;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private BranchFacade branchFacade;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private AccessControlService accessControlService;

    private PasswordService passwordService;
    private CredentialsCryptoService credentialsCryptoService;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService(new BCryptPasswordEncoder(12));
        credentialsCryptoService = new CredentialsCryptoService(
                new JwtConfig("work-pulse-test", "test-secret-that-is-long-enough-for-hmac", 15, 7)
        );
        deviceService = new DeviceService(
                deviceRepository,
                branchRepository,
                branchFacade,
                passwordService,
                credentialsCryptoService,
                accessControlService
        );
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createDeviceHappyPath() {
        UUID branchId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(branchFacade.getCompanyId(branchId)).thenReturn(companyId);
        doNothing().when(branchFacade).ensureActiveBranch(branchId);
        when(deviceRepository.existsBySerialNumber("SN-001")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            ReflectionTestUtils.setField(device, "id", UUID.randomUUID());
            return device;
        });

        var response = deviceService.create(createRequest(branchId, "sn-001"));

        assertThat(response.name()).isEqualTo("Main Door");
        assertThat(response.serialNumber()).isEqualTo("SN-001");
        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.status()).isEqualTo(Device.Status.ACTIVE);
    }

    @Test
    void duplicateSerialNumberFails() {
        UUID branchId = UUID.randomUUID();
        when(branchFacade.getCompanyId(branchId)).thenReturn(UUID.randomUUID());
        doNothing().when(branchFacade).ensureActiveBranch(branchId);
        when(deviceRepository.existsBySerialNumber("SN-001")).thenReturn(true);

        assertThatThrownBy(() -> deviceService.create(createRequest(branchId, "sn-001")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DEVICE_SERIAL_ALREADY_EXISTS);
    }

    @Test
    void deactivateSetsStatusInactive() {
        UUID deviceId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Device device = device(branchId, "SN-001");
        ReflectionTestUtils.setField(device, "id", deviceId);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(branchFacade.getCompanyId(branchId)).thenReturn(UUID.randomUUID());
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.deactivate(deviceId);

        assertThat(response.status()).isEqualTo(Device.Status.INACTIVE);
        verify(deviceRepository).save(device);
    }

    @Test
    void rotateApiKeyStoresHashAndReturnsPlainOnce() {
        UUID deviceId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Device device = device(branchId, "SN-001");
        ReflectionTestUtils.setField(device, "id", deviceId);
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(branchFacade.getCompanyId(branchId)).thenReturn(UUID.randomUUID());
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.rotateApiKey(deviceId);

        assertThat(response.apiKey()).startsWith("wpd_");
        assertThat(device.getApiKeyHash()).isNotBlank();
        assertThat(device.getApiKeyHash()).isNotEqualTo(response.apiKey());
        assertThat(passwordService.matches(response.apiKey(), device.getApiKeyHash())).isTrue();
    }

    private CreateDeviceRequest createRequest(UUID branchId, String serialNumber) {
        return new CreateDeviceRequest(
                "Main Door",
                serialNumber,
                null,
                80,
                null,
                null,
                branchId,
                Device.DeviceType.HIKVISION,
                Device.ConnectionType.PUSH
        );
    }

    private Device device(UUID branchId, String serialNumber) {
        return new Device("Main Door", serialNumber, branchId, Device.DeviceType.HIKVISION, Device.ConnectionType.PUSH);
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
