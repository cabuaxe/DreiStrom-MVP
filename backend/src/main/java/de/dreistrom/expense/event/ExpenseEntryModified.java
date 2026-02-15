package de.dreistrom.expense.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpenseEntryModified extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BigDecimal beforeAmount;
    private final BigDecimal afterAmount;
    private final String beforeCategory;
    private final String afterCategory;
    private final LocalDate beforeDate;
    private final LocalDate afterDate;

    public ExpenseEntryModified(Long entryId,
                                BigDecimal beforeAmount, BigDecimal afterAmount,
                                String beforeCategory, String afterCategory,
                                LocalDate beforeDate, LocalDate afterDate) {
        super("ExpenseEntry", entryId, "EXPENSE_ENTRY_MODIFIED");
        this.beforeAmount = beforeAmount;
        this.afterAmount = afterAmount;
        this.beforeCategory = beforeCategory;
        this.afterCategory = afterCategory;
        this.beforeDate = beforeDate;
        this.afterDate = afterDate;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            ObjectNode before = node.putObject("before");
            before.put("amount", beforeAmount.toPlainString());
            before.put("category", beforeCategory);
            before.put("entryDate", beforeDate.toString());

            ObjectNode after = node.putObject("after");
            after.put("amount", afterAmount.toPlainString());
            after.put("category", afterCategory);
            after.put("entryDate", afterDate.toString());

            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
