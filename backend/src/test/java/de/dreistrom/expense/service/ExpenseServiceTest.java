package de.dreistrom.expense.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.repository.EventLogRepository;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
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
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseEntryRepository expenseEntryRepository;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private DepreciationAssetRepository depreciationAssetRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        depreciationAssetRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();
        eventLogRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "expense@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Expense Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("expense@dreistrom.de", null, List.of()));
    }

    @Test
    void create_persistsEntryAndAuditEvent() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("250.00"),
                "Büromaterial", LocalDate.of(2026, 3, 15), null, null, "Druckerpapier");

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getAmount()).isEqualByComparingTo("250.00");
        assertThat(entry.getCategory()).isEqualTo("Büromaterial");
        assertThat(entry.getDescription()).isEqualTo("Druckerpapier");

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ExpenseEntry", entry.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getEventType()).isEqualTo("EXPENSE_ENTRY_CREATED");
        assertThat(events.getFirst().getPayload()).contains("Büromaterial");
        assertThat(events.getFirst().getActor()).isEqualTo("expense@dreistrom.de");
    }

    @Test
    void create_withAllocationRule_linksRuleToEntry() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Office Split", (short) 50, (short) 30, (short) 20));

        ExpenseEntry entry = expenseService.create(user, new BigDecimal("1200.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule.getId(), null, "Büro-Anteil");

        assertThat(entry.getAllocationRule().getId()).isEqualTo(rule.getId());
    }

    @Test
    void create_gwgItem_flaggedInEvent_noDepreciation() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("799.00"),
                "Arbeitsmittel", LocalDate.of(2026, 3, 1), null, null, "Tastatur");

        // GWG: no depreciation asset should be created
        List<DepreciationAsset> assets = depreciationAssetRepository.findByExpenseEntryId(entry.getId());
        assertThat(assets).isEmpty();

        // Event should flag as GWG
        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ExpenseEntry", entry.getId());
        assertThat(events.getFirst().getPayload()).contains("\"gwg\":true");
    }

    @Test
    void create_exactGwgThreshold_isGwg() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("800.00"),
                "Arbeitsmittel", LocalDate.of(2026, 3, 1), null, null, "Monitor");

        // Exactly 800 EUR is still GWG
        List<DepreciationAsset> assets = depreciationAssetRepository.findByExpenseEntryId(entry.getId());
        assertThat(assets).isEmpty();

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ExpenseEntry", entry.getId());
        assertThat(events.getFirst().getPayload()).contains("\"gwg\":true");
    }

    @Test
    void create_aboveGwg_autoCreatesDepreciationAsset() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("2400.00"),
                "Arbeitsmittel", LocalDate.of(2026, 1, 15), null, null, "MacBook Pro");

        // Should auto-create depreciation asset with 36-month useful life
        List<DepreciationAsset> assets = depreciationAssetRepository.findByExpenseEntryId(entry.getId());
        assertThat(assets).hasSize(1);

        DepreciationAsset asset = assets.getFirst();
        assertThat(asset.getName()).isEqualTo("Arbeitsmittel");
        assertThat(asset.getNetCost()).isEqualByComparingTo("2400.00");
        assertThat(asset.getUsefulLifeMonths()).isEqualTo(36);
        // Annual AfA = 2400 / 3 = 800.00
        assertThat(asset.getAnnualAfa()).isEqualByComparingTo("800.00");
        assertThat(asset.getExpenseEntry().getId()).isEqualTo(entry.getId());

        // Expense event should not be flagged as GWG
        List<EventLog> expenseEvents = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ExpenseEntry", entry.getId());
        assertThat(expenseEvents.getFirst().getPayload()).contains("\"gwg\":false");

        // Depreciation asset audit event
        List<EventLog> assetEvents = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("DepreciationAsset", asset.getId());
        assertThat(assetEvents).hasSize(1);
        assertThat(assetEvents.getFirst().getEventType()).isEqualTo("DEPRECIATION_ASSET_CREATED");
    }

    @Test
    void update_modifiesFieldsAndPersistsAuditEvent() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("100.00"),
                "Büromaterial", LocalDate.of(2026, 3, 1), null, null, null);

        ExpenseEntry updated = expenseService.update(entry.getId(), user.getId(),
                new BigDecimal("150.00"), "Reisekosten", LocalDate.of(2026, 3, 5),
                null, null, "Korrigiert");

        assertThat(updated.getAmount()).isEqualByComparingTo("150.00");
        assertThat(updated.getCategory()).isEqualTo("Reisekosten");
        assertThat(updated.getEntryDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(updated.getDescription()).isEqualTo("Korrigiert");

        List<EventLog> events = eventLogRepository
                .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("ExpenseEntry", entry.getId());
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("EXPENSE_ENTRY_CREATED");
        assertThat(events.get(1).getEventType()).isEqualTo("EXPENSE_ENTRY_MODIFIED");
        assertThat(events.get(1).getPayload()).contains("\"before\"");
        assertThat(events.get(1).getPayload()).contains("\"after\"");
    }

    @Test
    void update_nonExistentEntry_throwsEntityNotFound() {
        assertThatThrownBy(() -> expenseService.update(999L, user.getId(),
                new BigDecimal("100.00"), "Test", LocalDate.now(), null, null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ExpenseEntry");
    }

    @Test
    void update_otherUsersEntry_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        ExpenseEntry entry = expenseService.create(otherUser, new BigDecimal("100.00"),
                "Test", LocalDate.of(2026, 3, 1), null, null, null);

        assertThatThrownBy(() -> expenseService.update(entry.getId(), user.getId(),
                new BigDecimal("200.00"), "Test", LocalDate.now(), null, null, null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_withOtherUsersAllocationRule_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        AllocationRule otherRule = allocationRuleRepository.save(new AllocationRule(otherUser,
                "Other Rule", (short) 100, (short) 0, (short) 0));

        assertThatThrownBy(() -> expenseService.create(user, new BigDecimal("100.00"),
                "Test", LocalDate.of(2026, 3, 1), otherRule.getId(), null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AllocationRule");
    }

    @Test
    void listAll_returnsOnlyUserEntries() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));

        expenseService.create(user, new BigDecimal("100.00"),
                "Test", LocalDate.of(2026, 3, 1), null, null, null);
        expenseService.create(user, new BigDecimal("200.00"),
                "Test2", LocalDate.of(2026, 3, 15), null, null, null);
        expenseService.create(otherUser, new BigDecimal("300.00"),
                "Other", LocalDate.of(2026, 3, 1), null, null, null);

        List<ExpenseEntry> entries = expenseService.listAll(user.getId());
        assertThat(entries).hasSize(2);
    }

    @Test
    void delete_removesEntry() {
        ExpenseEntry entry = expenseService.create(user, new BigDecimal("100.00"),
                "Test", LocalDate.of(2026, 3, 1), null, null, null);

        expenseService.delete(entry.getId(), user.getId());

        assertThat(expenseEntryRepository.findById(entry.getId())).isEmpty();
    }

    @Test
    void delete_otherUsersEntry_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        ExpenseEntry entry = expenseService.create(otherUser, new BigDecimal("100.00"),
                "Test", LocalDate.of(2026, 3, 1), null, null, null);

        assertThatThrownBy(() -> expenseService.delete(entry.getId(), user.getId()))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void isGwg_returnsCorrectResults() {
        assertThat(expenseService.isGwg(new BigDecimal("799.00"))).isTrue();
        assertThat(expenseService.isGwg(new BigDecimal("800.00"))).isTrue();
        assertThat(expenseService.isGwg(new BigDecimal("800.01"))).isFalse();
        assertThat(expenseService.isGwg(new BigDecimal("1500.00"))).isFalse();
    }
}
