package de.dreistrom.calendar.repository;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.ComplianceEventStatus;
import de.dreistrom.calendar.domain.ComplianceEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, Long> {

    List<ComplianceEvent> findByUserIdAndDueDateBetweenOrderByDueDateAsc(
            Long userId, LocalDate from, LocalDate to);

    List<ComplianceEvent> findByUserIdAndStatusOrderByDueDateAsc(
            Long userId, ComplianceEventStatus status);

    List<ComplianceEvent> findByUserIdAndEventTypeAndDueDateBetween(
            Long userId, ComplianceEventType eventType, LocalDate from, LocalDate to);

    List<ComplianceEvent> findByStatusAndDueDateBefore(
            ComplianceEventStatus status, LocalDate date);

    List<ComplianceEvent> findByStatusAndDueDateEquals(
            ComplianceEventStatus status, LocalDate date);

    List<ComplianceEvent> findByUserIdOrderByDueDateAsc(Long userId);
}
