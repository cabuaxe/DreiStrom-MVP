package de.dreistrom.calendar.repository;

import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndDeliveredFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
            Long userId, NotificationChannel channel);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.complianceEvent.id = :eventId "
            + "AND n.channel = :channel AND n.daysBefore = :daysBefore")
    List<Notification> findByComplianceEventIdAndChannelAndDaysBefore(
            Long eventId, NotificationChannel channel, int daysBefore);

    long countByUserIdAndReadAtIsNull(Long userId);
}
