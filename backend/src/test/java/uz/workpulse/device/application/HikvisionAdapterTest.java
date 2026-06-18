package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.device.adapter.HikvisionAdapter;
import uz.workpulse.device.adapter.HikvisionIsapiClient;
import uz.workpulse.device.adapter.dto.HikvisionEventPayload;
import uz.workpulse.device.domain.Device;
import uz.workpulse.shared.security.CredentialsCryptoService;

@ExtendWith(MockitoExtension.class)
class HikvisionAdapterTest {

    @Mock
    private HikvisionIsapiClient isapiClient;

    @Mock
    private CredentialsCryptoService credentialsCryptoService;

    @InjectMocks
    private HikvisionAdapter hikvisionAdapter;

    @Test
    void pullEventsReturnsEmptyWhenCredentialsMissing() {
        Device device = device();
        when(credentialsCryptoService.decrypt(device.getCredentialsSecret())).thenReturn("");

        assertThat(hikvisionAdapter.pullEvents(device, Instant.now().minusSeconds(60))).isEmpty();
    }

    @Test
    void pullEventsMapsAccessControlEvents() {
        Device device = device();
        device.setUsername("admin");
        device.setCredentialsSecret("enc:v1:test");
        when(credentialsCryptoService.decrypt("enc:v1:test")).thenReturn("secret");
        when(isapiClient.fetchAccessControlEventsJson(any(), any(), any(), any(), any()))
                .thenReturn("{\"AcsEvent\":{\"numOfMatches\":1}}");
        when(isapiClient.parseAccessControlEvents(any())).thenReturn(new HikvisionEventPayload(
                new HikvisionEventPayload.AcsEvent(
                        java.util.List.of(new HikvisionEventPayload.Info(
                                1001L,
                                "EMP-001",
                                null,
                                "2026-06-13T09:00:00Z",
                                null,
                                75,
                                "face"
                        )),
                        1
                )
        ));

        var events = hikvisionAdapter.pullEvents(device, Instant.parse("2026-06-13T08:00:00Z"));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).employeeCode()).isEqualTo("EMP-001");
        assertThat(events.get(0).direction()).isEqualTo("IN");
        assertThat(events.get(0).authType()).isEqualTo("FACE");
    }

    private Device device() {
        Device device = new Device("Door", "HIK-001", UUID.randomUUID(), Device.DeviceType.HIKVISION, Device.ConnectionType.POLLING);
        ReflectionTestUtils.setField(device, "id", UUID.randomUUID());
        device.setIpAddress("127.0.0.1");
        device.setPort(80);
        return device;
    }
}
