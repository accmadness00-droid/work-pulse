package uz.workpulse.device.application;

import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.device.adapter.DeviceAdapter;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.IngestDeviceEventRequest;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.security.CredentialsCryptoService;

@Component
public class DevicePollingScheduler {

    private final DeviceRepository deviceRepository;
    private final DeviceAdapterRegistry adapterRegistry;
    private final DeviceEventService deviceEventService;
    private final CredentialsCryptoService credentialsCryptoService;

    public DevicePollingScheduler(
            DeviceRepository deviceRepository,
            DeviceAdapterRegistry adapterRegistry,
            DeviceEventService deviceEventService,
            CredentialsCryptoService credentialsCryptoService
    ) {
        this.deviceRepository = deviceRepository;
        this.adapterRegistry = adapterRegistry;
        this.deviceEventService = deviceEventService;
        this.credentialsCryptoService = credentialsCryptoService;
    }

    @Scheduled(fixedDelayString = "${app.device.polling.interval-ms:60000}")
    @Transactional
    public void pollDevices() {
        List<Device> devices = deviceRepository.findAllByConnectionTypeAndStatus(
                Device.ConnectionType.POLLING,
                Device.Status.ACTIVE
        );
        for (Device device : devices) {
            pollDevice(device);
        }
    }

    private void pollDevice(Device device) {
        credentialsCryptoService.decrypt(device.getCredentialsSecret());
        DeviceAdapter adapter = adapterRegistry.getAdapter(device.getType());
        Instant since = device.getLastSyncTime() == null ? Instant.now().minusSeconds(3600) : device.getLastSyncTime();
        List<DeviceAdapter.PulledDeviceEvent> events = adapter.pullEvents(device, since);
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
