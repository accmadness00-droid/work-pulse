package uz.workpulse.device.application;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.device.domain.EmployeeCredential;
import uz.workpulse.device.dto.CreateEmployeeCredentialRequest;
import uz.workpulse.device.dto.EmployeeCredentialResponse;
import uz.workpulse.device.infrastructure.EmployeeCredentialRepository;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class EmployeeCredentialService {

    private final EmployeeCredentialRepository credentialRepository;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;

    public EmployeeCredentialService(
            EmployeeCredentialRepository credentialRepository,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService
    ) {
        this.credentialRepository = credentialRepository;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public EmployeeCredentialResponse create(CreateEmployeeCredentialRequest request) {
        requireCredentialWriteAccess(request.employeeId());
        validateEmployee(request.employeeId());
        ensureCredentialUnique(request.credentialType(), request.externalId());

        EmployeeCredential credential = new EmployeeCredential(
                request.employeeId(),
                request.credentialType(),
                normalizeExternalId(request.externalId())
        );
        return EmployeeCredentialResponse.from(credentialRepository.save(credential));
    }

    @Transactional(readOnly = true)
    public List<EmployeeCredentialResponse> list(UUID employeeId, EmployeeCredential.CredentialType credentialType, Boolean active) {
        requireCredentialReadAccess(employeeId);
        return credentialRepository.findAll(toSpecification(employeeId, credentialType, active)).stream()
                .map(EmployeeCredentialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeCredentialResponse getById(UUID id) {
        EmployeeCredential credential = getCredentialOrThrow(id);
        requireCredentialReadAccess(credential.getEmployeeId());
        return EmployeeCredentialResponse.from(credential);
    }

    @Transactional
    public void delete(UUID id) {
        deactivate(id);
    }

    @Transactional
    public EmployeeCredentialResponse activate(UUID id) {
        EmployeeCredential credential = getCredentialOrThrow(id);
        requireCredentialWriteAccess(credential.getEmployeeId());
        credential.activate();
        return EmployeeCredentialResponse.from(credentialRepository.save(credential));
    }

    @Transactional
    public EmployeeCredentialResponse deactivate(UUID id) {
        EmployeeCredential credential = getCredentialOrThrow(id);
        requireCredentialWriteAccess(credential.getEmployeeId());
        credential.deactivate();
        return EmployeeCredentialResponse.from(credentialRepository.save(credential));
    }

    private EmployeeCredential getCredentialOrThrow(UUID id) {
        return credentialRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_CREDENTIAL_NOT_FOUND));
    }

    private void validateEmployee(UUID employeeId) {
        if (!employeeFacade.existsAndActive(employeeId)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND);
        }
    }

    private void ensureCredentialUnique(EmployeeCredential.CredentialType credentialType, String externalId) {
        if (credentialRepository.existsByCredentialTypeAndExternalId(credentialType, normalizeExternalId(externalId))) {
            throw new BusinessException(ErrorCode.EMPLOYEE_CREDENTIAL_ALREADY_EXISTS);
        }
    }

    private String normalizeExternalId(String externalId) {
        return externalId.trim();
    }

    private Specification<EmployeeCredential> toSpecification(UUID employeeId, EmployeeCredential.CredentialType credentialType, Boolean active) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), employeeId));
            }
            if (credentialType != null) {
                predicates.add(criteriaBuilder.equal(root.get("credentialType"), credentialType));
            }
            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void requireCredentialReadAccess(UUID employeeId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (employeeId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "employeeId is required");
        }
        EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        accessControlService.requireEmployeeSelfOrAdmin(
                principal,
                scope.userId(),
                scope.companyId(),
                scope.branchId()
        );
    }

    private void requireCredentialWriteAccess(UUID employeeId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN || principal.role() == User.Role.COMPANY_ADMIN) {
            EmployeeFacade.EmployeeScope scope = employeeFacade.findEmployeeScope(employeeId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
            accessControlService.requireCompanyAccess(principal, scope.companyId());
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
