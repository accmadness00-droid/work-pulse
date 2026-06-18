package uz.workpulse.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "Business rule violation"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "User is inactive"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is expired"),
    REVOKED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is revoked"),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "Invalid JWT"),
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "Company not found"),
    COMPANY_INACTIVE(HttpStatus.BAD_REQUEST, "Company is inactive"),
    COMPANY_INN_ALREADY_EXISTS(HttpStatus.CONFLICT, "Company INN already exists"),
    BRANCH_NOT_FOUND(HttpStatus.NOT_FOUND, "Branch not found"),
    BRANCH_INACTIVE(HttpStatus.BAD_REQUEST, "Branch is inactive"),
    BRANCH_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Branch schedule not found"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "Employee not found"),
    EMPLOYEE_INACTIVE(HttpStatus.BAD_REQUEST, "Employee is inactive"),
    EMPLOYEE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Employee code already exists"),
    EMPLOYEE_BRANCH_COMPANY_MISMATCH(HttpStatus.BAD_REQUEST, "Branch does not belong to employee company"),
    INVALID_EMPLOYMENT_TYPE(HttpStatus.BAD_REQUEST, "Invalid employment type"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Device not found"),
    DEVICE_INACTIVE(HttpStatus.BAD_REQUEST, "Device is inactive"),
    DEVICE_SERIAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Device serial number already exists"),
    DEVICE_API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "Device API key is invalid"),
    DEVICE_API_KEY_MISSING(HttpStatus.UNAUTHORIZED, "Device API key is missing"),
    DEVICE_CONNECTION_INVALID(HttpStatus.BAD_REQUEST, "Device connection settings are invalid"),
    EMPLOYEE_CREDENTIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Employee credential not found"),
    EMPLOYEE_CREDENTIAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Employee credential already exists"),
    EMPLOYEE_CREDENTIAL_INACTIVE(HttpStatus.BAD_REQUEST, "Employee credential is inactive"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attendance session not found"),
    ATTENDANCE_OPEN_SESSION_EXISTS(HttpStatus.CONFLICT, "Open attendance session already exists"),
    ATTENDANCE_OPEN_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Open attendance session not found"),
    ATTENDANCE_INVALID_CHECKOUT_TIME(HttpStatus.BAD_REQUEST, "Check-out time cannot be before check-in time"),
    ATTENDANCE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Invalid attendance date range"),
    ATTENDANCE_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attendance schedule not found"),
    ATTENDANCE_GEOFENCE_VIOLATION(HttpStatus.BAD_REQUEST, "Check-in location is outside branch geofence"),
    ATTENDANCE_GEOFENCE_COORDINATES_REQUIRED(HttpStatus.BAD_REQUEST, "GPS coordinates are required for GPS check-in"),
    LEAVE_NOT_FOUND(HttpStatus.NOT_FOUND, "Leave request not found"),
    LEAVE_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Invalid leave date range"),
    LEAVE_ALREADY_REVIEWED(HttpStatus.CONFLICT, "Leave request is already reviewed"),
    DEVICE_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Device event not found"),
    DEVICE_EVENT_DUPLICATE(HttpStatus.CONFLICT, "Device event already exists"),
    DEVICE_EVENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "Device event is already processed"),
    DEVICE_EVENT_UNKNOWN_DIRECTION(HttpStatus.BAD_REQUEST, "Device event direction is unknown"),
    DEVICE_EVENT_EMPLOYEE_IDENTIFIER_MISSING(HttpStatus.BAD_REQUEST, "Device event employee identifier is missing"),
    DEVICE_EVENT_EMPLOYEE_RESOLVE_FAILED(HttpStatus.BAD_REQUEST, "Device event employee could not be resolved"),
    DEVICE_EVENT_PROCESSING_FAILED(HttpStatus.BAD_REQUEST, "Device event processing failed"),
    DEVICE_EVENT_HASH_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Device event hash generation failed"),
    REPORT_INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Invalid report date range"),
    REPORT_INVALID_MONTH(HttpStatus.BAD_REQUEST, "Invalid report month"),
    REPORT_SNAPSHOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Report snapshot not found"),
    REPORT_EXPORT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Report export failed"),
    REPORT_UNSUPPORTED_TYPE(HttpStatus.BAD_REQUEST, "Unsupported report type"),
    FACE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Face service is unavailable"),
    FACE_NOT_DETECTED(HttpStatus.BAD_REQUEST, "Face was not detected in the photo"),
    FACE_MULTIPLE_DETECTED(HttpStatus.BAD_REQUEST, "Multiple faces were detected in the photo"),
    ATTENDANCE_FACE_NOT_RECOGNIZED(HttpStatus.BAD_REQUEST, "Face was not recognized"),
    ATTENDANCE_FACE_CONFIDENCE_LOW(HttpStatus.BAD_REQUEST, "Face confidence is too low"),
    ATTENDANCE_LOCATION_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "You are outside the allowed branch location"),
    ATTENDANCE_LOCATION_ACCURACY_LOW(HttpStatus.BAD_REQUEST, "Location accuracy is too low"),
    ATTENDANCE_PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "Attendance photo is required"),
    EMPLOYEE_FACE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "Employee face profile not found");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
