package de.dreistrom.audit.repository;

import de.dreistrom.audit.domain.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    List<EventLog> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType, Long aggregateId);

    List<EventLog> findByCreatedAtBetweenOrderByCreatedAtAsc(
            Instant from, Instant to);
}
