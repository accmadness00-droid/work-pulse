package uz.workpulse.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.application.PasswordService;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.branch.application.BranchService;
import uz.workpulse.branch.dto.BranchResponse;
import uz.workpulse.company.application.CompanyService;
import uz.workpulse.company.domain.Company;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.dto.CreateEmployeeRequest;
import uz.workpulse.employee.infrastructure.EmployeeRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeQueryService employeeQueryService;

    @Mock
    private CompanyService companyService;

    @Mock
    private BranchService branchService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @Mock
    private AccessControlService accessControlService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository,
                employeeQueryService,
                companyService,
                branchService,
                userRepository,
                passwordService,
                accessControlService
        );
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEmployeeHappyPath() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(companyService.getActiveCompanyOrThrow(companyId)).thenReturn(new Company("Work Pulse", "123"));
        when(branchService.getBranch(branchId)).thenReturn(branchResponse(branchId, companyId));
        when(employeeRepository.existsByEmployeeCode("EMP-001")).thenReturn(false);
        when(userRepository.existsByEmail("ali@demo.uz")).thenReturn(false);
        when(passwordService.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
            return employee;
        });

        var response = employeeService.create(createRequest(companyId, branchId, "emp-001"));

        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.branchId()).isEqualTo(branchId);
        assertThat(response.employeeCode()).isEqualTo("EMP-001");
        assertThat(response.active()).isTrue();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createEmployeeDuplicateEmployeeCodeFails() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(companyService.getActiveCompanyOrThrow(companyId)).thenReturn(new Company("Work Pulse", "123"));
        when(branchService.getBranch(branchId)).thenReturn(branchResponse(branchId, companyId));
        when(employeeRepository.existsByEmployeeCode("EMP-001")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(createRequest(companyId, branchId, "emp-001")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_CODE_ALREADY_EXISTS);
    }

    @Test
    void createEmployeeBranchCompanyMismatchFails() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(companyService.getActiveCompanyOrThrow(companyId)).thenReturn(new Company("Work Pulse", "123"));
        when(branchService.getBranch(branchId)).thenReturn(branchResponse(branchId, UUID.randomUUID()));

        assertThatThrownBy(() -> employeeService.create(createRequest(companyId, branchId, "EMP-001")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPLOYEE_BRANCH_COMPANY_MISMATCH);
    }

    @Test
    void softDeleteSetsActiveFalse() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(UUID.randomUUID(), UUID.randomUUID(), "EMP-001", true);
        ReflectionTestUtils.setField(employee, "id", employeeId);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        employeeService.softDelete(employeeId);

        assertThat(employee.isActive()).isFalse();
        verify(employeeRepository).save(employee);
    }

    @Test
    void findEmployeeIdByCodeWorks() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(UUID.randomUUID(), UUID.randomUUID(), "EMP-001", true);
        ReflectionTestUtils.setField(employee, "id", employeeId);
        when(employeeRepository.findByEmployeeCodeAndActiveTrue("EMP-001")).thenReturn(Optional.of(employee));

        assertThat(employeeService.findEmployeeIdByCode("emp-001")).contains(employeeId);
    }

    @Test
    void existsAndActiveReturnsTrueAndFalse() {
        UUID activeId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        Employee active = employee(UUID.randomUUID(), UUID.randomUUID(), "EMP-001", true);
        Employee inactive = employee(UUID.randomUUID(), UUID.randomUUID(), "EMP-002", false);
        when(employeeRepository.findById(activeId)).thenReturn(Optional.of(active));
        when(employeeRepository.findById(inactiveId)).thenReturn(Optional.of(inactive));

        assertThat(employeeService.existsAndActive(activeId)).isTrue();
        assertThat(employeeService.existsAndActive(inactiveId)).isFalse();
        assertThat(employeeService.existsAndActive(UUID.randomUUID())).isFalse();
    }

    private CreateEmployeeRequest createRequest(UUID companyId, UUID branchId, String employeeCode) {
        return new CreateEmployeeRequest(
                null,
                "ali@demo.uz",
                "demo123",
                companyId,
                branchId,
                "Ali",
                "Valiyev",
                "Akmalovich",
                "+998901234567",
                null,
                "Engineer",
                employeeCode,
                null,
                null,
                Employee.EmploymentType.FULL_TIME,
                BigDecimal.valueOf(1_000_000)
        );
    }

    private Employee employee(UUID companyId, UUID branchId, String employeeCode, boolean active) {
        Employee employee = new Employee(companyId, branchId, "Ali", "Valiyev", employeeCode);
        if (!active) {
            employee.deactivate();
        }
        return employee;
    }

    private BranchResponse branchResponse(UUID branchId, UUID companyId) {
        return new BranchResponse(branchId, companyId, "Main", "Tashkent", null, null, 100, true);
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
