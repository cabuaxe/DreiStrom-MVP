package de.dreistrom.audit.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.audit.repository.EventLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuditLogServiceTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Test
    void persist_savesEventToEventLog() {
        DomainEvent event = new TestEvent("IncomeEntry", 42L, "INCOME_CREATED",
                "{\"amount\": 10000, \"stream\": \"FREIBERUF\"}");

        EventLog saved = auditLogService.persist(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("IncomeEntry");
        assertThat(saved.getAggregateId()).isEqualTo(42L);
        assertThat(saved.getEventType()).isEqualTo("INCOME_CREATED");
        assertThat(saved.getPayload()).contains("FREIBERUF");
        assertThat(saved.getActor()).isEqualTo("system");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persist_usesAuthenticatedUserAsActor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@dreistrom.de", null, List.of()));

        DomainEvent event = new TestEvent("ExpenseEntry", 7L, "EXPENSE_CREATED",
                "{\"amount\": 5000}");

        EventLog saved = auditLogService.persist(event);

        assertThat(saved.getActor()).isEqualTo("owner@dreistrom.de");

        SecurityContextHolder.clearContext();
    }

    @Test
    void persist_multipleEventsAreQueryableByAggregate() {
        auditLogService.persist(new TestEvent("IncomeEntry", 1L, "CREATED", "{}"));
        auditLogService.persist(new TestEvent("IncomeEntry", 1L, "MODIFIED", "{\"before\":1,\"after\":2}"));
        auditLogService.persist(new TestEvent("IncomeEntry", 2L, "CREATED", "{}"));

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("IncomeEntry", 1L);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("CREATED");
        assertThat(events.get(1).getEventType()).isEqualTo("MODIFIED");
    }

    private static class TestEvent extends DomainEvent {
        private final String jsonPayload;

        TestEvent(String aggregateType, Long aggregateId, String eventType, String jsonPayload) {
            super(aggregateType, aggregateId, eventType);
            this.jsonPayload = jsonPayload;
        }

        @Override
        public String toJsonPayload() {
            return jsonPayload;
        }
    }
}
