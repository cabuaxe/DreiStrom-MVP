package de.dreistrom.income.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.income.domain.IncomeEntry;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeEntryCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String streamType;
    private final BigDecimal amount;
    private final LocalDate entryDate;
    private final String source;

    public IncomeEntryCreated(IncomeEntry entry) {
        super("IncomeEntry", entry.getId(), "INCOME_ENTRY_CREATED");
        this.streamType = entry.getStreamType().name();
        this.amount = entry.getAmount();
        this.entryDate = entry.getEntryDate();
        this.source = entry.getSource();
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("streamType", streamType);
            node.put("amount", amount.toPlainString());
            node.put("entryDate", entryDate.toString());
            if (source != null) {
                node.put("source", source);
            }
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
