package uz.workpulse.employee.dto;

import java.time.LocalTime;

public record EmployeeScheduleInfo(
        LocalTime startTime,
        LocalTime endTime,
        int lateThresholdMin,
        boolean isWorkday
) {
}
