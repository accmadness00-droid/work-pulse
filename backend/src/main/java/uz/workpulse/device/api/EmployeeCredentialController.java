package uz.workpulse.device.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.device.application.EmployeeCredentialService;
import uz.workpulse.device.domain.EmployeeCredential;
import uz.workpulse.device.dto.CreateEmployeeCredentialRequest;
import uz.workpulse.device.dto.EmployeeCredentialResponse;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/employee-credentials")
public class EmployeeCredentialController {

    private final EmployeeCredentialService credentialService;

    public EmployeeCredentialController(EmployeeCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeCredentialResponse>>> listCredentials(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) EmployeeCredential.CredentialType credentialType,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.success(credentialService.list(employeeId, credentialType, active)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeCredentialResponse>> createCredential(
            @Valid @RequestBody CreateEmployeeCredentialRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(credentialService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeCredentialResponse>> getCredential(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(credentialService.getById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(@PathVariable UUID id) {
        credentialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<EmployeeCredentialResponse>> activateCredential(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(credentialService.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeCredentialResponse>> deactivateCredential(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(credentialService.deactivate(id)));
    }
}
