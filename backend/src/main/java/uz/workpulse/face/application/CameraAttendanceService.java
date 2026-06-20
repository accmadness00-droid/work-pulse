package uz.workpulse.face.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.workpulse.attendance.application.AttendanceFacade;
import uz.workpulse.attendance.domain.AttendanceSession;
import uz.workpulse.attendance.dto.AttendanceResponse;
import uz.workpulse.attendance.dto.CheckInRequest;
import uz.workpulse.attendance.dto.CheckOutRequest;
import uz.workpulse.branch.application.BranchFacade;
import uz.workpulse.auth.domain.User;
import uz.workpulse.employee.application.EmployeeFacade;
import uz.workpulse.face.domain.CameraAttendanceAction;
import uz.workpulse.face.domain.CameraAttendanceLog;
import uz.workpulse.face.domain.FaceMatchResult;
import uz.workpulse.face.dto.CameraAttendanceResponse;
import uz.workpulse.face.dto.CameraCheckInRequest;
import uz.workpulse.face.dto.CameraCheckOutRequest;
import uz.workpulse.face.infrastructure.CameraAttendanceLogRepository;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;
import uz.workpulse.shared.file.FileStorageService;
import uz.workpulse.shared.file.StoredFile;
import uz.workpulse.shared.security.AccessControlService;
import uz.workpulse.shared.security.AuthPrincipal;

@Service
@EnableConfigurationProperties(FaceProperties.class)
public class CameraAttendanceService {
    private final FaceRecognitionService faceRecognitionService;
    private final FileStorageService fileStorageService;
    private final AttendanceFacade attendanceFacade;
    private final BranchFacade branchFacade;
    private final EmployeeFacade employeeFacade;
    private final AccessControlService accessControlService;
    private final CameraAttendanceLogRepository logRepository;
    private final FaceProperties properties;

    public CameraAttendanceService(
            FaceRecognitionService faceRecognitionService,
            FileStorageService fileStorageService,
            AttendanceFacade attendanceFacade,
            BranchFacade branchFacade,
            EmployeeFacade employeeFacade,
            AccessControlService accessControlService,
            CameraAttendanceLogRepository logRepository,
            FaceProperties properties
    ) {
        this.faceRecognitionService = faceRecognitionService;
        this.fileStorageService = fileStorageService;
        this.attendanceFacade = attendanceFacade;
        this.branchFacade = branchFacade;
        this.employeeFacade = employeeFacade;
        this.accessControlService = accessControlService;
        this.logRepository = logRepository;
        this.properties = properties;
    }

    @Transactional
    public CameraAttendanceResponse checkIn(CameraCheckInRequest request, String userAgent, String ipAddress) {
        validateLocationAccuracy(request.getAccuracyMeters());
        byte[] imageBytes = decodePhoto(request.getPhotoBase64());
        FaceMatchResult match = recognizeForCurrentUser(imageBytes);
        validateGeofence(request.getBranchId(), request.getLatitude(), request.getLongitude());

        StoredFile storedPhoto = fileStorageService.saveAttendancePhoto(match.getEmployeeId(), request.getPhotoBase64(), "CHECK_IN");

        CheckInRequest checkIn = new CheckInRequest(
                match.getEmployeeId(),
                request.getBranchId(),
                request.getLatitude(),
                request.getLongitude(),
                AttendanceSession.Method.WEB_CAMERA,
                "WEB_CAMERA faceDistance=" + match.getDistance()
        );

        AttendanceResponse attendance = attendanceFacade.checkIn(checkIn);
        attendance = updateAttendanceCameraFields(attendance.id(), CameraAttendanceAction.CHECK_IN, storedPhoto.getUrl(), match.getDistance());
        saveLog(attendance.id(), match.getEmployeeId(), request.getBranchId(), CameraAttendanceAction.CHECK_IN, request.getLatitude(), request.getLongitude(), request.getAccuracyMeters(), true, true, match.getDistance(), storedPhoto.getUrl(), userAgent, ipAddress, null);
        return toCameraResponse(attendance, CameraAttendanceAction.CHECK_IN, match, storedPhoto.getUrl());
    }

    @Transactional
    public CameraAttendanceResponse checkOut(CameraCheckOutRequest request, String userAgent, String ipAddress) {
        validateLocationAccuracy(request.getAccuracyMeters());
        byte[] imageBytes = decodePhoto(request.getPhotoBase64());
        FaceMatchResult match = recognizeForCurrentUser(imageBytes);
        validateGeofence(request.getBranchId(), request.getLatitude(), request.getLongitude());

        StoredFile storedPhoto = fileStorageService.saveAttendancePhoto(match.getEmployeeId(), request.getPhotoBase64(), "CHECK_OUT");

        CheckOutRequest checkOut = new CheckOutRequest(
                match.getEmployeeId(),
                request.getLatitude(),
                request.getLongitude(),
                AttendanceSession.Method.WEB_CAMERA,
                "WEB_CAMERA faceDistance=" + match.getDistance()
        );

        AttendanceResponse attendance = attendanceFacade.checkOut(checkOut);
        attendance = updateAttendanceCameraFields(attendance.id(), CameraAttendanceAction.CHECK_OUT, storedPhoto.getUrl(), match.getDistance());
        saveLog(attendance.id(), match.getEmployeeId(), request.getBranchId(), CameraAttendanceAction.CHECK_OUT, request.getLatitude(), request.getLongitude(), request.getAccuracyMeters(), true, true, match.getDistance(), storedPhoto.getUrl(), userAgent, ipAddress, null);
        return toCameraResponse(attendance, CameraAttendanceAction.CHECK_OUT, match, storedPhoto.getUrl());
    }

    private void validateLocationAccuracy(BigDecimal accuracyMeters) {
        if (accuracyMeters != null && accuracyMeters.doubleValue() > properties.getMaxLocationAccuracyMeters()) {
            throw new BusinessException(ErrorCode.ATTENDANCE_LOCATION_ACCURACY_LOW);
        }
    }

    private void validateGeofence(UUID branchId, BigDecimal latitude, BigDecimal longitude) {
        boolean inside = branchFacade.isWithinGeofence(branchId, latitude.doubleValue(), longitude.doubleValue());
        if (!inside) {
            throw new BusinessException(ErrorCode.ATTENDANCE_LOCATION_OUT_OF_RANGE);
        }
    }

    private AttendanceResponse updateAttendanceCameraFields(UUID attendanceId, CameraAttendanceAction action, String photoUrl, double distance) {
        AttendanceFacade.CameraVerificationAction facadeAction = action == CameraAttendanceAction.CHECK_IN
                ? AttendanceFacade.CameraVerificationAction.CHECK_IN
                : AttendanceFacade.CameraVerificationAction.CHECK_OUT;
        return attendanceFacade.markCameraVerification(attendanceId, facadeAction, photoUrl, BigDecimal.valueOf(distance));
    }

    private byte[] decodePhoto(String photoBase64) {
        try {
            return fileStorageService.decodeBase64Image(photoBase64);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.ATTENDANCE_PHOTO_REQUIRED);
        }
    }

    private FaceMatchResult recognizeForCurrentUser(byte[] imageBytes) {
        AuthPrincipal principal = accessControlService.currentUser();
        if (principal.role() != User.Role.EMPLOYEE) {
            return faceRecognitionService.recognize(imageBytes);
        }
        UUID employeeId = employeeFacade.findEmployeeIdByUserId(principal.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EMPLOYEE_NOT_FOUND));
        return faceRecognitionService.recognize(imageBytes, employeeId);
    }

    private void saveLog(UUID attendanceId, UUID employeeId, UUID branchId, CameraAttendanceAction action,
                         BigDecimal lat, BigDecimal lng, BigDecimal accuracy, boolean locationVerified,
                         boolean faceVerified, double distance, String photoUrl, String userAgent,
                         String ipAddress, String error) {
        CameraAttendanceLog log = new CameraAttendanceLog();
        log.setAttendanceSessionId(attendanceId);
        log.setEmployeeId(employeeId);
        log.setBranchId(branchId);
        log.setAction(action);
        log.setLatitude(lat);
        log.setLongitude(lng);
        log.setAccuracyMeters(accuracy);
        log.setLocationVerified(locationVerified);
        log.setFaceVerified(faceVerified);
        log.setFaceDistance(BigDecimal.valueOf(distance));
        log.setPhotoUrl(photoUrl);
        log.setUserAgent(userAgent);
        log.setIpAddress(ipAddress);
        log.setErrorMessage(error);
        logRepository.save(log);
    }

    private CameraAttendanceResponse toCameraResponse(AttendanceResponse attendance, CameraAttendanceAction action, FaceMatchResult match, String photoUrl) {
        CameraAttendanceResponse r = new CameraAttendanceResponse();
        r.setAttendanceId(attendance.id());
        r.setEmployeeId(match.getEmployeeId());
        r.setAction(action.name());
        r.setMethod("WEB_CAMERA");
        r.setFaceVerified(true);
        r.setFaceDistance(match.getDistance());
        r.setLocationVerified(true);
        r.setPhotoUrl(photoUrl);
        r.setBranchId(attendance.branchId());
        r.setStatus(attendance.status() == null ? null : attendance.status().toString());
        ZoneId zoneId = ZoneId.systemDefault();
        r.setCheckInTime(attendance.checkInTime() == null ? null : LocalDateTime.ofInstant(attendance.checkInTime(), zoneId));
        r.setCheckOutTime(attendance.checkOutTime() == null ? null : LocalDateTime.ofInstant(attendance.checkOutTime(), zoneId));
        r.setWorkMinutes(attendance.workMinutes());
        return r;
    }
}
