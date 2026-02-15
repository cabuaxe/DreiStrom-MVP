package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Push notification channel. Framework placeholder – logs the push payload.
 * To be replaced with FCM/APNs integration when mobile app is ready.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PushReminderChannel implements ReminderChannel {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(AppUser user, ComplianceEvent event, int daysBefore) {
        String title = event.getTitle();
        String message = daysBefore == 0
                ? "Frist heute: " + event.getTitle()
                : "Frist in " + daysBefore + " Tagen: " + event.getTitle();

        log.info("PUSH notification to user {}: {} – {}", user.getId(), title, message);

        Notification notification = new Notification(
                user, event, NotificationChannel.PUSH, title, message, daysBefore);
        notification.markDelivered();
        notificationRepository.save(notification);
    }
}
