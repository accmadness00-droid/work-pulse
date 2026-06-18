package uz.workpulse.report.application;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import uz.workpulse.report.dto.ReportExportRequest;
import uz.workpulse.report.dto.ReportRequest;
import uz.workpulse.report.dto.ReportSessionRow;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Service
public class PdfExportService {

    private final ReportService reportService;

    public PdfExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public byte[] export(ReportExportRequest request) {
        if (request.type() == null) {
            throw new BusinessException(ErrorCode.REPORT_UNSUPPORTED_TYPE);
        }

        ReportRequest reportRequest = toReportRequest(request);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f);
            document.add(new Paragraph("WorkPulse Attendance Report", titleFont));
            document.add(new Paragraph("Generated: " + LocalDate.now()));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            addHeader(table, "Employee");
            addHeader(table, "Branch");
            addHeader(table, "Date");
            addHeader(table, "Check In");
            addHeader(table, "Check Out");
            addHeader(table, "Status");
            addHeader(table, "Late Min");
            addHeader(table, "Work Min");
            addHeader(table, "Method");

            for (ReportSessionRow item : reportService.findExportRows(reportRequest)) {
                table.addCell(cell(item.employeeId() == null ? "" : item.employeeId().toString()));
                table.addCell(cell(item.branchId() == null ? "" : item.branchId().toString()));
                table.addCell(cell(item.date() == null ? "" : item.date().toString()));
                table.addCell(cell(item.checkInTime() == null ? "" : item.checkInTime().toString()));
                table.addCell(cell(item.checkOutTime() == null ? "" : item.checkOutTime().toString()));
                table.addCell(cell(item.status()));
                table.addCell(cell(String.valueOf(item.lateMinutes())));
                table.addCell(cell(String.valueOf(item.workMinutes())));
                table.addCell(cell(item.method()));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.REPORT_EXPORT_FAILED);
        }
    }

    public String filename() {
        return "workpulse-report-" + LocalDate.now() + ".pdf";
    }

    private ReportRequest toReportRequest(ReportExportRequest request) {
        return new ReportRequest(
                request.companyId(),
                request.branchId(),
                request.employeeId(),
                request.from(),
                request.to(),
                request.date(),
                request.year(),
                request.month()
        );
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private PdfPCell cell(String text) {
        return new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9f)));
    }
}
