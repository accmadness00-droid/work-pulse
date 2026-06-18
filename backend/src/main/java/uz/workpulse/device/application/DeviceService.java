package uz.workpulse.device.application;

import jakarta.persistence.criteria.Predicate;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.branch.infrastructure.BranchRepository;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.dto.CreateDeviceRequest;
import uz.workpulse.device.dto.DeviceFilterRequest;
import uz.workpulse.device.dto.DeviceResponse;
import uz.workpulse.device.dto.RotateDeviceApiKeyResponse;
import uz.workpulse.device.dto.UpdateDeviceRequest;
import uz.workpulse.device.dto.UpdateDeviceStatusRequest;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;
import uz.workpulse.shared.security.CredentialsCryptoService;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final BranchRepository branchRepository;
    private final BranchFacade branchFacade;
    private final PasswordService passwordService;
    private final CredentialsCryptoService credentialsCryptoService;
    private final AccessControlService accessControlService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceService(
            DeviceRepository deviceRepository,
            BranchRepository branchRepository,
            BranchFacade branchFacade,
            PasswordService passwordService,
            CredentialsCryptoService credentialsCryptoService,
            AccessControlService accessControlService
    ) {
        this.deviceRepository = deviceRepository;
        this.branchRepository = branchRepository;
        this.branchFacade = branchFacade;
        this.passwordService = passwordService;
        this.credentialsCryptoService = credentialsCryptoService;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public DeviceResponse create(CreateDeviceRequest request) {
        requireDeviceWriteAccess(request.branchId());
        validateBranch(request.branchId());
        validateConnection(request.connectionType(), request.ipAddress(), request.port());
        ensureSerialUnique(request.serialNumber());

        Device device = new Device(
                request.name().trim(),
                normalizeSerial(request.serialNumber()),
                request.branchId(),
                request.type(),
                request.connectionType()
        );
        applyFields(device, request.ipAddress(), request.port(), request.username(), request.credentialsSecret());
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse update(UUID id, UpdateDeviceRequest request) {
        Device device = getDeviceOrThrow(id);
        requireDeviceWriteAccess(device.getBranchId());
        validateBranch(request.branchId());
        validateConnection(request.connectionType(), request.ipAddress(), request.port());
        ensureSerialUniqueForUpdate(request.serialNumber(), id);

        device.setName(request.name().trim());
        device.setSerialNumber(normalizeSerial(request.serialNumber()));
        device.setBranchId(request.branchId());
        device.setType(request.type());
        device.setConnectionType(request.connectionType());
        applyFields(device, request.ipAddress(), request.port(), request.username(), request.credentialsSecret());
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional(readOnly = true)
    public DeviceResponse getById(UUID id) {
        Device device = getDeviceOrThrow(id);
        requireDeviceReadAccess(device.getBranchId());
        return DeviceResponse.from(device);
    }

    @Transactional(readOnly = true)
    public Page<DeviceResponse> list(DeviceFilterRequest filter, Pageable pageable) {
        requireDeviceListAccess();
        DeviceFilterRequest scopedFilter = applyListScope(filter);
        return deviceRepository.findAll(toSpecification(scopedFilter), pageable)
                .map(DeviceResponse::from);
    }

    @Transactional
    public DeviceResponse updateStatus(UUID id, UpdateDeviceStatusRequest request) {
        Device device = getDeviceOrThrow(id);
        requireDeviceWriteAccess(device.getBranchId());
        if (request.status() == Device.Status.ACTIVE) {
            device.activate();
        } else {
            device.deactivate();
        }
        return DeviceResponse.from(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse activate(UUID id) {
        return updateStatus(id, new UpdateDeviceStatusRequest(Device.Status.ACTIVE));
    }

    @Transactional
    public DeviceResponse deactivate(UUID id) {
        return updateStatus(id, new UpdateDeviceStatusRequest(Device.Status.INACTIVE));
    }

    @Transactional
    public RotateDeviceApiKeyResponse rotateApiKey(UUID id) {
        Device device = getDeviceOrThrow(id);
        requireDeviceWriteAccess(device.getBranchId());
        if (device.getStatus() != Device.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_INACTIVE);
        }
        if (device.getConnectionType() != Device.ConnectionType.PUSH) {
            throw new BusinessException(ErrorCode.DEVICE_CONNECTION_INVALID, "API key rotation is only supported for PUSH devices");
        }

        String plainKey = generatePlainApiKey();
        device.setApiKeyHash(passwordService.encode(plainKey));
        deviceRepository.save(device);
        return new RotateDeviceApiKeyResponse(device.getId(), device.getSerialNumber(), plainKey);
    }

    private Device getDeviceOrThrow(UUID id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
    }

    private void validateBranch(UUID branchId) {
        branchFacade.ensureActiveBranch(branchId);
    }

    private void validateConnection(Device.ConnectionType connectionType, String ipAddress, Integer port) {
        if (connectionType == Device.ConnectionType.POLLING || connectionType == Device.ConnectionType.ALERT_STREAM) {
            if (!StringUtils.hasText(ipAddress) || port == null || port <= 0) {
                throw new BusinessException(ErrorCode.DEVICE_CONNECTION_INVALID);
            }
        }
        if (connectionType == Device.ConnectionType.PUSH && !StringUtils.hasText(ipAddress)) {
            return;
        }
    }

    private void applyFields(Device device, String ipAddress, Integer port, String username, String credentialsSecret) {
        device.setIpAddress(ipAddress);
        device.setPort(port == null ? 80 : port);
        device.setUsername(username);
        if (StringUtils.hasText(credentialsSecret)) {
            device.setCredentialsSecret(credentialsCryptoService.encrypt(credentialsSecret));
        }
    }

    private void ensureSerialUnique(String serialNumber) {
        if (deviceRepository.existsBySerialNumber(normalizeSerial(serialNumber))) {
            throw new BusinessException(ErrorCode.DEVICE_SERIAL_ALREADY_EXISTS);
        }
    }

    private void ensureSerialUniqueForUpdate(String serialNumber, UUID id) {
        if (deviceRepository.existsBySerialNumberAndIdNot(normalizeSerial(serialNumber), id)) {
            throw new BusinessException(ErrorCode.DEVICE_SERIAL_ALREADY_EXISTS);
        }
    }

    private String normalizeSerial(String serialNumber) {
        return serialNumber.trim().toUpperCase();
    }

    private String generatePlainApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "wpd_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Specification<Device> toSpecification(DeviceFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.branchId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("branchId"), filter.branchId()));
            }
            if (filter.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), filter.type()));
            }
            if (filter.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.status()));
            }
            if (filter.connectionType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("connectionType"), filter.connectionType()));
            }
            applyTenantScope(predicates, root, criteriaBuilder, filter);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyTenantScope(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Device> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder,
            DeviceFilterRequest filter
    ) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            predicates.add(criteriaBuilder.equal(root.get("branchId"), principal.branchId()));
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN && filter.branchId() == null) {
            List<UUID> branchIds = branchRepository.findIdsByCompanyId(principal.companyId());
            predicates.add(branchIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("branchId").in(branchIds));
        }
    }

    private void requireDeviceListAccess() {
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN || role == User.Role.BRANCH_MANAGER) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private DeviceFilterRequest applyListScope(DeviceFilterRequest filter) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            return new DeviceFilterRequest(
                    principal.branchId(),
                    filter.type(),
                    filter.status(),
                    filter.connectionType()
            );
        }
        return filter;
    }

    private void requireDeviceReadAccess(UUID branchId) {
        UUID companyId = branchFacade.getCompanyId(branchId);
        accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, branchId);
    }

    private void requireDeviceWriteAccess(UUID branchId) {
        UUID companyId = branchFacade.getCompanyId(branchId);
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(accessControlService.currentUser(), companyId);
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
