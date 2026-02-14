package de.dreistrom.income.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IncomeEntryTest {

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    }

    @Test
    void persist_andRetrieve_incomeEntry() {
        IncomeEntry entry = new IncomeEntry(user, IncomeStream.FREIBERUF,
                new BigDecimal("1500.50"), LocalDate.of(2026, 3, 15));
        IncomeEntry saved = incomeEntryRepository.save(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStreamType()).isEqualTo(IncomeStream.FREIBERUF);
        assertThat(saved.getAmount()).isEqualByComparingTo("1500.50");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getEntryDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persist_withClient_andDescription() {
        Client client = clientRepository.save(
                new Client(user, "Acme GmbH", IncomeStream.GEWERBE));

        IncomeEntry entry = new IncomeEntry(user, IncomeStream.GEWERBE,
                new BigDecimal("5000.00"), LocalDate.of(2026, 3, 1),
                "Projektabrechnung", client, "Q1 Beratung");
        IncomeEntry saved = incomeEntryRepository.save(entry);

        assertThat(saved.getSource()).isEqualTo("Projektabrechnung");
        assertThat(saved.getClient().getId()).isEqualTo(client.getId());
        assertThat(saved.getDescription()).isEqualTo("Q1 Beratung");
    }

    @Test
    void moneyConverter_storesCents_andRestoresEuros() {
        IncomeEntry entry = new IncomeEntry(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("2345.67"), LocalDate.of(2026, 1, 31));
        IncomeEntry saved = incomeEntryRepository.save(entry);
        incomeEntryRepository.flush();

        IncomeEntry fetched = incomeEntryRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getAmount()).isEqualByComparingTo("2345.67");
    }

    @Test
    void findByUserIdAndStreamType_filtersCorrectly() {
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.FREIBERUF,
                new BigDecimal("100.00"), LocalDate.of(2026, 3, 1)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.GEWERBE,
                new BigDecimal("200.00"), LocalDate.of(2026, 3, 1)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.FREIBERUF,
                new BigDecimal("300.00"), LocalDate.of(2026, 3, 15)));

        List<IncomeEntry> freelance = incomeEntryRepository
                .findByUserIdAndStreamType(user.getId(), IncomeStream.FREIBERUF);
        assertThat(freelance).hasSize(2);
    }

    @Test
    void findByUserIdAndEntryDateBetween_filtersDateRange() {
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("3000.00"), LocalDate.of(2026, 1, 31)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("3000.00"), LocalDate.of(2026, 2, 28)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.EMPLOYMENT,
                new BigDecimal("3000.00"), LocalDate.of(2026, 3, 31)));

        List<IncomeEntry> q1 = incomeEntryRepository.findByUserIdAndEntryDateBetween(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        assertThat(q1).hasSize(3);

        List<IncomeEntry> feb = incomeEntryRepository.findByUserIdAndEntryDateBetween(
                user.getId(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(feb).hasSize(1);
    }

    @Test
    void findByUserIdAndStreamTypeAndEntryDateBetween_combinesFilters() {
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.FREIBERUF,
                new BigDecimal("500.00"), LocalDate.of(2026, 3, 1)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.GEWERBE,
                new BigDecimal("700.00"), LocalDate.of(2026, 3, 15)));
        incomeEntryRepository.save(new IncomeEntry(user, IncomeStream.FREIBERUF,
                new BigDecimal("600.00"), LocalDate.of(2026, 4, 1)));

        List<IncomeEntry> marchFreelance = incomeEntryRepository
                .findByUserIdAndStreamTypeAndEntryDateBetween(
                        user.getId(), IncomeStream.FREIBERUF,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(marchFreelance).hasSize(1);
        assertThat(marchFreelance.getFirst().getAmount()).isEqualByComparingTo("500.00");
    }
}
