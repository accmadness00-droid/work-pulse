package uz.workpulse.device.dto;

import java.util.List;
import java.util.UUID;

public record HikvisionPhotoSyncResponse(
        UUID employeeId,
        int totalDevices,
        int successCount,
        int failureCount,
        List<HikvisionPhotoSyncResult> results
) {
}
