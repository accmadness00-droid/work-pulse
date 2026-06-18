package uz.workpulse.device.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.attendance.application.AttendanceFacade;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.domain.DeviceEvent;
import uz.workpulse.device.dto.ProcessDeviceEventResponse;
import uz.workpulse.device.infrastructure.DeviceEventRepository;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.device.infrastructure.EmployeeCredentialRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class DeviceEventProcessor {

    private static final String UNKNOWN_DIRECTION = "UNKNOWN_DIRECTION";
    private static final String EMPLOYEE_IDENTIFIER_MISSING = "EMPLOYEE_IDENTIFIER_MISSING";
    private static final String EMPLOYEE_RESOLVE_FAILED = "EMPLOYEE_RESOLVE_FAILED";
    private static final String ORPHAN_OUT = "ORPHAN_OUT";

    private final DeviceEventRepository eventRepository;
    private final DeviceRepository deviceRepository;
    private final EmployeeCredentialRepository credentialRepository;
    private final EmployeeFacade employeeFacade;
    private final AttendanceFacade attendanceFacade;
    private final int batchSize;
    private final int maxRetry;

    public DeviceEventProcessor(
            DeviceEventRepository eventRepository,
            DeviceRepository deviceRepository,
            EmployeeCredentialRepository credentialRepository,
            EmployeeFacade employeeFacade,
            AttendanceFacade attendanceFacade,
            @Value("${app.device-events.processor.batch-size:50}") int batchSize,
            @Value("${app.device-events.processor.max-retry:3}") int maxRetry
    ) {
        this.eventRepository = eventRepository;
        this.deviceRepository = deviceRepository;
        this.credentialRepository = credentialRepository;
        this.employeeFacade = employeeFacade;
        this.attendanceFacade = attendanceFacade;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
    }

    @Transactional
    public ProcessDeviceEventResponse processEvent(UUID eventId) {
        DeviceEvent event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EVENT_NOT_FOUND));
        return processLockedEvent(event);
    }

    @Transactional
    public int processBatch() {
        List<DeviceEvent> events = eventRepository.findUnprocessedBatchForUpdate(batchSize, maxRetry);
        events.forEach(this::processLockedEvent);
        return events.size();
    }

    @Scheduled(fixedDelayString = "${app.device-events.processor.fixed-delay-ms:30000}")
    @Transactional
    public void scheduledProcessBatch() {
        processBatch();
    }

    private ProcessDeviceEventResponse processLockedEvent(DeviceEvent event) {
        if (event.isProcessed()) {
            return response(event);
        }
        try {
            if (event.getDirection() == DeviceEvent.Direction.UNKNOWN) {
                throw new BusinessException(ErrorCode.DEVICE_EVENT_UNKNOWN_DIRECTION, UNKNOWN_DIRECTION);
            }

            UUID employeeId = resolveEmployee(event)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EVENT_EMPLOYEE_RESOLVE_FAILED, EMPLOYEE_RESOLVE_FAILED));
            UUID branchId = deviceRepository.findById(event.getDeviceId())
                    .map(Device::getBranchId)
                    .orElse(null);

            AttendanceFacade.ProcessDeviceEventResult result = attendanceFacade.processDeviceEvent(
                    employeeId,
                    event.getEventTime(),
                    toAttendanceDirection(event.getDirection()),
                    event.getDeviceId(),
                    branchId
            );

            switch (result) {
                case DUPLICATE_IN -> event.setProcessingError(null);
                case ORPHAN_OUT -> event.setProcessingError(ORPHAN_OUT);
                case CHECKED_IN, CHECKED_OUT -> event.setProcessingError(null);
            }
            event.markProcessed();
            eventRepository.save(event);
            return response(event);
        } catch (BusinessException ex) {
            markFailed(event, ex.getMessage());
            return response(event);
        } catch (RuntimeException ex) {
            markFailed(event, ex.getMessage() == null ? ErrorCode.DEVICE_EVENT_PROCESSING_FAILED.getDefaultMessage() : ex.getMessage());
            return response(event);
        }
    }

    private Optional<UUID> resolveEmployee(DeviceEvent event) {
        if (StringUtils.hasText(event.getCredentialValue()) && event.getAuthType() != null) {
            uz.workpulse.device.domain.EmployeeCredential.CredentialType credentialType =
                    uz.workpulse.device.domain.EmployeeCredential.CredentialType.valueOf(event.getAuthType().name());
            Optional<UUID> byCredential = credentialRepository
                    .findByCredentialTypeAndExternalIdAndActiveTrue(credentialType, event.getCredentialValue().trim())
                    .map(uz.workpulse.device.domain.EmployeeCredential::getEmployeeId);
            if (byCredential.isPresent()) {
                return byCredential;
            }
        }

        if (StringUtils.hasText(event.getEmployeeCode())) {
            return employeeFacade.findEmployeeIdByCode(event.getEmployeeCode().trim());
        }

        throw new BusinessException(ErrorCode.DEVICE_EVENT_EMPLOYEE_IDENTIFIER_MISSING, EMPLOYEE_IDENTIFIER_MISSING);
    }

    private AttendanceFacade.Direction toAttendanceDirection(DeviceEvent.Direction direction) {
        return direction == DeviceEvent.Direction.IN ? AttendanceFacade.Direction.IN : AttendanceFacade.Direction.OUT;
    }

    private void markFailed(DeviceEvent event, String message) {
        event.incrementRetryCount();
        event.setProcessingError(message);
        eventRepository.save(event);
    }

    private ProcessDeviceEventResponse response(DeviceEvent event) {
        return new ProcessDeviceEventResponse(event.getId(), event.isProcessed(), event.getProcessingError(), event.getRetryCount());
    }
}
