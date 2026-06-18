package uz.workpulse.report.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import uz.workpulse.attendance.application.event.AttendanceSessionCreatedEvent;

@Component
public class ReportAttendanceEventListener {

    private final ReportService reportService;

    public ReportAttendanceEventListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @EventListener
    public void onAttendanceSessionCreated(AttendanceSessionCreatedEvent event) {
        if (event.branchId() != null) {
            reportService.invalidateDailySnapshotForDate(event.branchId(), event.date());
        }
    }
}
