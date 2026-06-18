package uz.workpulse.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.workpulse.report.dto.ReportExportRequest;
import uz.workpulse.report.dto.ReportRequest;
import uz.workpulse.report.dto.ReportSessionRow;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock
    private ReportService reportService;

    @Test
    void excelExportReturnsNonEmptyXlsxBytes() {
        ExcelExportService service = new ExcelExportService(reportService);
        UUID companyId = UUID.randomUUID();
        ReportRequest reportRequest = new ReportRequest(companyId, null, null, null, null, LocalDate.of(2026, 6, 13), null, null);
        when(reportService.findExportRows(reportRequest)).thenReturn(List.of(new ReportSessionRow(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 6, 13),
                Instant.parse("2026-06-13T04:00:00Z"),
                Instant.parse("2026-06-13T12:00:00Z"),
                "PRESENT",
                0,
                480,
                "MANUAL"
        )));

        byte[] bytes = service.export(new ReportExportRequest(
                ReportExportRequest.ExportType.DAILY,
                companyId,
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 6, 13),
                null,
                null
        ));

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }
}
