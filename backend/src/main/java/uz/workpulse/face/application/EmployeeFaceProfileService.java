package uz.workpulse.face.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uz.workpulse.face.domain.EmployeeFaceProfile;
import uz.workpulse.face.domain.FaceEmbeddingResponse;
import uz.workpulse.face.dto.EmployeeFaceProfileResponse;
import uz.workpulse.face.dto.EnrollFaceResponse;
import uz.workpulse.face.infrastructure.EmployeeFaceProfileRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.employee.application.EmployeePhotoService;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.file.FileStorageService;
import uz.workpulse.shared.file.StoredFile;

@Service
@EnableConfigurationProperties(FaceProperties.class)
public class EmployeeFaceProfileService {
    private final EmployeeFaceProfileRepository repository;
    private final FaceServiceClient faceServiceClient;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final EmployeeFacade employeeFacade;
    private final EmployeePhotoService employeePhotoService;
    private final AccessControlService accessControlService;
    private final FaceProperties properties;

    public EmployeeFaceProfileService(
            EmployeeFaceProfileRepository repository,
            FaceServiceClient faceServiceClient,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper,
            EmployeeFacade employeeFacade,
            EmployeePhotoService employeePhotoService,
            AccessControlService accessControlService,
            FaceProperties properties
    ) {
        this.repository = repository;
        this.faceServiceClient = faceServiceClient;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.employeeFacade = employeeFacade;
        this.employeePhotoService = employeePhotoService;
        this.accessControlService = accessControlService;
        this.properties = properties;
    }

    @Transactional
    public EnrollFaceResponse enroll(UUID employeeId, MultipartFile file) {
        requireFaceProfileAccess(employeeId);
        StoredFile storedFile = fileStorageService.saveEmployeePhoto(employeeId, file);

        FaceEmbeddingResponse embeddingResponse;
        try {
            embeddingResponse = faceServiceClient.extractEmbedding(file.getBytes());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FACE_SERVICE_UNAVAILABLE);
        }

        if (embeddingResponse == null || !embeddingResponse.isSuccess()) {
            throw faceError(embeddingResponse == null ? null : embeddingResponse.getError());
        }

        EmployeeFaceProfile profile = new EmployeeFaceProfile();
        profile.setEmployeeId(employeeId);
        profile.setPhotoUrl(storedFile.getUrl());
        profile.setModelName(embeddingResponse.getModelName() == null ? "unknown" : embeddingResponse.getModelName());
        profile.setThreshold(BigDecimal.valueOf(properties.getThreshold()));
        profile.setActive(true);
        try {
            profile.setEmbeddingJson(objectMapper.writeValueAsString(embeddingResponse.getEmbedding()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        EmployeeFaceProfile saved = repository.save(profile);
        employeePhotoService.updatePhotoUrl(employeeId, storedFile.getUrl());

        return new EnrollFaceResponse(employeeId, saved.getId(), storedFile.getUrl(), saved.getModelName(), saved.isActive());
    }

    @Transactional(readOnly = true)
    public List<EmployeeFaceProfileResponse> list(UUID employeeId) {
        requireFaceProfileAccess(employeeId);
        return repository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(EmployeeFaceProfileResponse::from)
                .toList();
    }

    @Transactional
    public EmployeeFaceProfileResponse deactivate(UUID employeeId, UUID profileId) {
        requireFaceProfileAccess(employeeId);
        EmployeeFaceProfile profile = repository.findById(profileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_FACE_PROFILE_NOT_FOUND));
        if (!profile.getEmployeeId().equals(employeeId)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_FACE_PROFILE_NOT_FOUND);
        }
        profile.deactivate();
        return EmployeeFaceProfileResponse.from(repository.save(profile));
    }

    private void requireFaceProfileAccess(UUID employeeId) {
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        accessControlService.requireEmployeeSelfOrAdmin(
                accessControlService.currentUser(),
                scope.userId(),
                scope.companyId(),
                scope.branchId()
        );
    }

    private BusinessException faceError(String error) {
        if (error != null) {
            if (error.contains("FACE_NOT_DETECTED")) {
                return new BusinessException(ErrorCode.FACE_NOT_DETECTED);
            }
            if (error.contains("FACE_MULTIPLE_DETECTED")) {
                return new BusinessException(ErrorCode.FACE_MULTIPLE_DETECTED);
            }
        }
        return new BusinessException(ErrorCode.FACE_SERVICE_UNAVAILABLE);
    }
}
