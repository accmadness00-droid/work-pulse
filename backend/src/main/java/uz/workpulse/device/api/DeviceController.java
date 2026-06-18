package uz.workpulse.device.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.device.application.DeviceService;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.CreateDeviceRequest;
import uz.workpulse.device.dto.DeviceFilterRequest;
import uz.workpulse.device.dto.DeviceResponse;
import uz.workpulse.device.dto.RotateDeviceApiKeyResponse;
import uz.workpulse.device.dto.UpdateDeviceRequest;
import uz.workpulse.device.dto.UpdateDeviceStatusRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DeviceResponse>>> listDevices(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Device.DeviceType type,
            @RequestParam(required = false) Device.Status status,
            @RequestParam(required = false) Device.ConnectionType connectionType,
            Pageable pageable
    ) {
        DeviceFilterRequest filter = new DeviceFilterRequest(branchId, type, status, connectionType);
        return ResponseEntity.ok(ApiResponse.success(deviceService.list(filter, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(@Valid @RequestBody CreateDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeviceStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.updateStatus(id, request)));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<DeviceResponse>> activateDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<DeviceResponse>> deactivateDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.deactivate(id)));
    }

    @PostMapping("/{id}/rotate-key")
    public ResponseEntity<ApiResponse<RotateDeviceApiKeyResponse>> rotateApiKey(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(deviceService.rotateApiKey(id)));
    }
}
