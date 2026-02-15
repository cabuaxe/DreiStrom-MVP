package de.dreistrom.expense.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.expense.domain.ExpenseEntry;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class ExpenseEntryCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BigDecimal amount;
    private final String category;
    private final LocalDate entryDate;
    private final Long allocationRuleId;
    private final boolean gwg;

    public ExpenseEntryCreated(ExpenseEntry entry, boolean gwg) {
        super("ExpenseEntry", entry.getId(), "EXPENSE_ENTRY_CREATED");
        this.amount = entry.getAmount();
        this.category = entry.getCategory();
        this.entryDate = entry.getEntryDate();
        this.allocationRuleId = entry.getAllocationRule() != null
                ? entry.getAllocationRule().getId() : null;
        this.gwg = gwg;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("amount", amount.toPlainString());
            node.put("category", category);
            node.put("entryDate", entryDate.toString());
            if (allocationRuleId != null) {
                node.put("allocationRuleId", allocationRuleId);
            }
            node.put("gwg", gwg);
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
