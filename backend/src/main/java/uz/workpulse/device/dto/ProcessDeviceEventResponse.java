package uz.workpulse.device.dto;

import java.util.UUID;

public record ProcessDeviceEventResponse(
        UUID eventId,
        boolean processed,
        String processingError,
        int retryCount
) {
}
