package de.dreistrom.audit.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.audit.repository.EventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final EventLogRepository eventLogRepository;

    /**
     * Persist a domain event to the append-only event_log table.
     * Must be called within the same @Transactional boundary as the
     * aggregate mutation (co-persistence pattern, GoBD compliance).
     */
    public EventLog persist(DomainEvent event) {
        String actor = resolveActor();
        EventLog entry = new EventLog(
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.toJsonPayload(),
                actor
        );
        EventLog saved = eventLogRepository.save(entry);
        log.debug("Audit event persisted: type={}, aggregateId={}, actor={}",
                event.getEventType(), event.getAggregateId(), actor);
        return saved;
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }
}
