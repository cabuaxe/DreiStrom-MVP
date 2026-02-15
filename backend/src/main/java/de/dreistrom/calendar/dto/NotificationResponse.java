package de.dreistrom.calendar.dto;

import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationChannel channel,
        String title,
        String message,
        int daysBefore,
        boolean delivered,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getChannel(),
                n.getTitle(),
                n.getMessage(),
                n.getDaysBefore(),
                n.isDelivered(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
