package uz.workpulse.device.dto;

import java.util.UUID;

public record RotateDeviceApiKeyResponse(
        UUID deviceId,
        String serialNumber,
        String apiKey
) {
}
