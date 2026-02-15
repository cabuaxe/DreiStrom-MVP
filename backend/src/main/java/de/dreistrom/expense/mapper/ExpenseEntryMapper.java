package de.dreistrom.expense.mapper;

import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.dto.AllocationRuleSummary;
import de.dreistrom.expense.dto.ExpenseEntryResponse;
import de.dreistrom.expense.service.ExpenseService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ExpenseEntryMapper {

    @Autowired
    protected ExpenseService expenseService;

    @Mapping(target = "allocationRule", source = "allocationRule", qualifiedByName = "ruleToSummary")
    @Mapping(target = "gwg", expression = "java(expenseService.isGwg(entry.getAmount()))")
    public abstract ExpenseEntryResponse toResponse(ExpenseEntry entry);

    public abstract List<ExpenseEntryResponse> toResponseList(List<ExpenseEntry> entries);

    @Named("ruleToSummary")
    AllocationRuleSummary ruleToSummary(AllocationRule rule) {
        if (rule == null) {
            return null;
        }
        return new AllocationRuleSummary(
                rule.getId(), rule.getName(),
                rule.getFreiberufPct(), rule.getGewerbePct(), rule.getPersonalPct());
    }
}
