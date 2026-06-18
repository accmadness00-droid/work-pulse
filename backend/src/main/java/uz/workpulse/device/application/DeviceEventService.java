package uz.workpulse.device.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.device.domain.Device;
import uz.workpulse.device.domain.DeviceEvent;
import uz.workpulse.device.dto.DeviceEventFilterRequest;
import uz.workpulse.device.dto.DeviceEventResponse;
import uz.workpulse.device.dto.IngestDeviceEventRequest;
import uz.workpulse.device.dto.ProcessDeviceEventResponse;
import uz.workpulse.device.infrastructure.DeviceEventRepository;
import uz.workpulse.device.infrastructure.DeviceRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;
import uz.workpulse.shared.security.DevicePushPrincipal;

@Service
public class DeviceEventService {

    private final DeviceRepository deviceRepository;
    private final DeviceEventRepository eventRepository;
    private final DeviceEventProcessor processor;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final BranchFacade branchFacade;

    public DeviceEventService(
            DeviceRepository deviceRepository,
            DeviceEventRepository eventRepository,
            DeviceEventProcessor processor,
            ObjectMapper objectMapper,
            AccessControlService accessControlService,
            BranchFacade branchFacade
    ) {
        this.deviceRepository = deviceRepository;
        this.eventRepository = eventRepository;
        this.processor = processor;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.branchFacade = branchFacade;
    }

    @Transactional
    public DeviceEventResponse ingest(IngestDeviceEventRequest request) {
        Device device = resolveDeviceForIngest(request);
        if (device.getStatus() != Device.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_INACTIVE);
        }

        String eventHash = generateEventHash(device.getId(), request);
        return eventRepository.findByDeviceIdAndEventHash(device.getId(), eventHash)
                .map(DeviceEventResponse::from)
                .orElseGet(() -> saveNewEvent(device, eventHash, request));
    }

    @Transactional(readOnly = true)
    public Page<DeviceEventResponse> list(DeviceEventFilterRequest filter, Pageable pageable) {
        requireDeviceEventReadAccess();
        return eventRepository.findAll(toSpecification(filter), pageable)
                .map(DeviceEventResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<DeviceEventResponse> getUnprocessed(Pageable pageable) {
        return list(new DeviceEventFilterRequest(null, false, null, null, null, null), pageable);
    }

    @Transactional(readOnly = true)
    public DeviceEventResponse getById(UUID id) {
        requireDeviceEventReadAccess();
        return DeviceEventResponse.from(getEventOrThrow(id));
    }

    public ProcessDeviceEventResponse processOne(UUID id) {
        requireDeviceEventWriteAccess();
        return processor.processEvent(id);
    }

    public int processBatch() {
        requireDeviceEventWriteAccess();
        return processor.processBatch();
    }

    @Transactional
    public void ingestInternal(Device device, IngestDeviceEventRequest request) {
        if (device.getStatus() != Device.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.DEVICE_INACTIVE);
        }
        String eventHash = generateEventHash(device.getId(), request);
        if (eventRepository.findByDeviceIdAndEventHash(device.getId(), eventHash).isPresent()) {
            return;
        }
        saveNewEvent(device, eventHash, request);
    }

    private Device resolveDeviceForIngest(IngestDeviceEventRequest request) {
        if (accessControlService.isDevicePush()) {
            DevicePushPrincipal devicePush = accessControlService.currentDevicePush();
            return deviceRepository.findById(devicePush.deviceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
        }

        requireDeviceEventWriteAccess();
        String serial = StringUtils.hasText(request.deviceSerialNumber())
                ? request.deviceSerialNumber()
                : null;
        if (!StringUtils.hasText(serial)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "deviceSerialNumber is required");
        }
        return deviceRepository.findBySerialNumber(normalizeSerial(serial))
                .map(device -> {
                    UUID companyId = branchFacade.getCompanyId(device.getBranchId());
                    accessControlService.requireBranchAccess(accessControlService.currentUser(), companyId, device.getBranchId());
                    return device;
                })
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_NOT_FOUND));
    }

    private DeviceEventResponse saveNewEvent(Device device, String eventHash, IngestDeviceEventRequest request) {
        DeviceEvent event = new DeviceEvent(device.getId(), request.externalEventId(), eventHash, request.eventTime(), request.direction());
        event.setEmployeeCode(trimToNull(request.employeeCode()));
        event.setCredentialValue(trimToNull(request.credentialValue()));
        event.setAuthType(request.authType());
        event.setRawPayload(rawPayloadToString(request.rawPayload()));

        try {
            DeviceEvent saved = eventRepository.saveAndFlush(event);
            if (saved.getDirection() != DeviceEvent.Direction.UNKNOWN) {
                processor.processEvent(saved.getId());
            }
            return DeviceEventResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            return eventRepository.findByDeviceIdAndEventHash(device.getId(), eventHash)
                    .map(DeviceEventResponse::from)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EVENT_DUPLICATE));
        }
    }

    private DeviceEvent getEventOrThrow(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_EVENT_NOT_FOUND));
    }

    private String generateEventHash(UUID deviceId, IngestDeviceEventRequest request) {
        String normalizedPayload = normalizeRawPayload(request.rawPayload());
        String source = StringUtils.hasText(request.externalEventId())
                ? deviceId + "|" + request.externalEventId().trim()
                : deviceId + "|" + request.eventTime() + "|" + nullSafe(request.employeeCode()) + "|"
                + nullSafe(request.credentialValue()) + "|" + request.direction() + "|" + request.authType() + "|"
                + normalizedPayload;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.DEVICE_EVENT_HASH_GENERATION_FAILED);
        }
    }

    private String normalizeRawPayload(JsonNode rawPayload) {
        if (rawPayload == null || rawPayload.isNull()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return rawPayload.toString();
        }
    }

    private Specification<DeviceEvent> toSpecification(DeviceEventFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.deviceId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("deviceId"), filter.deviceId()));
            }
            if (filter.processed() != null) {
                predicates.add(criteriaBuilder.equal(root.get("processed"), filter.processed()));
            }
            if (filter.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventTime"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("eventTime"), filter.to()));
            }
            if (filter.direction() != null) {
                predicates.add(criteriaBuilder.equal(root.get("direction"), filter.direction()));
            }
            if (filter.authType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("authType"), filter.authType()));
            }
            applyTenantScope(predicates, root, criteriaBuilder);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void applyTenantScope(List<Predicate> predicates, jakarta.persistence.criteria.Root<DeviceEvent> root, jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder) {
        if (accessControlService.isDevicePush()) {
            return;
        }
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            List<UUID> deviceIds = deviceRepository.findIdsByBranchId(principal.branchId());
            predicates.add(deviceIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("deviceId").in(deviceIds));
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN) {
            List<UUID> deviceIds = deviceRepository.findIdsByCompanyId(principal.companyId());
            predicates.add(deviceIds.isEmpty()
                    ? criteriaBuilder.disjunction()
                    : root.get("deviceId").in(deviceIds));
        }
    }

    private String rawPayloadToString(JsonNode rawPayload) {
        if (rawPayload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rawPayload);
        } catch (Exception ex) {
            return rawPayload.toString();
        }
    }

    private String normalizeSerial(String serialNumber) {
        return serialNumber.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private void requireDeviceEventReadAccess() {
        if (accessControlService.isDevicePush()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        User.Role role = accessControlService.currentUser().role();
        if (role == User.Role.SUPER_ADMIN || role == User.Role.COMPANY_ADMIN || role == User.Role.BRANCH_MANAGER) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireDeviceEventWriteAccess() {
        if (accessControlService.isDevicePush()) {
            return;
        }
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN
                || principal.role() == User.Role.COMPANY_ADMIN
                || principal.role() == User.Role.BRANCH_MANAGER) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
