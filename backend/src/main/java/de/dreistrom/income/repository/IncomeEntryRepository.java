package de.dreistrom.income.repository;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.IncomeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, Long> {

    List<IncomeEntry> findByUserIdAndStreamType(Long userId, IncomeStream streamType);

    List<IncomeEntry> findByUserIdAndEntryDateBetween(Long userId, LocalDate from, LocalDate to);

    List<IncomeEntry> findByUserIdAndStreamTypeAndEntryDateBetween(
            Long userId, IncomeStream streamType, LocalDate from, LocalDate to);
}
