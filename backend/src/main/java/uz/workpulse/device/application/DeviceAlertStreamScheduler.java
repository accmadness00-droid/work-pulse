package uz.workpulse.device.application;

import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.device.adapter.DeviceAdapter;
import uz.workpulse.device.adapter.HikvisionAdapter;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.IngestDeviceEventRequest;
import uz.workpulse.device.infrastructure.DeviceRepository;

@Component
public class DeviceAlertStreamScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceAdapterRegistry adapterRegistry;
    private final DeviceEventService deviceEventService;
    private final HikvisionAdapter hikvisionAdapter;

    public DeviceAlertStreamScheduler(
            DeviceRepository deviceRepository,
            DeviceAdapterRegistry adapterRegistry,
            DeviceEventService deviceEventService,
            HikvisionAdapter hikvisionAdapter
    ) {
        this.deviceRepository = deviceRepository;
        this.adapterRegistry = adapterRegistry;
        this.deviceEventService = deviceEventService;
        this.hikvisionAdapter = hikvisionAdapter;
    }

    @Scheduled(fixedDelayString = "${app.device.alert-stream.interval-ms:15000}")
    @Transactional
    public void consumeAlertStreams() {
        List<Device> devices = deviceRepository.findAllByConnectionTypeAndStatus(
                Device.ConnectionType.ALERT_STREAM,
                Device.Status.ACTIVE
        );
        for (Device device : devices) {
            consumeAlertStream(device);
        }
    }

    private void consumeAlertStream(Device device) {
        Instant since = device.getLastSyncTime() == null ? Instant.now().minusSeconds(3600) : device.getLastSyncTime();
        List<DeviceAdapter.PulledDeviceEvent> events = device.getType() == Device.DeviceType.HIKVISION
                ? hikvisionAdapter.pullAlertStreamEvents(device, since)
                : adapterRegistry.getAdapter(device.getType()).pullEvents(device, since);

        for (DeviceAdapter.PulledDeviceEvent pulled : events) {
            deviceEventService.ingestInternal(device, new IngestDeviceEventRequest(
                    device.getSerialNumber(),
                    pulled.externalEventId(),
                    pulled.employeeCode(),
                    pulled.credentialValue(),
                    pulled.eventTime(),
                    parseDirection(pulled.direction()),
                    parseAuthType(pulled.authType()),
                    null
            ));
        }
        device.setLastSyncTime(Instant.now());
        deviceRepository.save(device);
    }

    private uz.workpulse.device.domain.DeviceEvent.Direction parseDirection(String direction) {
        if (direction == null) {
            return uz.workpulse.device.domain.DeviceEvent.Direction.UNKNOWN;
        }
        return uz.workpulse.device.domain.DeviceEvent.Direction.valueOf(direction.toUpperCase());
    }

    private uz.workpulse.device.domain.DeviceEvent.AuthType parseAuthType(String authType) {
        if (authType == null) {
            return null;
        }
        return uz.workpulse.device.domain.DeviceEvent.AuthType.valueOf(authType.toUpperCase());
    }
}
