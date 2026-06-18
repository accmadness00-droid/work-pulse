package uz.workpulse.report.application;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportSnapshotScheduler {

    private final ReportService reportService;

    public ReportSnapshotScheduler(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(cron = "${app.reports.snapshot-cron}")
    public void generateYesterdaySnapshot() {
        reportService.generateDailySnapshotForScheduler(LocalDate.now().minusDays(1));
    }
}
