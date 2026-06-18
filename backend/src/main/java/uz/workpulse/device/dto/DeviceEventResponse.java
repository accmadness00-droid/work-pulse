package uz.workpulse.device.dto;

import java.time.Instant;
import java.util.UUID;
import uz.workpulse.device.domain.DeviceEvent;

public record DeviceEventResponse(
        UUID id,
        UUID deviceId,
        String externalEventId,
        String eventHash,
        String employeeCode,
        String credentialValue,
        Instant eventTime,
        DeviceEvent.Direction direction,
        DeviceEvent.AuthType authType,
        String rawPayload,
        boolean processed,
        String processingError,
        int retryCount
) {

    public static DeviceEventResponse from(DeviceEvent event) {
        return new DeviceEventResponse(
                event.getId(),
                event.getDeviceId(),
                event.getExternalEventId(),
                event.getEventHash(),
                event.getEmployeeCode(),
                event.getCredentialValue(),
                event.getEventTime(),
                event.getDirection(),
                event.getAuthType(),
                event.getRawPayload(),
                event.isProcessed(),
                event.getProcessingError(),
                event.getRetryCount()
        );
    }
}
