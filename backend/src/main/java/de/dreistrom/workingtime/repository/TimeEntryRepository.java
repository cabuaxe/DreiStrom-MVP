package de.dreistrom.workingtime.repository;

import de.dreistrom.workingtime.domain.ActivityType;
import de.dreistrom.workingtime.domain.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(
            Long userId, LocalDate from, LocalDate to);

    List<TimeEntry> findByUserIdAndActivityTypeAndEntryDateBetweenOrderByEntryDateAsc(
            Long userId, ActivityType activityType, LocalDate from, LocalDate to);

    List<TimeEntry> findByUserIdAndEntryDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t " +
            "WHERE t.user.id = :userId AND t.entryDate BETWEEN :from AND :to")
    BigDecimal sumHours(@Param("userId") Long userId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t " +
            "WHERE t.user.id = :userId AND t.activityType = :type " +
            "AND t.entryDate BETWEEN :from AND :to")
    BigDecimal sumHoursByType(@Param("userId") Long userId,
                              @Param("type") ActivityType type,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}
