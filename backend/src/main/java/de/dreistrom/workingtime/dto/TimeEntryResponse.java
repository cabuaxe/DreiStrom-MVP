package de.dreistrom.workingtime.dto;

import de.dreistrom.workingtime.domain.ActivityType;
import de.dreistrom.workingtime.domain.TimeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntryResponse(
        Long id,
        LocalDate entryDate,
        BigDecimal hours,
        ActivityType activityType,
        String description
) {
    public static TimeEntryResponse from(TimeEntry entry) {
        return new TimeEntryResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getHours(),
                entry.getActivityType(),
                entry.getDescription()
        );
    }
}
