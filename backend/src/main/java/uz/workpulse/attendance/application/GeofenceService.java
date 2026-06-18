package uz.workpulse.attendance.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import uz.workpulse.attendance.domain.AttendanceSession;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class GeofenceService {

    private final BranchFacade branchFacade;

    public GeofenceService(BranchFacade branchFacade) {
        this.branchFacade = branchFacade;
    }

    public void validateCheckIn(UUID branchId, BigDecimal latitude, BigDecimal longitude, AttendanceSession.Method method) {
        if (method != AttendanceSession.Method.GPS) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GEOFENCE_COORDINATES_REQUIRED);
        }
        if (!branchFacade.isWithinGeofence(branchId, latitude.doubleValue(), longitude.doubleValue())) {
            throw new BusinessException(ErrorCode.ATTENDANCE_GEOFENCE_VIOLATION);
        }
    }
}
