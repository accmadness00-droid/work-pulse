package uz.workpulse.device.dto;

import java.time.Instant;
import java.util.UUID;
import uz.workpulse.device.domain.Device;

public record DeviceResponse(
        UUID id,
        String name,
        String serialNumber,
        String ipAddress,
        int port,
        String username,
        UUID branchId,
        Device.DeviceType type,
        Device.ConnectionType connectionType,
        Device.Status status,
        Instant lastSyncTime,
        boolean apiKeyConfigured
) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getSerialNumber(),
                device.getIpAddress(),
                device.getPort(),
                device.getUsername(),
                device.getBranchId(),
                device.getType(),
                device.getConnectionType(),
                device.getStatus(),
                device.getLastSyncTime(),
                device.getApiKeyHash() != null
        );
    }
}
