package uz.workpulse.branch.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record UpdateBranchScheduleRequest(
        @NotEmpty List<@Valid DaySchedule> schedules
) {

    public record DaySchedule(
            short dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            int lateThresholdMin,
            boolean isWorkday
    ) {
    }
}
