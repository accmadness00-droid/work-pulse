package uz.workpulse.device.api;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.device.application.DeviceEventService;
import uz.workpulse.device.domain.DeviceEvent;
import uz.workpulse.device.dto.DeviceEventFilterRequest;
import uz.workpulse.device.dto.DeviceEventResponse;
import uz.workpulse.device.dto.IngestDeviceEventRequest;
import uz.workpulse.device.dto.ProcessDeviceEventResponse;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/device-events")
public class DeviceEventController {

    private final DeviceEventService deviceEventService;

    public DeviceEventController(DeviceEventService deviceEventService) {
        this.deviceEventService = deviceEventService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','BRANCH_MANAGER') or hasAuthority('DEVICE_PUSH')")
    public ResponseEntity<ApiResponse<DeviceEventResponse>> ingest(@Valid @RequestBody IngestDeviceEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.ingest(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Page<DeviceEventResponse>>> list(
            @RequestParam(required = false) UUID deviceId,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) DeviceEvent.Direction direction,
            @RequestParam(required = false) DeviceEvent.AuthType authType,
            Pageable pageable
    ) {
        DeviceEventFilterRequest filter = new DeviceEventFilterRequest(deviceId, processed, from, to, direction, authType);
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.list(filter, pageable)));
    }

    @GetMapping("/unprocessed")
    public ResponseEntity<ApiResponse<Page<DeviceEventResponse>>> unprocessed(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.getUnprocessed(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceEventResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.getById(id)));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<ProcessDeviceEventResponse>> processOne(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.processOne(id)));
    }

    @PostMapping("/process-batch")
    public ResponseEntity<ApiResponse<Integer>> processBatch() {
        return ResponseEntity.ok(ApiResponse.success(deviceEventService.processBatch()));
    }
}
