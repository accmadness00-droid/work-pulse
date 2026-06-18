package uz.workpulse.device.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.attendance.application.AttendanceFacade;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.domain.DeviceEvent;
import uz.workpulse.device.domain.EmployeeCredential;
import uz.workpulse.device.infrastructure.DeviceEventRepository;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.device.infrastructure.EmployeeCredentialRepository;
import uz.workpulse.employee.application.EmployeeFacade;

@ExtendWith(MockitoExtension.class)
class DeviceEventProcessorTest {

    @Mock
    private DeviceEventRepository eventRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private EmployeeCredentialRepository credentialRepository;

    @Mock
    private EmployeeFacade employeeFacade;

    @Mock
    private AttendanceFacade attendanceFacade;

    private DeviceEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new DeviceEventProcessor(
                eventRepository,
                deviceRepository,
                credentialRepository,
                employeeFacade,
                attendanceFacade,
                10,
                3
        );
    }

    @Test
    void processCredentialValueAndAuthTypeResolvesEmployeeAndCallsAttendanceFacade() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        DeviceEvent event = event(DeviceEvent.Direction.IN);
        event.setCredentialValue("CARD-001");
        event.setAuthType(DeviceEvent.AuthType.CARD);
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(credentialRepository.findByCredentialTypeAndExternalIdAndActiveTrue(EmployeeCredential.CredentialType.CARD, "CARD-001"))
                .thenReturn(Optional.of(new EmployeeCredential(employeeId, EmployeeCredential.CredentialType.CARD, "CARD-001")));
        when(deviceRepository.findById(event.getDeviceId())).thenReturn(Optional.of(device(event.getDeviceId(), branchId)));
        when(attendanceFacade.processDeviceEvent(
                employeeId,
                event.getEventTime(),
                AttendanceFacade.Direction.IN,
                event.getDeviceId(),
                branchId
        )).thenReturn(AttendanceFacade.ProcessDeviceEventResult.CHECKED_IN);

        var response = processor.processEvent(event.getId());

        assertThat(response.processed()).isTrue();
        verify(attendanceFacade).processDeviceEvent(
                employeeId,
                event.getEventTime(),
                AttendanceFacade.Direction.IN,
                event.getDeviceId(),
                branchId
        );
    }

    @Test
    void processEmployeeCodeResolvesViaEmployeeFacade() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        DeviceEvent event = event(DeviceEvent.Direction.OUT);
        event.setEmployeeCode("EMP-001");
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(employeeFacade.findEmployeeIdByCode("EMP-001")).thenReturn(Optional.of(employeeId));
        when(deviceRepository.findById(event.getDeviceId())).thenReturn(Optional.of(device(event.getDeviceId(), branchId)));
        when(attendanceFacade.processDeviceEvent(
                employeeId,
                event.getEventTime(),
                AttendanceFacade.Direction.OUT,
                event.getDeviceId(),
                branchId
        )).thenReturn(AttendanceFacade.ProcessDeviceEventResult.CHECKED_OUT);

        var response = processor.processEvent(event.getId());

        assertThat(response.processed()).isTrue();
    }

    @Test
    void unknownDirectionMarksProcessingError() {
        DeviceEvent event = event(DeviceEvent.Direction.UNKNOWN);
        event.setEmployeeCode("EMP-001");
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));

        var response = processor.processEvent(event.getId());

        assertThat(response.processed()).isFalse();
        assertThat(response.processingError()).isEqualTo("UNKNOWN_DIRECTION");
        assertThat(response.retryCount()).isEqualTo(1);
    }

    @Test
    void employeeNotFoundMarksProcessingError() {
        DeviceEvent event = event(DeviceEvent.Direction.IN);
        event.setEmployeeCode("EMP-404");
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(employeeFacade.findEmployeeIdByCode("EMP-404")).thenReturn(Optional.empty());

        var response = processor.processEvent(event.getId());

        assertThat(response.processed()).isFalse();
        assertThat(response.processingError()).isEqualTo("EMPLOYEE_RESOLVE_FAILED");
    }

    @Test
    void duplicateInMarksProcessedWithoutRetry() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        DeviceEvent event = event(DeviceEvent.Direction.IN);
        event.setEmployeeCode("EMP-001");
        when(eventRepository.findByIdForUpdate(event.getId())).thenReturn(Optional.of(event));
        when(employeeFacade.findEmployeeIdByCode("EMP-001")).thenReturn(Optional.of(employeeId));
        when(deviceRepository.findById(event.getDeviceId())).thenReturn(Optional.of(device(event.getDeviceId(), branchId)));
        when(attendanceFacade.processDeviceEvent(any(), any(), any(), any(), any()))
                .thenReturn(AttendanceFacade.ProcessDeviceEventResult.DUPLICATE_IN);

        var response = processor.processEvent(event.getId());

        assertThat(response.processed()).isTrue();
        assertThat(response.processingError()).isNull();
    }

    @Test
    void batchProcessesUnprocessedEvents() {
        UUID employeeId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        DeviceEvent event = event(DeviceEvent.Direction.IN);
        event.setEmployeeCode("EMP-001");
        when(eventRepository.findUnprocessedBatchForUpdate(10, 3)).thenReturn(List.of(event));
        when(employeeFacade.findEmployeeIdByCode("EMP-001")).thenReturn(Optional.of(employeeId));
        when(deviceRepository.findById(event.getDeviceId())).thenReturn(Optional.of(device(event.getDeviceId(), branchId)));
        when(attendanceFacade.processDeviceEvent(any(), any(), any(), any(), eq(branchId)))
                .thenReturn(AttendanceFacade.ProcessDeviceEventResult.CHECKED_IN);

        int processed = processor.processBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.isProcessed()).isTrue();
    }

    private DeviceEvent event(DeviceEvent.Direction direction) {
        DeviceEvent event = new DeviceEvent(UUID.randomUUID(), "external-1", UUID.randomUUID().toString(), Instant.now(), direction);
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
    }

    private Device device(UUID id, UUID branchId) {
        Device device = new Device("Door", "SN-001", branchId, Device.DeviceType.HIKVISION, Device.ConnectionType.PUSH);
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }
}
