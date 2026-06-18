package uz.workpulse.payroll.api;

import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.payroll.application.PayrollService;
import uz.workpulse.payroll.dto.PayrollAdjustmentResponse;
import uz.workpulse.payroll.dto.PayrollResponse;
import uz.workpulse.payroll.dto.UpdatePayrollAdjustmentRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<PayrollResponse>> monthly(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.calculateMonthlyPayroll(companyId, branchId, year, month)));
    }

    @PutMapping("/monthly/{employeeId}/adjustment")
    public ResponseEntity<ApiResponse<PayrollAdjustmentResponse>> updateAdjustment(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month,
            @Valid @RequestBody UpdatePayrollAdjustmentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateAdjustment(employeeId, year, month, request)));
    }
}
