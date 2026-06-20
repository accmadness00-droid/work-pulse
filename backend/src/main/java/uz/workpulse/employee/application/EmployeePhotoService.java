package uz.workpulse.employee.application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.dto.EmployeeResponse;
import uz.workpulse.employee.infrastructure.EmployeeRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;

@Service
public class EmployeePhotoService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;
    private final Path photoDirectory;

    public EmployeePhotoService(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService,
            @Value("${app.uploads.photo-dir:uploads/photos}") String photoDir
    ) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
        this.photoDirectory = Path.of(photoDir);
    }

    @Transactional
    public EmployeeResponse uploadPhoto(UUID employeeId, MultipartFile file) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        accessControlService.requireBranchAccess(
                accessControlService.currentUser(),
                scope.companyId(),
                scope.branchId()
        );

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Photo file is required");
        }

        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        String filename = employeeId + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(photoDirectory);
            Path target = photoDirectory.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            employee.setPhotoUrl("/uploads/photos/" + filename);
            return toResponse(employeeRepository.save(employee));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Unable to store employee photo");
        }
    }

    @Transactional
    public EmployeeResponse updatePhotoUrl(UUID employeeId, String photoUrl) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        employee.setPhotoUrl(photoUrl);
        return toResponse(employeeRepository.save(employee));
    }

    private EmployeeResponse toResponse(Employee employee) {
        String email = employee.getUserId() == null
                ? null
                : userRepository.findById(employee.getUserId()).map(User::getEmail).orElse(null);
        return EmployeeResponse.from(employee, email);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        return ".bin";
    }
}
