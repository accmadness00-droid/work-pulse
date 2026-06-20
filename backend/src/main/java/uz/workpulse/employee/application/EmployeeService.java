package uz.workpulse.employee.application;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.branch.application.BranchService;
import uz.workpulse.branch.dto.BranchResponse;
import uz.workpulse.company.application.CompanyService;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.dto.CreateEmployeeRequest;
import uz.workpulse.employee.dto.EmployeeFilterRequest;
import uz.workpulse.employee.dto.EmployeeResponse;
import uz.workpulse.employee.dto.GeneratedEmployeeCodeResponse;
import uz.workpulse.employee.dto.UpdateEmployeeRequest;
import uz.workpulse.employee.infrastructure.EmployeeRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class EmployeeService implements EmployeeFacade {

    private static final String EMPLOYEE_CODE_PREFIX = "EMP";
    private static final Pattern EMPLOYEE_CODE_PATTERN = Pattern.compile("^" + EMPLOYEE_CODE_PREFIX + "(\\d+)$");

    private final EmployeeRepository employeeRepository;
    private final EmployeeQueryService employeeQueryService;
    private final CompanyService companyService;
    private final BranchService branchService;
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final AccessControlService accessControlService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeService(
            EmployeeRepository employeeRepository,
            EmployeeQueryService employeeQueryService,
            CompanyService companyService,
            BranchService branchService,
            UserRepository userRepository,
            PasswordService passwordService,
            AccessControlService accessControlService
    ) {
        this.employeeRepository = employeeRepository;
        this.employeeQueryService = employeeQueryService;
        this.companyService = companyService;
        this.branchService = branchService;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        requireEmployeeAdminWriteAccess(request.companyId(), request.branchId());
        validateCompanyAndBranch(request.companyId(), request.branchId());
        ensureEmployeeCodeUnique(request.employeeCode());

        Employee employee = new Employee(
                request.companyId(),
                request.branchId(),
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeEmployeeCode(request.employeeCode())
        );
        employee.setUserId(resolveOrCreateUser(request));
        applyCreateFields(employee, request);
        syncLinkedUserScope(employee);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(UUID id, UpdateEmployeeRequest request) {
        Employee employee = getEmployeeOrThrow(id);
        if (!employee.isActive()) {
            throw new BusinessException(ErrorCode.EMPLOYEE_INACTIVE);
        }
        requireEmployeeUpdateAccess(employee.getCompanyId(), employee.getBranchId());
        validateCompanyAndBranch(request.companyId(), request.branchId());
        ensureEmployeeCodeUniqueForUpdate(request.employeeCode(), id);

        employee.setCompanyId(request.companyId());
        employee.setBranchId(request.branchId());
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmployeeCode(normalizeEmployeeCode(request.employeeCode()));
        applyUpdateFields(employee, request);
        syncLinkedUserScope(employee);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        Employee employee = getEmployeeOrThrow(id);
        requireEmployeeReadAccess(employee);
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getMe() {
        AuthPrincipal principal = accessControlService.currentUser();
        Employee employee = employeeRepository.findByUserIdAndActiveTrue(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return toResponse(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(EmployeeFilterRequest filter, Pageable pageable) {
        EmployeeFilterRequest scopedFilter = applyListScope(filter);
        requireEmployeeListAccess(scopedFilter);
        return employeeQueryService.findEmployees(scopedFilter, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GeneratedEmployeeCodeResponse generateEmployeeCode() {
        requireEmployeeCodeGenerationAccess();
        int nextNumber = employeeRepository.findEmployeeCodesByPrefix(EMPLOYEE_CODE_PREFIX)
                .stream()
                .map(this::employeeCodeNumber)
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        String employeeCode = formatEmployeeCode(nextNumber);
        while (employeeRepository.existsByEmployeeCode(employeeCode)) {
            nextNumber++;
            employeeCode = formatEmployeeCode(nextNumber);
        }
        return new GeneratedEmployeeCodeResponse(employeeCode);
    }

    @Transactional
    public void softDelete(UUID id) {
        Employee employee = getActiveEmployeeOrThrow(id);
        requireEmployeeAdminWriteAccess(employee.getCompanyId(), employee.getBranchId());
        employee.deactivate();
        employeeRepository.save(employee);
    }

    @Transactional
    public EmployeeResponse activate(UUID id) {
        Employee employee = getEmployeeOrThrow(id);
        requireEmployeeAdminWriteAccess(employee.getCompanyId(), employee.getBranchId());
        employee.activate();
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse deactivate(UUID id) {
        Employee employee = getActiveEmployeeOrThrow(id);
        requireEmployeeAdminWriteAccess(employee.getCompanyId(), employee.getBranchId());
        employee.deactivate();
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findByEmployeeCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeCode(normalizeEmployeeCode(employeeCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        requireEmployeeReadAccess(employee);
        return toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findEmployeeIdByCode(String employeeCode) {
        return employeeRepository.findByEmployeeCodeAndActiveTrue(normalizeEmployeeCode(employeeCode))
                .map(Employee::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsAndActive(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .map(Employee::isActive)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findBranchIdByEmployeeId(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .map(Employee::getBranchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeScope> findEmployeeScope(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .map(employee -> new EmployeeScope(
                        employee.getId(),
                        employee.getCompanyId(),
                        employee.getBranchId(),
                        employee.getUserId()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findEmployeeIdByUserId(UUID userId) {
        return employeeRepository.findByUserIdAndActiveTrue(userId)
                .map(Employee::getId);
    }

    private UUID resolveOrCreateUser(CreateEmployeeRequest request) {
        if (request.userId() != null) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
            if (!user.isActive()) {
                throw new BusinessException(ErrorCode.INACTIVE_USER);
            }
            return user.getId();
        }
        if (!StringUtils.hasText(request.email())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "email is required when userId is not provided");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "User email already exists");
        }
        String password = StringUtils.hasText(request.password()) ? request.password() : generateTemporaryPassword();
        User user = new User(email, passwordService.encode(password), User.Role.EMPLOYEE, true);
        user.setCompanyId(request.companyId());
        user.setBranchId(request.branchId());
        return userRepository.save(user).getId();
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private EmployeeFilterRequest applyListScope(EmployeeFilterRequest filter) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return filter;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, principal.companyId());
            return new EmployeeFilterRequest(
                    principal.companyId(),
                    filter.branchId(),
                    filter.position(),
                    filter.active(),
                    filter.search()
            );
        }
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            accessControlService.requireBranchAccess(principal, principal.companyId(), principal.branchId());
            return new EmployeeFilterRequest(
                    principal.companyId(),
                    principal.branchId(),
                    filter.position(),
                    filter.active(),
                    filter.search()
            );
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void validateCompanyAndBranch(UUID companyId, UUID branchId) {
        companyService.getActiveCompanyOrThrow(companyId);
        BranchResponse branch = branchService.getBranch(branchId);
        if (!branch.companyId().equals(companyId)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_BRANCH_COMPANY_MISMATCH);
        }
    }

    private Employee getEmployeeOrThrow(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private Employee getActiveEmployeeOrThrow(UUID id) {
        Employee employee = getEmployeeOrThrow(id);
        if (!employee.isActive()) {
            throw new BusinessException(ErrorCode.EMPLOYEE_INACTIVE);
        }
        return employee;
    }

    private void ensureEmployeeCodeUnique(String employeeCode) {
        if (employeeRepository.existsByEmployeeCode(normalizeEmployeeCode(employeeCode))) {
            throw new BusinessException(ErrorCode.EMPLOYEE_CODE_ALREADY_EXISTS);
        }
    }

    private void ensureEmployeeCodeUniqueForUpdate(String employeeCode, UUID id) {
        if (employeeRepository.existsByEmployeeCodeAndIdNot(normalizeEmployeeCode(employeeCode), id)) {
            throw new BusinessException(ErrorCode.EMPLOYEE_CODE_ALREADY_EXISTS);
        }
    }

    private String normalizeEmployeeCode(String employeeCode) {
        return employeeCode.trim().toUpperCase();
    }

    private Optional<Integer> employeeCodeNumber(String employeeCode) {
        if (!StringUtils.hasText(employeeCode)) {
            return Optional.empty();
        }
        Matcher matcher = EMPLOYEE_CODE_PATTERN.matcher(normalizeEmployeeCode(employeeCode));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private String formatEmployeeCode(int number) {
        return EMPLOYEE_CODE_PREFIX + String.format("%04d", number);
    }

    private String normalizePhone(String phone) {
        return StringUtils.hasText(phone) ? phone : null;
    }

    private void applyCreateFields(Employee employee, CreateEmployeeRequest request) {
        employee.setMiddleName(request.middleName());
        employee.setPhone(normalizePhone(request.phone()));
        employee.setPhotoUrl(request.photoUrl());
        employee.setPosition(request.position());
        employee.setHiredDate(request.hiredDate());
        employee.setBirthDate(request.birthDate());
        employee.setEmploymentType(request.employmentType() == null ? Employee.EmploymentType.FULL_TIME : request.employmentType());
        employee.setSalary(request.salary());
    }

    private void applyUpdateFields(Employee employee, UpdateEmployeeRequest request) {
        if (request.userId() != null) {
            employee.setUserId(request.userId());
        }
        employee.setMiddleName(request.middleName());
        employee.setPhone(normalizePhone(request.phone()));
        employee.setPhotoUrl(request.photoUrl());
        employee.setPosition(request.position());
        employee.setHiredDate(request.hiredDate());
        employee.setBirthDate(request.birthDate());
        employee.setEmploymentType(request.employmentType() == null ? Employee.EmploymentType.FULL_TIME : request.employmentType());
        employee.setSalary(request.salary());
    }

    private EmployeeResponse toResponse(Employee employee) {
        String email = employee.getUserId() == null
                ? null
                : userRepository.findById(employee.getUserId()).map(User::getEmail).orElse(null);
        return EmployeeResponse.from(employee, email);
    }

    private void syncLinkedUserScope(Employee employee) {
        if (employee.getUserId() == null) {
            return;
        }
        userRepository.findById(employee.getUserId()).ifPresent(user -> {
            user.setCompanyId(employee.getCompanyId());
            user.setBranchId(employee.getBranchId());
            userRepository.save(user);
        });
    }

    private void requireEmployeeListAccess(EmployeeFilterRequest filter) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, filter.companyId());
            return;
        }
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            accessControlService.requireBranchAccess(principal, filter.companyId(), filter.branchId());
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireEmployeeReadAccess(Employee employee) {
        accessControlService.requireEmployeeSelfOrAdmin(
                accessControlService.currentUser(),
                employee.getUserId(),
                employee.getCompanyId(),
                employee.getBranchId()
        );
    }

    private void requireEmployeeUpdateAccess(UUID companyId, UUID branchId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN || principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, companyId);
            return;
        }
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            accessControlService.requireBranchAccess(principal, companyId, branchId);
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireEmployeeAdminWriteAccess(UUID companyId, UUID branchId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN || principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, companyId);
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireEmployeeCodeGenerationAccess() {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN || principal.role() == User.Role.COMPANY_ADMIN) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
