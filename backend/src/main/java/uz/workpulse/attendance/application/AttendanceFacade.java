package uz.workpulse.attendance.application;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;
import uz.workpulse.attendance.dto.AttendanceResponse;
import uz.workpulse.attendance.dto.CheckInRequest;
import uz.workpulse.attendance.dto.CheckOutRequest;

public interface AttendanceFacade {

    AttendanceResponse checkIn(CheckInRequest request);

    AttendanceResponse checkOut(CheckOutRequest request);

    AttendanceResponse markCameraVerification(
            UUID attendanceId,
            CameraVerificationAction action,
            String photoUrl,
            BigDecimal faceDistance
    );

    ProcessDeviceEventResult processDeviceEvent(
            UUID employeeId,
            Instant eventTime,
            Direction direction,
            UUID deviceId,
            UUID branchId
    );

    enum Direction {
        IN,
        OUT
    }

    enum CameraVerificationAction {
        CHECK_IN,
        CHECK_OUT
    }

    enum ProcessDeviceEventResult {
        CHECKED_IN,
        CHECKED_OUT,
        DUPLICATE_IN,
        ORPHAN_OUT
    }
}
