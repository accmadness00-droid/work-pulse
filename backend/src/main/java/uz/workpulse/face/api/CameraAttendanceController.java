package uz.workpulse.face.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.face.application.CameraAttendanceService;
import uz.workpulse.face.dto.CameraAttendanceResponse;
import uz.workpulse.face.dto.CameraCheckInRequest;
import uz.workpulse.face.dto.CameraCheckOutRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/attendance/camera")
public class CameraAttendanceController {
    private final CameraAttendanceService service;

    public CameraAttendanceController(CameraAttendanceService service) {
        this.service = service;
    }

    @PostMapping("/check-in")
    public ResponseEntity<ApiResponse<CameraAttendanceResponse>> checkIn(
            @Valid @RequestBody CameraCheckInRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.checkIn(
                request,
                servletRequest.getHeader("User-Agent"),
                clientIp(servletRequest)
        )));
    }

    @PostMapping("/check-out")
    public ResponseEntity<ApiResponse<CameraAttendanceResponse>> checkOut(
            @Valid @RequestBody CameraCheckOutRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.checkOut(
                request,
                servletRequest.getHeader("User-Agent"),
                clientIp(servletRequest)
        )));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
