package uz.workpulse.face.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.workpulse.face.application.EmployeeFaceProfileService;
import uz.workpulse.face.dto.EmployeeFaceProfileResponse;
import uz.workpulse.face.dto.EnrollFaceResponse;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}")
public class EmployeeFaceProfileController {
    private final EmployeeFaceProfileService service;

    public EmployeeFaceProfileController(EmployeeFaceProfileService service) {
        this.service = service;
    }

    @PostMapping(value = "/face/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EnrollFaceResponse>> enroll(
            @PathVariable UUID employeeId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.enroll(employeeId, file)));
    }

    @GetMapping("/face-profiles")
    public ResponseEntity<ApiResponse<List<EmployeeFaceProfileResponse>>> list(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(service.list(employeeId)));
    }

    @PatchMapping("/face-profiles/{profileId}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeFaceProfileResponse>> deactivate(
            @PathVariable UUID employeeId,
            @PathVariable UUID profileId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.deactivate(employeeId, profileId)));
    }
}
