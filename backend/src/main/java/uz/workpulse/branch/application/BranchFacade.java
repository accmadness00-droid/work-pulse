package uz.workpulse.branch.application;

import java.time.LocalDate;
import java.util.UUID;
import uz.workpulse.branch.dto.BranchScheduleInfo;

public interface BranchFacade {

    BranchScheduleInfo getScheduleForDate(UUID branchId, LocalDate date);

    boolean isWithinGeofence(UUID branchId, double lat, double lng);

    void ensureActiveBranch(UUID branchId);

    UUID getCompanyId(UUID branchId);
}
