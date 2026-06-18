package uz.workpulse.device.dto;

import java.util.UUID;
import uz.workpulse.device.domain.Device;

public record DeviceFilterRequest(
        UUID branchId,
        Device.DeviceType type,
        Device.Status status,
        Device.ConnectionType connectionType
) {
}
