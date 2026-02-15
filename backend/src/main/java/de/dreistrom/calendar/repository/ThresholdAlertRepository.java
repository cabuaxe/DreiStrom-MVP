package de.dreistrom.calendar.repository;

import de.dreistrom.calendar.domain.ThresholdAlert;
import de.dreistrom.calendar.domain.ThresholdAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThresholdAlertRepository extends JpaRepository<ThresholdAlert, Long> {

    List<ThresholdAlert> findByUserIdAndAcknowledgedFalseOrderByTriggeredAtDesc(Long userId);

    List<ThresholdAlert> findByUserIdAndAlertTypeOrderByTriggeredAtDesc(
            Long userId, ThresholdAlertType alertType);

    List<ThresholdAlert> findByUserIdOrderByTriggeredAtDesc(Long userId);

    long countByUserIdAndAcknowledgedFalse(Long userId);
}
