package de.dreistrom.tax.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.domain.Vorauszahlung;
import de.dreistrom.tax.domain.VorauszahlungStatus;
import de.dreistrom.tax.dto.VorauszahlungSchedule;
import de.dreistrom.tax.repository.VorauszahlungRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class VorauszahlungServiceTest {

    @Autowired private VorauszahlungService vorauszahlungService;
    @Autowired private VorauszahlungRepository vorauszahlungRepository;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        vorauszahlungRepository.deleteAll();
        incomeEntryRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "vz@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "VZ Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vz@dreistrom.de", null, List.of()));
    }

    @Nested
    class ScheduleGeneration {

        @Test
        void createsAllFourQuarters() {
            VorauszahlungSchedule schedule = vorauszahlungService.generateSchedule(
                    user, 2025, new BigDecimal("12000"));

            assertThat(schedule.year()).isEqualTo(2025);
            assertThat(schedule.payments()).hasSize(4);
            assertThat(schedule.quarterlyAmount()).isEqualByComparingTo("3000.00");
            assertThat(schedule.annualTotal()).isEqualByComparingTo("12000.00");
        }

        @Test
        void correctDueDates() {
            VorauszahlungSchedule schedule = vorauszahlungService.generateSchedule(
                    user, 2025, new BigDecimal("8000"));

            assertThat(schedule.payments().get(0).dueDate()).isEqualTo("2025-03-10");
            assertThat(schedule.payments().get(1).dueDate()).isEqualTo("2025-06-10");
            assertThat(schedule.payments().get(2).dueDate()).isEqualTo("2025-09-10");
            assertThat(schedule.payments().get(3).dueDate()).isEqualTo("2025-12-10");
        }

        @Test
        void persistsPayments() {
            vorauszahlungService.generateSchedule(user, 2025, new BigDecimal("10000"));

            List<Vorauszahlung> stored = vorauszahlungRepository
                    .findByUserIdAndYearOrderByQuarter(user.getId(), (short) 2025);
            assertThat(stored).hasSize(4);
            assertThat(stored.get(0).getAmount()).isEqualByComparingTo("2500.00");
        }

        @Test
        void idempotent_returnsSameSchedule() {
            vorauszahlungService.generateSchedule(user, 2025, new BigDecimal("10000"));
            VorauszahlungSchedule second = vorauszahlungService.generateSchedule(
                    user, 2025, new BigDecimal("10000"));

            assertThat(second.payments()).hasSize(4);
            // Should not create duplicates
            List<Vorauszahlung> stored = vorauszahlungRepository
                    .findByUserIdAndYearOrderByQuarter(user.getId(), (short) 2025);
            assertThat(stored).hasSize(4);
        }

        @Test
        void updatesAmountIfAssessmentBasisChanges() {
            vorauszahlungService.generateSchedule(user, 2025, new BigDecimal("10000"));
            VorauszahlungSchedule updated = vorauszahlungService.generateSchedule(
                    user, 2025, new BigDecimal("16000"));

            // Pending payments should be updated to new quarterly amount
            assertThat(updated.quarterlyAmount()).isEqualByComparingTo("4000.00");
        }
    }

    @Nested
    class PaymentRecording {

        @Test
        void recordsPaymentForQuarter() {
            vorauszahlungService.generateSchedule(user, 2025, new BigDecimal("12000"));

            Vorauszahlung paid = vorauszahlungService.recordPayment(
                    user.getId(), 2025, 1,
                    new BigDecimal("3000"), LocalDate.of(2025, 3, 8));

            assertThat(paid.getStatus()).isEqualTo(VorauszahlungStatus.PAID);
            assertThat(paid.getPaid()).isEqualByComparingTo("3000");
            assertThat(paid.getPaidDate()).isEqualTo(LocalDate.of(2025, 3, 8));
        }

        @Test
        void throwsWhenQuarterNotFound() {
            assertThatThrownBy(() ->
                    vorauszahlungService.recordPayment(user.getId(), 2025, 1,
                            new BigDecimal("3000"), LocalDate.of(2025, 3, 8)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Q1/2025");
        }
    }

    @Nested
    class DeviationCheck {

        @Test
        void noSuggestionWhenNoIncome() {
            VorauszahlungSchedule.AdjustmentSuggestion suggestion =
                    vorauszahlungService.checkDeviation(user.getId(), 2025,
                            new BigDecimal("20000"));

            assertThat(suggestion.recommended()).isFalse();
        }

        @Test
        void noSuggestionWhenZeroAssessment() {
            VorauszahlungSchedule.AdjustmentSuggestion suggestion =
                    vorauszahlungService.checkDeviation(user.getId(), 2025, BigDecimal.ZERO);

            assertThat(suggestion.recommended()).isFalse();
        }

        @Test
        void suggestsAdjustmentWhenSignificantDeviation() {
            // Create income that will project much higher than assessment
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                    LocalDate.of(2025, 1, 15), "Big project", null, null));

            VorauszahlungSchedule.AdjustmentSuggestion suggestion =
                    vorauszahlungService.checkDeviation(user.getId(), 2025,
                            new BigDecimal("20000"));

            // Projected annual income will be >> 20000, so deviation > 25%
            assertThat(suggestion.recommended()).isTrue();
            assertThat(suggestion.deviationPercent().compareTo(new BigDecimal("25"))).isGreaterThan(0);
        }
    }

    @Nested
    class OverdueDetection {

        @Test
        void marksOverduePaymentsInPast() {
            // Create schedule for a past year where all due dates have passed
            VorauszahlungSchedule schedule = vorauszahlungService.generateSchedule(
                    user, 2020, new BigDecimal("8000"));

            // All Q1-Q4 2020 due dates are in the past → should be OVERDUE
            long overdueCount = schedule.payments().stream()
                    .filter(p -> "OVERDUE".equals(p.status()))
                    .count();
            assertThat(overdueCount).isEqualTo(4);
        }
    }
}
