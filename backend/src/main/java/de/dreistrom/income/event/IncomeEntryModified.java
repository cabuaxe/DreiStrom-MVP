package de.dreistrom.income.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncomeEntryModified extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BigDecimal beforeAmount;
    private final BigDecimal afterAmount;
    private final LocalDate beforeDate;
    private final LocalDate afterDate;
    private final String beforeSource;
    private final String afterSource;

    public IncomeEntryModified(Long entryId,
                               BigDecimal beforeAmount, BigDecimal afterAmount,
                               LocalDate beforeDate, LocalDate afterDate,
                               String beforeSource, String afterSource) {
        super("IncomeEntry", entryId, "INCOME_ENTRY_MODIFIED");
        this.beforeAmount = beforeAmount;
        this.afterAmount = afterAmount;
        this.beforeDate = beforeDate;
        this.afterDate = afterDate;
        this.beforeSource = beforeSource;
        this.afterSource = afterSource;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            ObjectNode before = node.putObject("before");
            before.put("amount", beforeAmount.toPlainString());
            before.put("entryDate", beforeDate.toString());
            if (beforeSource != null) before.put("source", beforeSource);

            ObjectNode after = node.putObject("after");
            after.put("amount", afterAmount.toPlainString());
            after.put("entryDate", afterDate.toString());
            if (afterSource != null) after.put("source", afterSource);

            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
