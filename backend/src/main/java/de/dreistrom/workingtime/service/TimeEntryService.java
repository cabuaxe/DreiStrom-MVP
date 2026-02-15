package de.dreistrom.workingtime.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.workingtime.domain.ActivityType;
import de.dreistrom.workingtime.domain.TimeEntry;
import de.dreistrom.workingtime.dto.WeeklySummary;
import de.dreistrom.workingtime.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;

    @Transactional
    public TimeEntry create(AppUser user, LocalDate date, BigDecimal hours,
                            ActivityType activityType, String description) {
        return timeEntryRepository.save(
                new TimeEntry(user, date, hours, activityType, description));
    }

    @Transactional
    public TimeEntry update(Long id, BigDecimal hours, String description) {
        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Time entry not found: " + id));
        entry.update(hours, description);
        return entry;
    }

    @Transactional
    public void delete(Long id) {
        timeEntryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<TimeEntry> getEntriesForDate(Long userId, LocalDate date) {
        return timeEntryRepository.findByUserIdAndEntryDateOrderByCreatedAtAsc(userId, date);
    }

    @Transactional(readOnly = true)
    public List<TimeEntry> getEntriesForRange(Long userId, LocalDate from, LocalDate to) {
        return timeEntryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateAsc(
                userId, from, to);
    }

    /**
     * Weekly aggregation for a date range. Returns one WeeklySummary per ISO week.
     */
    @Transactional(readOnly = true)
    public List<WeeklySummary> getWeeklySummaries(Long userId, LocalDate from, LocalDate to) {
        // Align to week boundaries (Monday-Sunday)
        LocalDate weekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate rangeEnd = to.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<WeeklySummary> summaries = new ArrayList<>();

        while (!weekStart.isAfter(rangeEnd)) {
            LocalDate weekEnd = weekStart.plusDays(6);

            BigDecimal employment = timeEntryRepository.sumHoursByType(
                    userId, ActivityType.EMPLOYMENT, weekStart, weekEnd);
            BigDecimal freiberuf = timeEntryRepository.sumHoursByType(
                    userId, ActivityType.FREIBERUF, weekStart, weekEnd);
            BigDecimal gewerbe = timeEntryRepository.sumHoursByType(
                    userId, ActivityType.GEWERBE, weekStart, weekEnd);

            BigDecimal selfEmployed = freiberuf.add(gewerbe);
            BigDecimal total = employment.add(selfEmployed);

            summaries.add(new WeeklySummary(
                    weekStart, weekEnd,
                    employment, freiberuf, gewerbe,
                    total, selfEmployed));

            weekStart = weekStart.plusWeeks(1);
        }

        return summaries;
    }

    /**
     * Average weekly hours by activity type for a month.
     * Used by social insurance monitor.
     */
    @Transactional(readOnly = true)
    public BigDecimal getAverageWeeklyHours(Long userId, ActivityType type,
                                            int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

        BigDecimal totalHours = timeEntryRepository.sumHoursByType(
                userId, type, monthStart, monthEnd);

        // Approximate weeks in month
        long days = monthEnd.toEpochDay() - monthStart.toEpochDay() + 1;
        BigDecimal weeks = new BigDecimal(days).divide(new BigDecimal("7"), 2, RoundingMode.HALF_UP);

        if (weeks.signum() == 0) return BigDecimal.ZERO;
        return totalHours.divide(weeks, 1, RoundingMode.HALF_UP);
    }
}
