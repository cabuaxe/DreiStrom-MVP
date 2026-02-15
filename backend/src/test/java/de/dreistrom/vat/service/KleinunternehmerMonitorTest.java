package de.dreistrom.vat.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.income.service.IncomeService;
import de.dreistrom.vat.service.KleinunternehmerMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class KleinunternehmerMonitorTest {

    /** Fix clock to July 1, 2026 (day 182 of 365) for deterministic projection tests. */
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    @Autowired
    private KleinunternehmerMonitor kleinunternehmerMonitor;

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ApplicationEvents events;

    private AppUser user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kleinunternehmerMonitor, "clock", FIXED_CLOCK);

        incomeEntryRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "kleinunternehmer@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Test Kleinunternehmer"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("kleinunternehmer@dreistrom.de", null, List.of()));
    }

    // ── Current-year threshold (€22,000 × 80% = €17,600) ─────────────

    @Nested
    class CurrentYearThreshold {

        @Test
        void alertFires_whenRevenueExceeds80PercentOfLimit() {
            // €18,000 → 18000/22000 = 0.8182 ≥ 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("18000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.getFirst().getRatio()).isEqualByComparingTo("0.8182");
            assertThat(alerts.getFirst().getGewerbeRevenue()).isEqualByComparingTo("18000.00");
            assertThat(alerts.getFirst().getUserId()).isEqualTo(user.getId());
            assertThat(alerts.getFirst().getYear()).isEqualTo(2026);
        }

        @Test
        void alertDoesNotFire_whenRevenueBelowWarningThreshold() {
            // €15,000 → 15000/22000 = 0.6818 < 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("15000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            assertThat(alerts).isEmpty();
        }

        @Test
        void alertFiresAtExactly80Percent() {
            // €17,600 → 17600/22000 = 0.8000 ≥ 0.80
            incomeService.create(user, IncomeStream.GEWERBE,
                    new BigDecimal("17600.00"), LocalDate.of(2026, 3, 15),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.getFirst().getRatio()).isEqualByComparingTo("0.8000");
        }
    }

    // ── Projected threshold (€50,000 × 80% = €40,000) ────────────────
    // Clock fixed to July 1 (day 182): projected = revenue × 365 / 182

    @Nested
    class ProjectedThreshold {

        @Test
        void alertFires_whenProjectedExceeds80PercentOfLimit() {
            // €22,000 on June 1, projected = 22000 × 365 / 182 = €44,120.88
            // 44120.88 / 50000 = 0.8824 ≥ 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("22000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();
            assertThat(alerts).hasSize(1);
            assertThat(alerts.getFirst().getGewerbeRevenue())
                    .isGreaterThan(new BigDecimal("44000"));
        }

        @Test
        void alertDoesNotFire_whenProjectedBelowWarningThreshold() {
            // €18,000 on June 1, projected = 18000 × 365 / 182 = €36,098.90
            // 36098.90 / 50000 = 0.7220 < 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("18000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();
            assertThat(alerts).isEmpty();
        }

        @Test
        void projectionUsesLinearExtrapolation() {
            // €20,000 by day 182 → projected = 20000 × 365 / 182 = €40,109.89
            // This just barely exceeds 80% of €50,000 (= €40,000)
            incomeService.create(user, IncomeStream.GEWERBE,
                    new BigDecimal("20000.00"), LocalDate.of(2026, 5, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();
            assertThat(alerts).hasSize(1);
            // Verify the projected value is the linear extrapolation
            BigDecimal projected = alerts.getFirst().getGewerbeRevenue();
            assertThat(projected).isEqualByComparingTo("40109.89");
        }
    }

    // ── Combined stream tracking ──────────────────────────────────────

    @Nested
    class CombinedStreamTracking {

        @Test
        void combinedFreiberufAndGewerbeRevenueTriggers() {
            // €10,000 FREIBERUF + €10,000 GEWERBE = €20,000
            // 20000/22000 = 0.9091 ≥ 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("10000.00"), LocalDate.of(2026, 3, 1),
                    null, null, null);
            incomeService.create(user, IncomeStream.GEWERBE,
                    new BigDecimal("10000.00"), LocalDate.of(2026, 4, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            // Second entry triggers (first was only €10,000 = 0.4545 < 0.80)
            assertThat(alerts).hasSize(1);
            assertThat(alerts.getFirst().getGewerbeRevenue()).isEqualByComparingTo("20000.00");
        }

        @Test
        void employmentIncomeDoesNotCount() {
            // €50,000 EMPLOYMENT → not tracked (only FREIBERUF + GEWERBE)
            incomeService.create(user, IncomeStream.EMPLOYMENT,
                    new BigDecimal("50000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR
                            || a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();
            assertThat(alerts).isEmpty();
        }
    }

    // ── Update triggers ───────────────────────────────────────────────

    @Nested
    class UpdateTriggers {

        @Test
        void alertFiresOnUpdateThatCrossesThreshold() {
            // Initially: €15,000 (below warning)
            IncomeEntry entry = incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("15000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            long alertsBefore = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .count();
            assertThat(alertsBefore).isZero();

            // Update to €20,000 → 20000/22000 = 0.9091 ≥ 0.80
            incomeService.update(entry.getId(), IncomeStream.FREIBERUF,
                    new BigDecimal("20000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            assertThat(alerts).hasSize(1);
        }
    }

    // ── Both alerts simultaneously ────────────────────────────────────

    @Nested
    class BothAlerts {

        @Test
        void bothCurrentAndProjectedAlertsFire() {
            // €22,000 → current: 22000/22000 = 1.0 ≥ 0.80
            // projected: 22000 × 365 / 182 = 44120.88, 44120.88/50000 = 0.8824 ≥ 0.80
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("22000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> currentAlerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            List<ThresholdAlert> projectedAlerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();

            assertThat(currentAlerts).hasSize(1);
            assertThat(projectedAlerts).hasSize(1);
        }

        @Test
        void currentAlertWithoutProjectedAlert() {
            // €18,000 → current: 18000/22000 = 0.8182 ≥ 0.80 → FIRES
            // projected: 18000 × 365 / 182 = 36098.90, 36098.90/50000 = 0.7220 < 0.80 → NO
            incomeService.create(user, IncomeStream.FREIBERUF,
                    new BigDecimal("18000.00"), LocalDate.of(2026, 6, 1),
                    null, null, null);

            List<ThresholdAlert> currentAlerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR)
                    .toList();
            List<ThresholdAlert> projectedAlerts = events.stream(ThresholdAlert.class)
                    .filter(a -> a.getType() == ThresholdType.KLEINUNTERNEHMER_PROJECTED)
                    .toList();

            assertThat(currentAlerts).hasSize(1);
            assertThat(projectedAlerts).isEmpty();
        }
    }
}
