package uz.workpulse.branch.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uz.workpulse.auth.domain.User;
import uz.workpulse.branch.domain.Branch;
import uz.workpulse.branch.domain.BranchSchedule;
import uz.workpulse.branch.dto.BranchResponse;
import uz.workpulse.branch.dto.BranchScheduleInfo;
import uz.workpulse.branch.dto.BranchScheduleResponse;
import uz.workpulse.branch.dto.CreateBranchRequest;
import uz.workpulse.branch.dto.UpdateBranchRequest;
import uz.workpulse.branch.dto.UpdateBranchScheduleRequest;
import uz.workpulse.branch.infrastructure.BranchRepository;
import uz.workpulse.branch.infrastructure.BranchScheduleRepository;
import uz.workpulse.company.application.CompanyService;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
public class BranchService implements BranchFacade {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final int DEFAULT_LATE_THRESHOLD_MIN = 15;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final BranchRepository branchRepository;
    private final BranchScheduleRepository branchScheduleRepository;
    private final CompanyService companyService;
    private final AccessControlService accessControlService;

    public BranchService(
            BranchRepository branchRepository,
            BranchScheduleRepository branchScheduleRepository,
            CompanyService companyService,
            AccessControlService accessControlService
    ) {
        this.branchRepository = branchRepository;
        this.branchScheduleRepository = branchScheduleRepository;
        this.companyService = companyService;
        this.accessControlService = accessControlService;
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(UUID companyId) {
        requireBranchReadAccess(companyId);
        companyService.getActiveCompanyOrThrow(companyId);
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            Branch branch = getActiveBranchOrThrow(principal.branchId());
            if (!branch.getCompanyId().equals(companyId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return List.of(BranchResponse.from(branch));
        }
        if (principal.role() == User.Role.EMPLOYEE) {
            Branch branch = getActiveBranchOrThrow(principal.branchId());
            if (!branch.getCompanyId().equals(companyId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return List.of(BranchResponse.from(branch));
        }
        return branchRepository.findAllByCompanyIdAndActiveTrue(companyId).stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Transactional
    public BranchResponse createBranch(UUID companyId, CreateBranchRequest request) {
        requireBranchWriteAccess(companyId);
        companyService.getActiveCompanyOrThrow(companyId);

        Branch branch = new Branch(companyId, request.name().trim());
        applyBranchFields(
                branch,
                request.address(),
                request.phone(),
                request.latitude(),
                request.longitude(),
                request.geofenceRadiusMeters()
        );
        Branch savedBranch = branchRepository.save(branch);
        createDefaultSchedule(savedBranch.getId());
        return BranchResponse.from(savedBranch);
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(UUID id) {
        Branch branch = getActiveBranchOrThrow(id);
        requireBranchReadAccess(branch.getCompanyId(), id);
        return BranchResponse.from(branch);
    }

    @Transactional
    public BranchResponse updateBranch(UUID id, UpdateBranchRequest request) {
        Branch branch = getActiveBranchOrThrow(id);
        requireBranchWriteAccess(branch.getCompanyId(), id);
        applyBranchFields(
                branch,
                request.address(),
                request.phone(),
                request.latitude(),
                request.longitude(),
                request.geofenceRadiusMeters()
        );
        branch.setName(request.name().trim());
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(UUID id) {
        Branch branch = getActiveBranchOrThrow(id);
        requireBranchWriteAccess(branch.getCompanyId(), id);
        branch.deactivate();
        branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<BranchScheduleResponse> getSchedule(UUID branchId) {
        Branch branch = getActiveBranchOrThrow(branchId);
        requireBranchReadAccess(branch.getCompanyId(), branchId);
        return branchScheduleRepository.findAllByBranchIdOrderByDayOfWeek(branchId).stream()
                .map(BranchScheduleResponse::from)
                .toList();
    }

    @Transactional
    public List<BranchScheduleResponse> updateSchedule(UUID branchId, UpdateBranchScheduleRequest request) {
        Branch branch = getActiveBranchOrThrow(branchId);
        requireBranchWriteAccess(branch.getCompanyId(), branchId);

        for (UpdateBranchScheduleRequest.DaySchedule item : request.schedules()) {
            validateDayOfWeek(item.dayOfWeek());
            BranchSchedule schedule = branchScheduleRepository.findByBranchIdAndDayOfWeek(branchId, item.dayOfWeek())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_SCHEDULE_NOT_FOUND));
            schedule.setStartTime(item.startTime());
            schedule.setEndTime(item.endTime());
            schedule.setLateThresholdMin(item.lateThresholdMin());
            schedule.setWorkday(item.isWorkday());
            branchScheduleRepository.save(schedule);
        }

        return getSchedule(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchScheduleInfo getScheduleForDate(UUID branchId, LocalDate date) {
        getActiveBranchOrThrow(branchId);
        short dayOfWeek = (short) date.getDayOfWeek().getValue();
        BranchSchedule schedule = branchScheduleRepository.findByBranchIdAndDayOfWeek(branchId, dayOfWeek)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_SCHEDULE_NOT_FOUND));
        return new BranchScheduleInfo(
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getLateThresholdMin(),
                schedule.isWorkday()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWithinGeofence(UUID branchId, double lat, double lng) {
        Branch branch = getActiveBranchOrThrow(branchId);
        if (branch.getLatitude() == null || branch.getLongitude() == null || branch.getGeofenceRadiusMeters() == null) {
            return false;
        }

        double distance = haversineMeters(
                branch.getLatitude().doubleValue(),
                branch.getLongitude().doubleValue(),
                lat,
                lng
        );
        return distance <= branch.getGeofenceRadiusMeters();
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureActiveBranch(UUID branchId) {
        getActiveBranchOrThrow(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getCompanyId(UUID branchId) {
        return getActiveBranchOrThrow(branchId).getCompanyId();
    }

    private Branch getActiveBranchOrThrow(UUID id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_NOT_FOUND));
        if (!branch.isActive()) {
            throw new BusinessException(ErrorCode.BRANCH_INACTIVE);
        }
        return branch;
    }

    private void createDefaultSchedule(UUID branchId) {
        for (short day = 1; day <= 7; day++) {
            boolean workday = day <= 5;
            branchScheduleRepository.save(new BranchSchedule(
                    branchId,
                    day,
                    DEFAULT_START_TIME,
                    DEFAULT_END_TIME,
                    DEFAULT_LATE_THRESHOLD_MIN,
                    workday
            ));
        }
    }

    private void applyBranchFields(
            Branch branch,
            String address,
            String phone,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            Integer geofenceRadiusMeters
    ) {
        branch.setAddress(address);
        branch.setPhone(StringUtils.hasText(phone) ? phone : null);
        branch.setLatitude(latitude);
        branch.setLongitude(longitude);
        branch.setGeofenceRadiusMeters(geofenceRadiusMeters);
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private void validateDayOfWeek(short dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "dayOfWeek must be between 1 and 7");
        }
    }

    private void requireBranchReadAccess(UUID companyId) {
        requireBranchReadAccess(companyId, null);
    }

    private void requireBranchReadAccess(UUID companyId, UUID branchId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, companyId);
            return;
        }
        if (principal.role() == User.Role.BRANCH_MANAGER) {
            accessControlService.requireBranchAccess(principal, companyId, branchId == null ? principal.branchId() : branchId);
            return;
        }
        if (principal.role() == User.Role.EMPLOYEE
                && companyId != null
                && companyId.equals(principal.companyId())
                && (branchId == null || branchId.equals(principal.branchId()))) {
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private void requireBranchWriteAccess(UUID companyId) {
        requireBranchWriteAccess(companyId, null);
    }

    private void requireBranchWriteAccess(UUID companyId, UUID branchId) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() == User.Role.SUPER_ADMIN || principal.role() == User.Role.COMPANY_ADMIN) {
            accessControlService.requireCompanyAccess(principal, companyId);
            return;
        }
        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
}
