package de.dreistrom.audit.event;

import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class DomainEvent {

    private final String aggregateType;
    private final Long aggregateId;
    private final String eventType;
    private final Instant occurredAt;

    protected DomainEvent(String aggregateType, Long aggregateId, String eventType) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.occurredAt = Instant.now();
    }

    /**
     * Serialise the event-specific payload to JSON.
     * Implementations should include before/after state where applicable.
     */
    public abstract String toJsonPayload();
}
