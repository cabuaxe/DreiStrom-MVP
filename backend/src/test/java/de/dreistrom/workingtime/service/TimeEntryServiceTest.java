package de.dreistrom.workingtime.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.workingtime.domain.ActivityType;
import de.dreistrom.workingtime.domain.TimeEntry;
import de.dreistrom.workingtime.dto.WeeklySummary;
import de.dreistrom.workingtime.repository.TimeEntryRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @InjectMocks
    private TimeEntryService service;

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Nested
    class Create {
        @Test
        void createsTimeEntry() {
            when(timeEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            TimeEntry result = service.create(user,
                    LocalDate.of(2026, 2, 10), new BigDecimal("8.00"),
                    ActivityType.EMPLOYMENT, "Office work");

            assertThat(result.getEntryDate()).isEqualTo(LocalDate.of(2026, 2, 10));
            assertThat(result.getHours()).isEqualByComparingTo("8.00");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.EMPLOYMENT);
            verify(timeEntryRepository).save(any());
        }
    }

    @Nested
    class Update {
        @Test
        void updatesExistingEntry() {
            TimeEntry entry = new TimeEntry(user, LocalDate.of(2026, 2, 10),
                    new BigDecimal("6.00"), ActivityType.FREIBERUF, "Consulting");
            when(timeEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

            TimeEntry result = service.update(1L, new BigDecimal("7.50"), "Extended consulting");

            assertThat(result.getHours()).isEqualByComparingTo("7.50");
            assertThat(result.getDescription()).isEqualTo("Extended consulting");
        }

        @Test
        void throwsWhenNotFound() {
            when(timeEntryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, new BigDecimal("4"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    class WeeklyAggregation {
        @Test
        void aggregatesWeeklyHours() {
            LocalDate monday = LocalDate.of(2026, 2, 9); // Monday
            LocalDate sunday = LocalDate.of(2026, 2, 15); // Sunday

            when(timeEntryRepository.sumHoursByType(any(), eq(ActivityType.EMPLOYMENT), eq(monday), eq(sunday)))
                    .thenReturn(new BigDecimal("40.00"));
            when(timeEntryRepository.sumHoursByType(any(), eq(ActivityType.FREIBERUF), eq(monday), eq(sunday)))
                    .thenReturn(new BigDecimal("10.00"));
            when(timeEntryRepository.sumHoursByType(any(), eq(ActivityType.GEWERBE), eq(monday), eq(sunday)))
                    .thenReturn(new BigDecimal("5.00"));

            List<WeeklySummary> summaries = service.getWeeklySummaries(1L, monday, sunday);

            assertThat(summaries).hasSize(1);
            WeeklySummary week = summaries.get(0);
            assertThat(week.weekStart()).isEqualTo(monday);
            assertThat(week.weekEnd()).isEqualTo(sunday);
            assertThat(week.employmentHours()).isEqualByComparingTo("40.00");
            assertThat(week.freiberufHours()).isEqualByComparingTo("10.00");
            assertThat(week.gewerbeHours()).isEqualByComparingTo("5.00");
            assertThat(week.selfEmployedHours()).isEqualByComparingTo("15.00");
            assertThat(week.totalHours()).isEqualByComparingTo("55.00");
        }

        @Test
        void handlesMultipleWeeks() {
            LocalDate from = LocalDate.of(2026, 2, 9);  // Monday
            LocalDate to = LocalDate.of(2026, 2, 22);    // Sunday

            when(timeEntryRepository.sumHoursByType(any(), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            List<WeeklySummary> summaries = service.getWeeklySummaries(1L, from, to);

            assertThat(summaries).hasSize(2);
            assertThat(summaries.get(0).weekStart()).isEqualTo(LocalDate.of(2026, 2, 9));
            assertThat(summaries.get(1).weekStart()).isEqualTo(LocalDate.of(2026, 2, 16));
        }

        @Test
        void alignsMidWeekDatesToWeekBoundaries() {
            // Wednesday to Thursday spans 2 weeks
            LocalDate from = LocalDate.of(2026, 2, 11); // Wednesday
            LocalDate to = LocalDate.of(2026, 2, 19);    // Thursday

            when(timeEntryRepository.sumHoursByType(any(), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            List<WeeklySummary> summaries = service.getWeeklySummaries(1L, from, to);

            // Aligned: Mon 9 to Sun 22 = 2 weeks
            assertThat(summaries).hasSize(2);
            assertThat(summaries.get(0).weekStart()).isEqualTo(LocalDate.of(2026, 2, 9));
        }
    }

    @Nested
    class AverageWeeklyHours {
        @Test
        void calculatesAverageForMonth() {
            when(timeEntryRepository.sumHoursByType(
                    eq(1L), eq(ActivityType.FREIBERUF),
                    eq(LocalDate.of(2026, 2, 1)), eq(LocalDate.of(2026, 2, 28))))
                    .thenReturn(new BigDecimal("48.00"));

            BigDecimal avg = service.getAverageWeeklyHours(1L, ActivityType.FREIBERUF, 2026, 2);

            // Feb 2026 = 28 days = 4.00 weeks → 48/4 = 12.0
            assertThat(avg).isEqualByComparingTo("12.0");
        }

        @Test
        void returnsZeroWhenNoHours() {
            when(timeEntryRepository.sumHoursByType(any(), any(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal avg = service.getAverageWeeklyHours(1L, ActivityType.GEWERBE, 2026, 3);

            assertThat(avg).isEqualByComparingTo("0");
        }
    }
}
