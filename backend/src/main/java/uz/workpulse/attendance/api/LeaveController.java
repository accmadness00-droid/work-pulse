package uz.workpulse.attendance.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.attendance.application.LeaveService;
import uz.workpulse.attendance.dto.CreateLeaveRequest;
import uz.workpulse.attendance.dto.LeaveResponse;
import uz.workpulse.attendance.dto.ReviewLeaveRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LeaveResponse>> create(@Valid @RequestBody CreateLeaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getById(id)));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<LeaveResponse>> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewLeaveRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.review(id, request)));
    }
}
