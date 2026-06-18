package uz.workpulse.branch.dto;

import java.time.LocalTime;
import java.util.UUID;
import uz.workpulse.branch.domain.BranchSchedule;

public record BranchScheduleResponse(
        UUID id,
        UUID branchId,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int lateThresholdMin,
        boolean isWorkday
) {

    public static BranchScheduleResponse from(BranchSchedule schedule) {
        return new BranchScheduleResponse(
                schedule.getId(),
                schedule.getBranchId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getLateThresholdMin(),
                schedule.isWorkday()
        );
    }
}
