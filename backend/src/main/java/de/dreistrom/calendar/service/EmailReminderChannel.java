package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email reminder channel using Spring Mail.
 * Only activates when a mail server is configured.
 */
@Component
@ConditionalOnBean(JavaMailSender.class)
@RequiredArgsConstructor
@Slf4j
public class EmailReminderChannel implements ReminderChannel {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(AppUser user, ComplianceEvent event, int daysBefore) {
        String subject = "DreiStrom: " + event.getTitle() + " – Frist "
                + (daysBefore == 0 ? "heute" : "in " + daysBefore + " Tagen");
        String body = buildBody(event, daysBefore);

        Notification notification = new Notification(
                user, event, NotificationChannel.EMAIL, subject, body, daysBefore);

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject(subject);
            mail.setText(body);
            mail.setFrom("noreply@dreistrom.de");
            mailSender.send(mail);
            notification.markDelivered();
        } catch (Exception e) {
            log.warn("Failed to send email reminder to {}: {}", user.getEmail(), e.getMessage());
        }

        notificationRepository.save(notification);
    }

    private String buildBody(ComplianceEvent event, int daysBefore) {
        return """
                Sehr geehrte(r) Nutzer(in),

                die folgende steuerliche Frist steht bevor:

                %s
                Fällig am: %s
                %s

                Bitte stellen Sie die rechtzeitige Erledigung sicher.

                Mit freundlichen Grüßen
                Ihr DreiStrom System
                """.formatted(
                event.getTitle(),
                event.getDueDate(),
                daysBefore == 0
                        ? "Die Frist endet heute!"
                        : "Verbleibende Tage: " + daysBefore
        );
    }
}
