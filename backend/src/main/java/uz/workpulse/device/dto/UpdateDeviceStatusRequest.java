package uz.workpulse.device.dto;

import jakarta.validation.constraints.NotNull;
import uz.workpulse.device.domain.Device;

public record UpdateDeviceStatusRequest(
        @NotNull Device.Status status
) {
}
