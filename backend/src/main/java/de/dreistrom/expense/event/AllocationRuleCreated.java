package de.dreistrom.expense.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.expense.domain.AllocationRule;

public class AllocationRuleCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final short freiberufPct;
    private final short gewerbePct;
    private final short personalPct;

    public AllocationRuleCreated(AllocationRule rule) {
        super("AllocationRule", rule.getId(), "ALLOCATION_RULE_CREATED");
        this.name = rule.getName();
        this.freiberufPct = rule.getFreiberufPct();
        this.gewerbePct = rule.getGewerbePct();
        this.personalPct = rule.getPersonalPct();
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", name);
            node.put("freiberufPct", freiberufPct);
            node.put("gewerbePct", gewerbePct);
            node.put("personalPct", personalPct);
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
