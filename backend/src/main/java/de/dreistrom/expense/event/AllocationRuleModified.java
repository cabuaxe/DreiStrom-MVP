package de.dreistrom.expense.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;

public class AllocationRuleModified extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String beforeName;
    private final String afterName;
    private final short beforeFreiberuf;
    private final short afterFreiberuf;
    private final short beforeGewerbe;
    private final short afterGewerbe;
    private final short beforePersonal;
    private final short afterPersonal;

    public AllocationRuleModified(Long ruleId,
                                  String beforeName, String afterName,
                                  short beforeFreiberuf, short afterFreiberuf,
                                  short beforeGewerbe, short afterGewerbe,
                                  short beforePersonal, short afterPersonal) {
        super("AllocationRule", ruleId, "ALLOCATION_RULE_MODIFIED");
        this.beforeName = beforeName;
        this.afterName = afterName;
        this.beforeFreiberuf = beforeFreiberuf;
        this.afterFreiberuf = afterFreiberuf;
        this.beforeGewerbe = beforeGewerbe;
        this.afterGewerbe = afterGewerbe;
        this.beforePersonal = beforePersonal;
        this.afterPersonal = afterPersonal;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            ObjectNode before = node.putObject("before");
            before.put("name", beforeName);
            before.put("freiberufPct", beforeFreiberuf);
            before.put("gewerbePct", beforeGewerbe);
            before.put("personalPct", beforePersonal);

            ObjectNode after = node.putObject("after");
            after.put("name", afterName);
            after.put("freiberufPct", afterFreiberuf);
            after.put("gewerbePct", afterGewerbe);
            after.put("personalPct", afterPersonal);

            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
