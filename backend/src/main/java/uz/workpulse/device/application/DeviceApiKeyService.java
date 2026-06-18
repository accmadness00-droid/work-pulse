package uz.workpulse.device.application;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class DeviceApiKeyService {

    private final DeviceRepository deviceRepository;
    private final PasswordService passwordService;

    public DeviceApiKeyService(DeviceRepository deviceRepository, PasswordService passwordService) {
        this.deviceRepository = deviceRepository;
        this.passwordService = passwordService;
    }

    public Device authenticateDevice(String serialNumber, String plainKey) {
        if (!StringUtils.hasText(serialNumber) || !StringUtils.hasText(plainKey)) {
            throw new BusinessException(ErrorCode.DEVICE_API_KEY_MISSING);
        }

        Device device = deviceRepository.findBySerialNumber(normalizeSerial(serialNumber))
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_API_KEY_INVALID));
        if (device.getStatus() != Device.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_INACTIVE);
        }
        if (device.getConnectionType() != Device.ConnectionType.PUSH) {
            throw new BusinessException(ErrorCode.DEVICE_CONNECTION_INVALID, "Device is not configured for PUSH mode");
        }
        if (!StringUtils.hasText(device.getApiKeyHash()) || !passwordService.matches(plainKey, device.getApiKeyHash())) {
            throw new BusinessException(ErrorCode.DEVICE_API_KEY_INVALID);
        }
        return device;
    }

    public boolean validateDeviceApiKey(String serialNumber, String plainKey) {
        authenticateDevice(serialNumber, plainKey);
        return true;
    }

    private String normalizeSerial(String serialNumber) {
        return serialNumber.trim().toUpperCase();
    }
}
