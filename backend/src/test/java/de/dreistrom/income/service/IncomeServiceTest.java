package de.dreistrom.income.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.repository.EventLogRepository;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
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
class IncomeServiceTest {

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private IncomeEntryRepository incomeEntryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        incomeEntryRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();
        eventLogRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "owner@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Test Owner"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@dreistrom.de", null, List.of()));
    }

    @Test
    void create_persistsEntryAndAuditEvent() {
        IncomeEntry entry = incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("1500.00"), LocalDate.of(2026, 3, 15),
                "Beratung", null, "Projektarbeit");

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getStreamType()).isEqualTo(IncomeStream.FREIBERUF);
        assertThat(entry.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(entry.getSource()).isEqualTo("Beratung");

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("IncomeEntry", entry.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo("INCOME_ENTRY_CREATED");
        assertThat(events.getFirst().getPayload()).contains("FREIBERUF");
        assertThat(events.getFirst().getActor()).isEqualTo("owner@dreistrom.de");
    }

    @Test
    void create_withClient_linksClientToEntry() {
        Client client = clientRepository.save(
                new Client(user, "Acme GmbH", IncomeStream.GEWERBE));

        IncomeEntry entry = incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("5000.00"), LocalDate.of(2026, 4, 1),
                "Rechnung #101", client, "Q1 Beratung");

        assertThat(entry.getClient().getId()).isEqualTo(client.getId());
    }

    @Test
    void update_modifiesFieldsAndPersistsAuditEvent() {
        IncomeEntry entry = incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1),
                "Original", null, null);

        IncomeEntry updated = incomeService.update(entry.getId(), IncomeStream.FREIBERUF,
                new BigDecimal("1200.00"), LocalDate.of(2026, 3, 5),
                "Korrigiert", null, "Nachberechnung");

        assertThat(updated.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(updated.getEntryDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(updated.getSource()).isEqualTo("Korrigiert");
        assertThat(updated.getDescription()).isEqualTo("Nachberechnung");

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("IncomeEntry", entry.getId());
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("INCOME_ENTRY_CREATED");
        assertThat(events.get(1).getEventType()).isEqualTo("INCOME_ENTRY_MODIFIED");
        assertThat(events.get(1).getPayload()).contains("\"before\"");
        assertThat(events.get(1).getPayload()).contains("\"after\"");
    }

    @Test
    void update_crossStream_throwsIllegalArgument() {
        IncomeEntry entry = incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1),
                null, null, null);

        assertThatThrownBy(() -> incomeService.update(entry.getId(), IncomeStream.GEWERBE,
                new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1),
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cross-stream update not allowed")
                .hasMessageContaining("FREIBERUF")
                .hasMessageContaining("GEWERBE");
    }

    @Test
    void update_nonExistentEntry_throwsEntityNotFound() {
        assertThatThrownBy(() -> incomeService.update(999L, IncomeStream.FREIBERUF,
                new BigDecimal("100.00"), LocalDate.now(), null, null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("IncomeEntry");
    }

    @Test
    void listByStream_returnsOnlyMatchingStream() {
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("1000.00"), LocalDate.of(2026, 3, 1),
                null, null, null);
        incomeService.create(user, IncomeStream.GEWERBE,
                new BigDecimal("2000.00"), LocalDate.of(2026, 3, 15),
                null, null, null);
        incomeService.create(user, IncomeStream.FREIBERUF,
                new BigDecimal("3000.00"), LocalDate.of(2026, 4, 1),
                null, null, null);

        List<IncomeEntry> freelance = incomeService.listByStream(
                user.getId(), IncomeStream.FREIBERUF);

        assertThat(freelance).hasSize(2);
        assertThat(freelance).allMatch(e -> e.getStreamType() == IncomeStream.FREIBERUF);
    }

    @Test
    void listByStream_returnsEmptyListWhenNoEntries() {
        List<IncomeEntry> entries = incomeService.listByStream(
                user.getId(), IncomeStream.EMPLOYMENT);
        assertThat(entries).isEmpty();
    }
}
