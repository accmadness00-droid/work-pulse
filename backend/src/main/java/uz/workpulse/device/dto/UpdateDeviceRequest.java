package uz.workpulse.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import uz.workpulse.device.domain.Device;

public record UpdateDeviceRequest(
        @NotBlank String name,
        @NotBlank String serialNumber,
        String ipAddress,
        Integer port,
        String username,
        String credentialsSecret,
        @NotNull UUID branchId,
        @NotNull Device.DeviceType type,
        @NotNull Device.ConnectionType connectionType
) {
}
