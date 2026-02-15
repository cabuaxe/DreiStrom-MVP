package de.dreistrom.expense.repository;

import de.dreistrom.expense.domain.AllocationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocationRuleRepository extends JpaRepository<AllocationRule, Long> {

    List<AllocationRule> findByUserId(Long userId);
}
