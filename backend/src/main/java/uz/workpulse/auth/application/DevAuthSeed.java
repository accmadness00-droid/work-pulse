package uz.workpulse.auth.application;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.auth.domain.User;
import uz.workpulse.auth.infrastructure.UserRepository;
import uz.workpulse.branch.domain.Branch;
import uz.workpulse.branch.infrastructure.BranchRepository;
import uz.workpulse.company.domain.Company;
import uz.workpulse.company.domain.CompanySettings;
import uz.workpulse.company.infrastructure.CompanyRepository;
import uz.workpulse.company.infrastructure.CompanySettingsRepository;
import uz.workpulse.employee.domain.Employee;
import uz.workpulse.employee.infrastructure.EmployeeRepository;

@Component
@Profile({"dev", "local"})
public class DevAuthSeed implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@workpulse.uz";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String COMPANY_ADMIN_EMAIL = "company.admin@demo.uz";
    private static final String BRANCH_MANAGER_EMAIL = "branch.manager@demo.uz";
    private static final String EMPLOYEE_EMAIL = "employee@demo.uz";
    private static final String DEMO_PASSWORD = "demo123";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final CompanyRepository companyRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;

    public DevAuthSeed(
            UserRepository userRepository,
            PasswordService passwordService,
            CompanyRepository companyRepository,
            CompanySettingsRepository companySettingsRepository,
            BranchRepository branchRepository,
            EmployeeRepository employeeRepository
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.companyRepository = companyRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            userRepository.save(new User(
                    ADMIN_EMAIL,
                    passwordService.encode(ADMIN_PASSWORD),
                    User.Role.SUPER_ADMIN,
                    true
            ));
        }

        if (companyRepository.count() > 0) {
            return;
        }

        Company company = companyRepository.save(new Company("Demo Company", "305123456"));
        company.setLegalName("Demo Company MChJ");
        company.setEmail("info@demo.uz");
        company.setPlan("FREE");
        companyRepository.save(company);
        companySettingsRepository.save(new CompanySettings(company.getId()));

        Branch branch = branchRepository.save(new Branch(company.getId(), "HQ Branch"));
        branch.setAddress("Tashkent");
        branch.setGeofenceRadiusMeters(100);
        branchRepository.save(branch);

        User companyAdmin = userRepository.save(createScopedUser(
                COMPANY_ADMIN_EMAIL,
                User.Role.COMPANY_ADMIN,
                company.getId(),
                null
        ));
        company.setOwnerId(companyAdmin.getId());
        companyRepository.save(company);

        userRepository.save(createScopedUser(
                BRANCH_MANAGER_EMAIL,
                User.Role.BRANCH_MANAGER,
                company.getId(),
                branch.getId()
        ));

        User employeeUser = userRepository.save(createScopedUser(
                EMPLOYEE_EMAIL,
                User.Role.EMPLOYEE,
                company.getId(),
                branch.getId()
        ));

        Employee employee = new Employee(
                company.getId(),
                branch.getId(),
                "Demo",
                "Employee",
                "EMP-001"
        );
        employee.setUserId(employeeUser.getId());
        employee.setPosition("Engineer");
        employee.setSalary(BigDecimal.valueOf(5_000_000));
        employeeRepository.save(employee);
    }

    private User createScopedUser(String email, User.Role role, java.util.UUID companyId, java.util.UUID branchId) {
        User user = new User(email, passwordService.encode(DEMO_PASSWORD), role, true);
        user.setCompanyId(companyId);
        user.setBranchId(branchId);
        return user;
    }
}
