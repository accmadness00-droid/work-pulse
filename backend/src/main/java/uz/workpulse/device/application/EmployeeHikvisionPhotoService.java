package uz.workpulse.device.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.device.adapter.HikvisionIsapiClient;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.HikvisionPhotoSyncResponse;
import uz.workpulse.device.dto.HikvisionPhotoSyncResult;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.infrastructure.EmployeeRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.file.FileStorageProperties;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.CredentialsCryptoService;

@Service
@EnableConfigurationProperties(FileStorageProperties.class)
public class EmployeeHikvisionPhotoService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeFacade employeeFacade;
    private final DeviceRepository deviceRepository;
    private final HikvisionIsapiClient hikvisionIsapiClient;
    private final CredentialsCryptoService credentialsCryptoService;
    private final AccessControlService accessControlService;
    private final FileStorageProperties fileStorageProperties;
    private final Path photoDirectory;

    public EmployeeHikvisionPhotoService(
            EmployeeRepository employeeRepository,
            EmployeeFacade employeeFacade,
            DeviceRepository deviceRepository,
            HikvisionIsapiClient hikvisionIsapiClient,
            CredentialsCryptoService credentialsCryptoService,
            AccessControlService accessControlService,
            FileStorageProperties fileStorageProperties,
            @Value("${app.uploads.photo-dir:uploads/photos}") String photoDir
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeFacade = employeeFacade;
        this.deviceRepository = deviceRepository;
        this.hikvisionIsapiClient = hikvisionIsapiClient;
        this.credentialsCryptoService = credentialsCryptoService;
        this.accessControlService = accessControlService;
        this.fileStorageProperties = fileStorageProperties;
        this.photoDirectory = Path.of(photoDir).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public HikvisionPhotoSyncResponse syncEmployeePhoto(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        accessControlService.requireBranchAccess(accessControlService.currentUser(), scope.companyId(), scope.branchId());

        if (!StringUtils.hasText(employee.getPhotoUrl())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Employee photo is required");
        }

        Path photoPath = resolvePhotoPath(employee.getPhotoUrl());
        byte[] imageBytes;
        try {
            imageBytes = Files.readAllBytes(photoPath);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Employee photo file was not found");
        }

        String contentType = contentType(photoPath);
        String displayName = (employee.getFirstName() + " " + employee.getLastName()).trim();
        List<Device> devices = deviceRepository.findAllByBranchIdAndTypeAndStatus(
                employee.getBranchId(),
                Device.DeviceType.HIKVISION,
                Device.Status.ACTIVE
        );

        List<HikvisionPhotoSyncResult> results = devices.stream()
                .map(device -> syncDevice(device, employee.getEmployeeCode(), displayName, imageBytes, contentType))
                .toList();
        int successCount = (int) results.stream().filter(HikvisionPhotoSyncResult::success).count();
        return new HikvisionPhotoSyncResponse(
                employeeId,
                results.size(),
                successCount,
                results.size() - successCount,
                results
        );
    }

    private HikvisionPhotoSyncResult syncDevice(
            Device device,
            String employeeCode,
            String displayName,
            byte[] imageBytes,
            String contentType
    ) {
        if (!StringUtils.hasText(device.getIpAddress())) {
            return new HikvisionPhotoSyncResult(device.getId(), device.getName(), false, "Device IP address is missing");
        }
        if (!StringUtils.hasText(device.getUsername()) || !StringUtils.hasText(device.getCredentialsSecret())) {
            return new HikvisionPhotoSyncResult(device.getId(), device.getName(), false, "Device credentials are missing");
        }
        try {
            String password = credentialsCryptoService.decrypt(device.getCredentialsSecret());
            hikvisionIsapiClient.uploadEmployeeFace(
                    device,
                    device.getUsername(),
                    password,
                    employeeCode,
                    displayName,
                    imageBytes,
                    contentType
            );
            return new HikvisionPhotoSyncResult(device.getId(), device.getName(), true, "Synced");
        } catch (Exception ex) {
            return new HikvisionPhotoSyncResult(device.getId(), device.getName(), false, ex.getMessage());
        }
    }

    private Path resolvePhotoPath(String photoUrl) {
        String normalized = photoUrl.trim();
        if (normalized.startsWith("/uploads/photos/")) {
            Path resolved = photoDirectory.resolve(normalized.substring("/uploads/photos/".length())).normalize();
            if (!resolved.startsWith(photoDirectory)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid employee photo path");
            }
            return resolved;
        }
        String publicBaseUrl = fileStorageProperties.getPublicBaseUrl().replaceAll("/$", "");
        if (normalized.startsWith(publicBaseUrl + "/")) {
            String relativePath = normalized.substring((publicBaseUrl + "/").length());
            Path uploadDirectory = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
            Path resolved = uploadDirectory.resolve(relativePath).normalize();
            if (!resolved.startsWith(uploadDirectory)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid employee photo path");
            }
            return resolved;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Only local employee photos can be synced to Hikvision");
    }

    private String contentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
