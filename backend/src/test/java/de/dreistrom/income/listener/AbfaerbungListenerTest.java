package de.dreistrom.income.listener;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.income.service.IncomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@RecordApplicationEvents
class AbfaerbungListenerTest {

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
        incomeEntryRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "owner@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Test Owner"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@dreistrom.de", null, List.of()));
    }

    @Test
    void alertFires_whenBothThresholdsExceeded() {
        // Gewerbe > 24500 AND ratio > 3%
        // Setup: Gewerbe = 25000, Freiberuf = 50000, total = 75000
        // Ratio = 25000/75000 = 0.3333 (33.33%) > 3%
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("50000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("25000.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getType()).isEqualTo(ThresholdType.ABFAERBUNG);
        assertThat(alerts.getFirst().getRatio()).isEqualByComparingTo("0.3333");
        assertThat(alerts.getFirst().getGewerbeRevenue()).isEqualByComparingTo("25000.00");
        assertThat(alerts.getFirst().getUserId()).isEqualTo(user.getId());
        assertThat(alerts.getFirst().getYear()).isEqualTo(2026);
    }

    @Test
    void alertDoesNotFire_whenRatioBelow3Percent() {
        // Gewerbe = 25000, Freiberuf = 900000, total = 925000
        // Ratio = 25000/925000 = 0.0270 (2.7%) <= 3%
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("900000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("25000.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).isEmpty();
    }

    @Test
    void alertDoesNotFire_whenGewerbeBelow24500() {
        // Gewerbe = 24000, Freiberuf = 30000, total = 54000
        // Ratio = 24000/54000 = 0.4444 (44.44%) > 3%, but Gewerbe <= 24500
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("30000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("24000.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).isEmpty();
    }

    @Test
    void alertDoesNotFire_whenOnlyFreiberufEntries() {
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("100000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).isEmpty();
    }

    @Test
    void alertDoesNotFire_whenOnlyEmploymentEntries() {
        incomeService.create(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("50000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).isEmpty();
    }

    @Test
    void alertFires_onUpdateThatCrossesThreshold() {
        // Initially: Gewerbe = 20000 (below 24500)
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("50000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);
        IncomeEntry gewerbeEntry = incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("20000.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        // No alert yet
        long alertsBefore = events.stream(ThresholdAlert.class).count();
        assertThat(alertsBefore).isZero();

        // Update Gewerbe to 30000 (exceeds 24500, ratio = 30000/80000 = 0.375 > 3%)
        incomeService.update(gewerbeEntry.getId(), IncomeStream.GEWERBE,
                new BigDecimal("30000.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getGewerbeRevenue()).isEqualByComparingTo("30000.00");
    }

    @Test
    void ratio_usesFourDecimalPrecision() {
        // Gewerbe = 25001, Freiberuf = 50000, total = 75001
        // Ratio = 25001/75001 = 0.3333... -> rounded to 4 decimal = 0.3333
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("50000.00"), LocalDate.of(2026, 6, 1),
                null, null, null);
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("25001.00"), LocalDate.of(2026, 6, 15),
                null, null, null);

        List<ThresholdAlert> alerts = events.stream(ThresholdAlert.class).toList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getRatio().scale()).isEqualTo(4);
    }
}
