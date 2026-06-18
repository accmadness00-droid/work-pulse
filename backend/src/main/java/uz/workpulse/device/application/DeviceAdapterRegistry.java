package uz.workpulse.device.application;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uz.workpulse.device.adapter.DeviceAdapter;
import uz.workpulse.device.domain.Device;

@Component
public class DeviceAdapterRegistry {

    private final Map<Device.DeviceType, DeviceAdapter> adapters;

    public DeviceAdapterRegistry(List<DeviceAdapter> adapters) {
        this.adapters = adapters.stream()
                .collect(Collectors.toMap(DeviceAdapter::supportedType, Function.identity()));
    }

    public DeviceAdapter getAdapter(Device.DeviceType type) {
        DeviceAdapter adapter = adapters.get(type);
        if (adapter == null) {
            throw new IllegalStateException("No device adapter registered for type " + type);
        }
        return adapter;
    }
}
