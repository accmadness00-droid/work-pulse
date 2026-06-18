package uz.workpulse.employee.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.employee.application.EmployeeScheduleService;
import uz.workpulse.employee.dto.EmployeeScheduleResponse;
import uz.workpulse.employee.dto.UpdateEmployeeScheduleRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/schedule")
public class EmployeeScheduleController {

    private final EmployeeScheduleService employeeScheduleService;

    public EmployeeScheduleController(EmployeeScheduleService employeeScheduleService) {
        this.employeeScheduleService = employeeScheduleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeScheduleResponse>>> getSchedule(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.success(employeeScheduleService.getSchedule(employeeId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<EmployeeScheduleResponse>>> updateSchedule(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeScheduleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(employeeScheduleService.updateSchedule(employeeId, request)));
    }
}
