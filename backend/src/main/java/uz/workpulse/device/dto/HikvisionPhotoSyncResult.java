package uz.workpulse.device.dto;

import java.util.UUID;

public record HikvisionPhotoSyncResult(
        UUID deviceId,
        String deviceName,
        boolean success,
        String message
) {
}
