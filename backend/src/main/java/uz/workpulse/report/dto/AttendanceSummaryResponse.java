package uz.workpulse.report.dto;

public record AttendanceSummaryResponse(
        long totalEmployees,
        long presentCount,
        long lateCount,
        long absentCount,
        long leaveCount,
        long totalWorkMinutes,
        long averageWorkMinutes,
        long totalLateMinutes,
        long sessionsCount
) {
}
