package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.infrastructure.DeviceRepository;

@ExtendWith(MockitoExtension.class)
class DeviceApiKeyServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Test
    void validateDeviceApiKeyWorks() {
        PasswordService passwordService = new PasswordService(new BCryptPasswordEncoder(12));
        DeviceApiKeyService service = new DeviceApiKeyService(deviceRepository, passwordService);
        Device device = new Device("Main Door", "SN-001", UUID.randomUUID(), Device.DeviceType.HIKVISION, Device.ConnectionType.PUSH);
        device.setApiKeyHash(passwordService.encode("plain-key"));
        when(deviceRepository.findBySerialNumber("SN-001")).thenReturn(Optional.of(device));

        assertThat(service.authenticateDevice("sn-001", "plain-key").getSerialNumber()).isEqualTo("SN-001");
    }
}
