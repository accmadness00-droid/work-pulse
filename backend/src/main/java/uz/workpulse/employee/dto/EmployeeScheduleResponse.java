package uz.workpulse.employee.dto;

import java.time.LocalTime;
import java.util.UUID;
import uz.workpulse.employee.domain.EmployeeSchedule;

public record EmployeeScheduleResponse(
        UUID id,
        UUID employeeId,
        short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int lateThresholdMin,
        boolean isWorkday,
        String note
) {

    public static EmployeeScheduleResponse from(EmployeeSchedule schedule) {
        return new EmployeeScheduleResponse(
                schedule.getId(),
                schedule.getEmployeeId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getLateThresholdMin(),
                schedule.isWorkday(),
                schedule.getNote()
        );
    }
}
