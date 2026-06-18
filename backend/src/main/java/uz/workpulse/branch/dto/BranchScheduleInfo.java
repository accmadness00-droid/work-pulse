package uz.workpulse.branch.dto;

import java.time.LocalTime;

public record BranchScheduleInfo(
        LocalTime startTime,
        LocalTime endTime,
        int lateThresholdMin,
        boolean isWorkday
) {
}
