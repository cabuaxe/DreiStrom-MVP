package de.dreistrom.expense.repository;

import de.dreistrom.expense.domain.ExpenseEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseEntryRepository extends JpaRepository<ExpenseEntry, Long> {

    List<ExpenseEntry> findByUserId(Long userId);

    List<ExpenseEntry> findByUserIdAndCategory(Long userId, String category);

    List<ExpenseEntry> findByUserIdAndEntryDateBetween(Long userId, LocalDate from, LocalDate to);

    List<ExpenseEntry> findByUserIdAndCategoryAndEntryDateBetween(
            Long userId, String category, LocalDate from, LocalDate to);

    /**
     * Sum allocated Freiberuf expense cents in a date range.
     * INNER JOIN excludes expenses without an allocation rule.
     */
    @Query(value = "SELECT SUM(e.amount_cents * ar.freiberuf_pct / 100) " +
                   "FROM expense_entry e " +
                   "INNER JOIN allocation_rule ar ON e.allocation_rule_id = ar.id " +
                   "WHERE e.user_id = :userId " +
                   "AND e.entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    Long sumCentsFreiberufByDateRange(@Param("userId") Long userId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    /**
     * Sum allocated Gewerbe expense cents in a date range.
     * INNER JOIN excludes expenses without an allocation rule.
     */
    @Query(value = "SELECT SUM(e.amount_cents * ar.gewerbe_pct / 100) " +
                   "FROM expense_entry e " +
                   "INNER JOIN allocation_rule ar ON e.allocation_rule_id = ar.id " +
                   "WHERE e.user_id = :userId " +
                   "AND e.entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    Long sumCentsGewerbeByDateRange(@Param("userId") Long userId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    /**
     * Sum allocated Personal expense cents in a date range.
     * INNER JOIN excludes expenses without an allocation rule.
     */
    @Query(value = "SELECT SUM(e.amount_cents * ar.personal_pct / 100) " +
                   "FROM expense_entry e " +
                   "INNER JOIN allocation_rule ar ON e.allocation_rule_id = ar.id " +
                   "WHERE e.user_id = :userId " +
                   "AND e.entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    Long sumCentsPersonalByDateRange(@Param("userId") Long userId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to);

    /**
     * Sum total gross expense cents in a date range (no allocation JOIN).
     */
    @Query(value = "SELECT SUM(amount_cents) FROM expense_entry " +
                   "WHERE user_id = :userId " +
                   "AND entry_date BETWEEN :from AND :to",
           nativeQuery = true)
    Long sumCentsByDateRange(@Param("userId") Long userId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);
}
