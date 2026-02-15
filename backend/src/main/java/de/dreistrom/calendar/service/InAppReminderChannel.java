package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * In-app notification channel. Persists notification and publishes via SSE.
 */
@Component
@RequiredArgsConstructor
public class InAppReminderChannel implements ReminderChannel {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(AppUser user, ComplianceEvent event, int daysBefore) {
        String title = formatTitle(event, daysBefore);
        String message = formatMessage(event, daysBefore);

        Notification notification = new Notification(
                user, event, NotificationChannel.IN_APP, title, message, daysBefore);
        notification.markDelivered();
        notificationRepository.save(notification);

        sseEmitterService.sendToUser(user.getId(), notification);
    }

    private String formatTitle(ComplianceEvent event, int daysBefore) {
        if (daysBefore == 0) {
            return "Frist heute: " + event.getTitle();
        }
        return "Frist in " + daysBefore + (daysBefore == 1 ? " Tag" : " Tagen") + ": " + event.getTitle();
    }

    private String formatMessage(ComplianceEvent event, int daysBefore) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.getTitle()).append(" ist am ").append(event.getDueDate()).append(" fällig.");
        if (daysBefore > 0) {
            sb.append(" Noch ").append(daysBefore).append(daysBefore == 1 ? " Tag." : " Tage.");
        } else {
            sb.append(" Die Frist endet heute!");
        }
        return sb.toString();
    }
}
