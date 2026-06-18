package uz.workpulse.branch.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.branch.application.BranchService;
import uz.workpulse.branch.dto.BranchResponse;
import uz.workpulse.branch.dto.BranchScheduleResponse;
import uz.workpulse.branch.dto.CreateBranchRequest;
import uz.workpulse.branch.dto.UpdateBranchRequest;
import uz.workpulse.branch.dto.UpdateBranchScheduleRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping("/companies/{companyId}/branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> listBranches(@PathVariable UUID companyId) {
        return ResponseEntity.ok(ApiResponse.success(branchService.listBranches(companyId)));
    }

    @PostMapping("/companies/{companyId}/branches")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateBranchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(branchService.createBranch(companyId, request)));
    }

    @GetMapping("/branches/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getBranch(id)));
    }

    @PutMapping("/branches/{id}")
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBranchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateBranch(id, request)));
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID id) {
        branchService.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/branches/{id}/schedule")
    public ResponseEntity<ApiResponse<List<BranchScheduleResponse>>> getSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.getSchedule(id)));
    }

    @PutMapping("/branches/{id}/schedule")
    public ResponseEntity<ApiResponse<List<BranchScheduleResponse>>> updateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBranchScheduleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(branchService.updateSchedule(id, request)));
    }
}
