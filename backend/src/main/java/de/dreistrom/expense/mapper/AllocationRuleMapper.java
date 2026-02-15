package de.dreistrom.expense.mapper;

import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.dto.AllocationRuleResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AllocationRuleMapper {

    AllocationRuleResponse toResponse(AllocationRule rule);

    List<AllocationRuleResponse> toResponseList(List<AllocationRule> rules);
}
