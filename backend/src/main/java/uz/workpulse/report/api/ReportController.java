package uz.workpulse.report.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.workpulse.report.application.ExcelExportService;
import uz.workpulse.report.application.PdfExportService;
import uz.workpulse.report.application.ReportService;
import uz.workpulse.report.domain.ReportSnapshot;
import uz.workpulse.report.dto.AttendanceSummaryResponse;
import uz.workpulse.report.dto.BranchReportResponse;
import uz.workpulse.report.dto.DailyReportResponse;
import uz.workpulse.report.dto.EmployeeReportResponse;
import uz.workpulse.report.dto.MonthlyReportResponse;
import uz.workpulse.report.dto.ReportExportRequest;
import uz.workpulse.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;
    private final ExcelExportService excelExportService;
    private final PdfExportService pdfExportService;

    public ReportController(
            ReportService reportService,
            ExcelExportService excelExportService,
            PdfExportService pdfExportService
    ) {
        this.reportService = reportService;
        this.excelExportService = excelExportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> summary(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(companyId, branchId, from, to)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<DailyReportResponse>> daily(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDailyReport(companyId, branchId, date)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<MonthlyReportResponse>> monthly(
            @RequestParam UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getMonthlyReport(companyId, branchId, year, month)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeReportResponse>> employee(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getEmployeeReport(employeeId, from, to)));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<BranchReportResponse>> branch(
            @PathVariable UUID branchId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getBranchReport(branchId, from, to)));
    }

    @PostMapping("/snapshots/daily")
    public ResponseEntity<ApiResponse<List<ReportSnapshot>>> regenerateDailySnapshot(@RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generateDailySnapshot(date)));
    }

    @PostMapping("/snapshots/monthly")
    public ResponseEntity<ApiResponse<List<ReportSnapshot>>> regenerateMonthlySnapshot(
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generateMonthlySnapshot(year, month)));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam ReportExportRequest.ExportType type,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        byte[] bytes = excelExportService.export(new ReportExportRequest(type, companyId, branchId, employeeId, from, to, date, year, month));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + excelExportService.filename() + "\"")
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(bytes);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam ReportExportRequest.ExportType type,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        byte[] bytes = pdfExportService.export(new ReportExportRequest(type, companyId, branchId, employeeId, from, to, date, year, month));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pdfExportService.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
