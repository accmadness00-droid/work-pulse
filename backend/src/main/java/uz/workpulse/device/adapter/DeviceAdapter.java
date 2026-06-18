package uz.workpulse.device.adapter;

import java.time.Instant;
import java.util.List;
import uz.workpulse.device.domain.Device;

public interface DeviceAdapter {

    Device.DeviceType supportedType();

    List<PulledDeviceEvent> pullEvents(Device device, Instant since);

    record PulledDeviceEvent(
            String externalEventId,
            String employeeCode,
            String credentialValue,
            Instant eventTime,
            String direction,
            String authType,
            String rawPayload
    ) {
    }
}
