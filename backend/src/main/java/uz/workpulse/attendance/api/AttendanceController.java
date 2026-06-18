package uz.workpulse.attendance.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.attendance.application.AttendanceService;
import uz.workpulse.attendance.domain.AttendanceSession;
import uz.workpulse.attendance.dto.AttendanceFilterRequest;
import uz.workpulse.attendance.dto.AttendanceResponse;
import uz.workpulse.attendance.dto.CheckInRequest;
import uz.workpulse.attendance.dto.CheckOutRequest;
import uz.workpulse.attendance.dto.UpdateAttendanceRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@Valid @RequestBody CheckInRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkIn(request)));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@Valid @RequestBody CheckOutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.checkOut(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) AttendanceSession.Status status,
            @RequestParam(required = false) AttendanceSession.Method method,
            Pageable pageable
    ) {
        AttendanceFilterRequest filter = new AttendanceFilterRequest(employeeId, branchId, date, from, to, status, method);
        return ResponseEntity.ok(ApiResponse.success(attendanceService.list(filter, pageable)));
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> today(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID branchId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getToday(companyId, branchId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getById(id)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> employeeHistory(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getEmployeeHistory(employeeId, from, to)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AttendanceResponse>> adminUpdate(
            @PathVariable UUID id,
            @RequestBody UpdateAttendanceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.adminUpdate(id, request)));
    }
}
