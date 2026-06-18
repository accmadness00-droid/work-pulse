package uz.workpulse.employee.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uz.workpulse.employee.application.EmployeePhotoService;
import uz.workpulse.employee.application.EmployeeService;
import uz.workpulse.employee.dto.CreateEmployeeRequest;
import uz.workpulse.employee.dto.EmployeeFilterRequest;
import uz.workpulse.employee.dto.EmployeeResponse;
import uz.workpulse.employee.dto.UpdateEmployeeRequest;
import uz.workpulse.device.application.EmployeeHikvisionPhotoService;
import uz.workpulse.device.dto.HikvisionPhotoSyncResponse;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeePhotoService employeePhotoService;
    private final EmployeeHikvisionPhotoService employeeHikvisionPhotoService;

    public EmployeeController(
            EmployeeService employeeService,
            EmployeePhotoService employeePhotoService,
            EmployeeHikvisionPhotoService employeeHikvisionPhotoService
    ) {
        this.employeeService = employeeService;
        this.employeePhotoService = employeePhotoService;
        this.employeeHikvisionPhotoService = employeeHikvisionPhotoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        EmployeeFilterRequest filter = new EmployeeFilterRequest(companyId, branchId, position, active, search);
        return ResponseEntity.ok(ApiResponse.success(employeeService.list(filter, pageable)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getCurrentEmployee() {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getMe()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPANY_ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.create(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable UUID id) {
        employeeService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activateEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivateEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.deactivate(id)));
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> uploadPhoto(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeePhotoService.uploadPhoto(id, file)));
    }

    @PostMapping("/{id}/photo/sync-hikvision")
    public ResponseEntity<ApiResponse<HikvisionPhotoSyncResponse>> syncPhotoToHikvision(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(employeeHikvisionPhotoService.syncEmployeePhoto(id)));
    }
}
