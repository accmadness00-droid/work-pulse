package uz.workpulse.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

public record UpdateEmployeeScheduleRequest(
        @NotEmpty List<@Valid DaySchedule> schedules
) {

    public record DaySchedule(
            @Min(1) @Max(7) short dayOfWeek,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(0) int lateThresholdMin,
            boolean isWorkday,
            String note
    ) {
    }
}
