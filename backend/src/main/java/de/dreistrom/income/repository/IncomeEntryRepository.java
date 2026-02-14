package de.dreistrom.income.repository;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.IncomeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, Long> {

    List<IncomeEntry> findByUserId(Long userId);

    List<IncomeEntry> findByUserIdAndStreamType(Long userId, IncomeStream streamType);

    List<IncomeEntry> findByUserIdAndEntryDateBetween(Long userId, LocalDate from, LocalDate to);

    List<IncomeEntry> findByUserIdAndStreamTypeAndEntryDateBetween(
            Long userId, IncomeStream streamType, LocalDate from, LocalDate to);

    /**
     * Sum amount_cents for a given user, stream type and year.
     * Returns the total in cents, or null if no entries exist.
     */
    @Query(value = "SELECT SUM(amount_cents) FROM income_entry " +
                   "WHERE user_id = :userId " +
                   "AND stream_type = :streamType " +
                   "AND entry_date BETWEEN :yearStart AND :yearEnd",
           nativeQuery = true)
    Long sumCentsByStreamAndDateRange(@Param("userId") Long userId,
                                     @Param("streamType") String streamType,
                                     @Param("yearStart") LocalDate yearStart,
                                     @Param("yearEnd") LocalDate yearEnd);

    /**
     * Sum amount_cents for all self-employed streams (FREIBERUF + GEWERBE) in a date range.
     * Returns the total in cents, or null if no entries exist.
     */
    @Query(value = "SELECT SUM(amount_cents) FROM income_entry " +
                   "WHERE user_id = :userId " +
                   "AND stream_type IN ('FREIBERUF', 'GEWERBE') " +
                   "AND entry_date BETWEEN :yearStart AND :yearEnd",
           nativeQuery = true)
    Long sumCentsSelfEmployedByDateRange(@Param("userId") Long userId,
                                        @Param("yearStart") LocalDate yearStart,
                                        @Param("yearEnd") LocalDate yearEnd);
}
