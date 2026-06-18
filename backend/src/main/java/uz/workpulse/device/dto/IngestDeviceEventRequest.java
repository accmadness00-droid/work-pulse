package uz.workpulse.device.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import uz.workpulse.device.domain.DeviceEvent;

public record IngestDeviceEventRequest(
        String deviceSerialNumber,
        String externalEventId,
        String employeeCode,
        String credentialValue,
        @NotNull Instant eventTime,
        @NotNull DeviceEvent.Direction direction,
        DeviceEvent.AuthType authType,
        JsonNode rawPayload
) {
}
