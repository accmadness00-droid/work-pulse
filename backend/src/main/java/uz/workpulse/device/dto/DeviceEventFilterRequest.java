package uz.workpulse.device.dto;

import java.time.Instant;
import java.util.UUID;
import uz.workpulse.device.domain.DeviceEvent;

public record DeviceEventFilterRequest(
        UUID deviceId,
        Boolean processed,
        Instant from,
        Instant to,
        DeviceEvent.Direction direction,
        DeviceEvent.AuthType authType
) {
}
