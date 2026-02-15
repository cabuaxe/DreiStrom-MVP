package de.dreistrom.calendar.repository;

import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndDeliveredFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
            Long userId, NotificationChannel channel);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByComplianceEventIdAndChannelAndDaysBefore(
            Long eventId, NotificationChannel channel, int daysBefore);

    long countByUserIdAndReadAtIsNull(Long userId);
}
