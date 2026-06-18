package uz.workpulse.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.domain.Branch;
import uz.workpulse.branch.domain.BranchSchedule;
import uz.workpulse.branch.dto.BranchScheduleInfo;
import uz.workpulse.branch.dto.CreateBranchRequest;
import uz.workpulse.branch.infrastructure.BranchRepository;
import uz.workpulse.branch.infrastructure.BranchScheduleRepository;
import uz.workpulse.company.application.CompanyService;
import uz.workpulse.company.domain.Company;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BranchScheduleRepository branchScheduleRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private AccessControlService accessControlService;

    private BranchService branchService;

    @BeforeEach
    void setUp() {
        branchService = new BranchService(branchRepository, branchScheduleRepository, companyService, accessControlService);
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", User.Role.SUPER_ADMIN);
        when(accessControlService.currentUser()).thenReturn(principal);
        authenticateAs(User.Role.SUPER_ADMIN);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBranchCreatesDefaultSchedule() {
        UUID companyId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(companyService.getActiveCompanyOrThrow(companyId)).thenReturn(new Company("Work Pulse", "123"));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> {
            Branch branch = invocation.getArgument(0);
            ReflectionTestUtils.setField(branch, "id", branchId);
            return branch;
        });
        when(branchScheduleRepository.save(any(BranchSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        branchService.createBranch(companyId, new CreateBranchRequest("Main", "Tashkent", null, null, null));

        ArgumentCaptor<BranchSchedule> scheduleCaptor = ArgumentCaptor.forClass(BranchSchedule.class);
        verify(branchScheduleRepository, org.mockito.Mockito.times(7)).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getAllValues()).hasSize(7);
        assertThat(scheduleCaptor.getAllValues().get(0).isWorkday()).isTrue();
        assertThat(scheduleCaptor.getAllValues().get(4).isWorkday()).isTrue();
        assertThat(scheduleCaptor.getAllValues().get(5).isWorkday()).isFalse();
        assertThat(scheduleCaptor.getAllValues().get(6).isWorkday()).isFalse();
        assertThat(scheduleCaptor.getAllValues())
                .allSatisfy(schedule -> {
                    assertThat(schedule.getBranchId()).isEqualTo(branchId);
                    assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
                    assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(18, 0));
                    assertThat(schedule.getLateThresholdMin()).isEqualTo(15);
                });
    }

    @Test
    void getScheduleForDateReturnsCorrectDay() {
        UUID branchId = UUID.randomUUID();
        Branch branch = branch(branchId, BigDecimal.valueOf(41.2995), BigDecimal.valueOf(69.2401), 100);
        BranchSchedule friday = new BranchSchedule(branchId, (short) 5, LocalTime.of(9, 0), LocalTime.of(18, 0), 15, true);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeek(branchId, (short) 5)).thenReturn(Optional.of(friday));

        BranchScheduleInfo info = branchService.getScheduleForDate(branchId, LocalDate.of(2026, 6, 12));

        assertThat(info.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(info.endTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(info.lateThresholdMin()).isEqualTo(15);
        assertThat(info.isWorkday()).isTrue();
    }

    @Test
    void isWithinGeofenceReturnsTrueAndFalse() {
        UUID branchId = UUID.randomUUID();
        Branch branch = branch(branchId, BigDecimal.valueOf(41.2995), BigDecimal.valueOf(69.2401), 100);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        assertThat(branchService.isWithinGeofence(branchId, 41.2996, 69.2402)).isTrue();
        assertThat(branchService.isWithinGeofence(branchId, 41.31, 69.25)).isFalse();
    }

    private Branch branch(UUID id, BigDecimal latitude, BigDecimal longitude, Integer radiusMeters) {
        Branch branch = new Branch(UUID.randomUUID(), "Main");
        ReflectionTestUtils.setField(branch, "id", id);
        branch.setLatitude(latitude);
        branch.setLongitude(longitude);
        branch.setGeofenceRadiusMeters(radiusMeters);
        return branch;
    }

    private void authenticateAs(User.Role role) {
        AuthPrincipal principal = new AuthPrincipal(UUID.randomUUID(), "admin@workpulse.uz", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }
}
