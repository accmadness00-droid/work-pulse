package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.domain.DeviceEvent;
import uz.workpulse.device.dto.IngestDeviceEventRequest;
import uz.workpulse.device.infrastructure.DeviceEventRepository;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class DeviceEventServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceEventRepository eventRepository;

    @Mock
    private DeviceEventProcessor processor;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private BranchFacade branchFacade;

    private DeviceEventService service;

    @BeforeEach
    void setUp() {
        service = new DeviceEventService(
                deviceRepository,
                eventRepository,
                processor,
                new ObjectMapper(),
                accessControlService,
                branchFacade
        );
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        when(accessControlService.isDevicePush()).thenReturn(false);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ingestSavesRawEvent() {
        Device device = device();
        when(deviceRepository.findBySerialNumber("SN-001")).thenReturn(Optional.of(device));
        when(branchFacade.getCompanyId(device.getBranchId())).thenReturn(UUID.randomUUID());
        when(eventRepository.findByDeviceIdAndEventHash(any(UUID.class), anyString())).thenReturn(Optional.empty());
        when(eventRepository.saveAndFlush(any(DeviceEvent.class))).thenAnswer(invocation -> {
            DeviceEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
            return event;
        });

        var response = service.ingest(new IngestDeviceEventRequest(
                "sn-001",
                null,
                "EMP-001",
                null,
                Instant.parse("2026-06-13T04:00:00Z"),
                DeviceEvent.Direction.UNKNOWN,
                null,
                new ObjectMapper().createObjectNode().put("raw", true)
        ));

        assertThat(response.deviceId()).isEqualTo(device.getId());
        assertThat(response.employeeCode()).isEqualTo("EMP-001");
        assertThat(response.processed()).isFalse();
        assertThat(response.rawPayload()).contains("raw");
        verify(processor, never()).processEvent(any(UUID.class));
    }

    @Test
    void ingestDuplicateEventReturnsExisting() {
        Device device = device();
        DeviceEvent existing = event(device.getId(), DeviceEvent.Direction.IN);
        when(deviceRepository.findBySerialNumber("SN-001")).thenReturn(Optional.of(device));
        when(branchFacade.getCompanyId(device.getBranchId())).thenReturn(UUID.randomUUID());
        when(eventRepository.findByDeviceIdAndEventHash(any(UUID.class), anyString())).thenReturn(Optional.of(existing));

        var response = service.ingest(new IngestDeviceEventRequest(
                "SN-001",
                "external-1",
                "EMP-001",
                null,
                Instant.parse("2026-06-13T04:00:00Z"),
                DeviceEvent.Direction.IN,
                null,
                null
        ));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(eventRepository, never()).saveAndFlush(any(DeviceEvent.class));
    }

    private Device device() {
        Device device = new Device("Main Door", "SN-001", UUID.randomUUID(), Device.DeviceType.HIKVISION, Device.ConnectionType.PUSH);
        ReflectionTestUtils.setField(device, "id", UUID.randomUUID());
        return device;
    }

    private DeviceEvent event(UUID deviceId, DeviceEvent.Direction direction) {
        DeviceEvent event = new DeviceEvent(deviceId, "external-1", "hash", Instant.now(), direction);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
