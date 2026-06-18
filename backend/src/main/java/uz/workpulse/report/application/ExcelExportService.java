package uz.workpulse.report.application;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import uz.workpulse.report.dto.ReportExportRequest;
import uz.workpulse.report.dto.ReportRequest;
import uz.workpulse.report.dto.ReportSessionRow;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class ExcelExportService {

    private final ReportService reportService;

    public ExcelExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public byte[] export(ReportExportRequest request) {
        if (request.type() == null) {
            throw new BusinessException(ErrorCode.REPORT_UNSUPPORTED_TYPE);
        }

        ReportRequest reportRequest = toReportRequest(request);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("WorkPulse Report");
            writeHeader(sheet);
            int rowIndex = 1;
            for (ReportSessionRow item : reportService.findExportRows(reportRequest)) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.employeeId() == null ? "" : item.employeeId().toString());
                row.createCell(1).setCellValue(item.branchId() == null ? "" : item.branchId().toString());
                row.createCell(2).setCellValue(item.date() == null ? "" : item.date().toString());
                row.createCell(3).setCellValue(item.checkInTime() == null ? "" : item.checkInTime().toString());
                row.createCell(4).setCellValue(item.checkOutTime() == null ? "" : item.checkOutTime().toString());
                row.createCell(5).setCellValue(item.status());
                row.createCell(6).setCellValue(item.lateMinutes());
                row.createCell(7).setCellValue(item.workMinutes());
                row.createCell(8).setCellValue(item.method());
            }
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    public String filename() {
        return "workpulse-report-" + LocalDate.now() + ".xlsx";
    }

    private void writeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Employee");
        header.createCell(1).setCellValue("Branch");
        header.createCell(2).setCellValue("Date");
        header.createCell(3).setCellValue("Check In");
        header.createCell(4).setCellValue("Check Out");
        header.createCell(5).setCellValue("Status");
        header.createCell(6).setCellValue("Late Minutes");
        header.createCell(7).setCellValue("Work Minutes");
        header.createCell(8).setCellValue("Method");
    }

    private ReportRequest toReportRequest(ReportExportRequest request) {
        return switch (request.type()) {
            case DAILY -> new ReportRequest(request.companyId(), request.branchId(), null, null, null, request.date(), null, null);
            case MONTHLY -> new ReportRequest(request.companyId(), request.branchId(), null, null, null, null, request.month(), request.year());
            case EMPLOYEE -> new ReportRequest(null, null, request.employeeId(), request.from(), request.to(), null, null, null);
            case BRANCH -> new ReportRequest(null, request.branchId(), null, request.from(), request.to(), null, null, null);
        };
    }
}
