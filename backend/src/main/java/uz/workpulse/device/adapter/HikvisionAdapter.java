package uz.workpulse.device.adapter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uz.workpulse.device.adapter.dto.HikvisionEventPayload;
import uz.workpulse.device.domain.Device;
import uz.workpulse.shared.security.CredentialsCryptoService;

@Component
public class HikvisionAdapter implements DeviceAdapter {

    private static final DateTimeFormatter DEVICE_EVENT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final HikvisionIsapiClient isapiClient;
    private final CredentialsCryptoService credentialsCryptoService;

    public HikvisionAdapter(HikvisionIsapiClient isapiClient, CredentialsCryptoService credentialsCryptoService) {
        this.isapiClient = isapiClient;
        this.credentialsCryptoService = credentialsCryptoService;
    }

    @Override
    public Device.DeviceType supportedType() {
        return Device.DeviceType.HIKVISION;
    }

    @Override
    public List<PulledDeviceEvent> pullEvents(Device device, Instant since) {
        String password = credentialsCryptoService.decrypt(device.getCredentialsSecret());
        String username = device.getUsername();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Collections.emptyList();
        }

        Instant until = Instant.now();
        String json = isapiClient.fetchAccessControlEventsJson(device, username, password, since, until);
        HikvisionEventPayload payload = isapiClient.parseAccessControlEvents(json);
        if (payload.acsEvent() == null || payload.acsEvent().infoList() == null) {
            return Collections.emptyList();
        }

        List<PulledDeviceEvent> events = new ArrayList<>();
        for (HikvisionEventPayload.Info info : payload.acsEvent().infoList()) {
            events.add(mapEvent(info, json));
        }
        return events;
    }

    public List<PulledDeviceEvent> pullAlertStreamEvents(Device device, Instant since) {
        String password = credentialsCryptoService.decrypt(device.getCredentialsSecret());
        String username = device.getUsername();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Collections.emptyList();
        }

        String chunk = isapiClient.fetchAlertStreamChunk(device, username, password, java.time.Duration.ofSeconds(5));
        if (!StringUtils.hasText(chunk)) {
            return pullEvents(device, since);
        }

        HikvisionEventPayload payload = isapiClient.parseAccessControlEvents(chunk);
        if (payload.acsEvent() == null || payload.acsEvent().infoList() == null || payload.acsEvent().infoList().isEmpty()) {
            return pullEvents(device, since);
        }

        List<PulledDeviceEvent> events = new ArrayList<>();
        for (HikvisionEventPayload.Info info : payload.acsEvent().infoList()) {
            Instant eventTime = parseEventTime(info.time());
            if (eventTime.isBefore(since)) {
                continue;
            }
            events.add(mapEvent(info, chunk));
        }
        return events;
    }

    private PulledDeviceEvent mapEvent(HikvisionEventPayload.Info info, String rawPayload) {
        return new PulledDeviceEvent(
                info.serialNo() == null ? null : String.valueOf(info.serialNo()),
                trimToNull(info.employeeNoString()),
                trimToNull(info.cardNo()),
                parseEventTime(info.time()),
                resolveDirection(info),
                resolveAuthType(info.currentVerifyMode()),
                rawPayload
        );
    }

    private Instant parseEventTime(String value) {
        if (!StringUtils.hasText(value)) {
            return Instant.now();
        }
        try {
            if (value.endsWith("Z")) {
                return Instant.parse(value);
            }
            return LocalDateTime.parse(value, DEVICE_EVENT_TIME).toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private String resolveDirection(HikvisionEventPayload.Info info) {
        if (info.minor() != null && (info.minor() == 75 || info.minor() == 1)) {
            return "IN";
        }
        if (info.minor() != null && (info.minor() == 76 || info.minor() == 2)) {
            return "OUT";
        }
        return "UNKNOWN";
    }

    private String resolveAuthType(String verifyMode) {
        if (!StringUtils.hasText(verifyMode)) {
            return null;
        }
        String normalized = verifyMode.trim().toLowerCase();
        if (normalized.contains("face")) {
            return "FACE";
        }
        if (normalized.contains("card")) {
            return "CARD";
        }
        if (normalized.contains("fp") || normalized.contains("finger")) {
            return "FINGERPRINT";
        }
        return "QR";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
