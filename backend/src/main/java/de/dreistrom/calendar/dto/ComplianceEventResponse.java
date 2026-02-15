package de.dreistrom.calendar.dto;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.ComplianceEventStatus;
import de.dreistrom.calendar.domain.ComplianceEventType;

import java.time.Instant;
import java.time.LocalDate;

public record ComplianceEventResponse(
        Long id,
        ComplianceEventType eventType,
        String title,
        String description,
        LocalDate dueDate,
        ComplianceEventStatus status,
        Long taxPeriodId,
        Instant completedAt
) {
    public static ComplianceEventResponse from(ComplianceEvent event) {
        return new ComplianceEventResponse(
                event.getId(),
                event.getEventType(),
                event.getTitle(),
                event.getDescription(),
                event.getDueDate(),
                event.getStatus(),
                event.getTaxPeriodId(),
                event.getCompletedAt()
        );
    }
}
