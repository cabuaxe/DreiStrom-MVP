package de.dreistrom.expense.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
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
class ExpenseEntryTest {

    @Autowired
    private ExpenseEntryRepository expenseEntryRepository;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "expense@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Expense Tester"));
    }

    @Test
    void persist_andRetrieve_expenseEntry() {
        ExpenseEntry entry = new ExpenseEntry(user, new BigDecimal("250.00"),
                "Büromaterial", LocalDate.of(2026, 3, 15));
        ExpenseEntry saved = expenseEntryRepository.save(entry);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo("250.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getCategory()).isEqualTo("Büromaterial");
        assertThat(saved.getEntryDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persist_withAllocationRule_andDescription() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Office Split", (short) 50, (short) 30, (short) 20));

        ExpenseEntry entry = new ExpenseEntry(user, new BigDecimal("1200.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule, null, "Büro-Anteil Miete März");
        ExpenseEntry saved = expenseEntryRepository.save(entry);

        assertThat(saved.getAllocationRule().getId()).isEqualTo(rule.getId());
        assertThat(saved.getDescription()).isEqualTo("Büro-Anteil Miete März");
    }

    @Test
    void moneyConverter_storesCents_andRestoresEuros() {
        ExpenseEntry entry = new ExpenseEntry(user, new BigDecimal("1234.56"),
                "Telefon", LocalDate.of(2026, 1, 31));
        ExpenseEntry saved = expenseEntryRepository.save(entry);
        expenseEntryRepository.flush();

        ExpenseEntry fetched = expenseEntryRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getAmount()).isEqualByComparingTo("1234.56");
    }

    @Test
    void findByUserIdAndCategory_filtersCorrectly() {
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("100.00"),
                "Büromaterial", LocalDate.of(2026, 3, 1)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("200.00"),
                "Reisekosten", LocalDate.of(2026, 3, 1)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("300.00"),
                "Büromaterial", LocalDate.of(2026, 3, 15)));

        List<ExpenseEntry> office = expenseEntryRepository
                .findByUserIdAndCategory(user.getId(), "Büromaterial");
        assertThat(office).hasSize(2);
    }

    @Test
    void findByUserIdAndEntryDateBetween_filtersDateRange() {
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("100.00"),
                "Telefon", LocalDate.of(2026, 1, 15)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("200.00"),
                "Internet", LocalDate.of(2026, 2, 15)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("300.00"),
                "Strom", LocalDate.of(2026, 3, 15)));

        List<ExpenseEntry> feb = expenseEntryRepository.findByUserIdAndEntryDateBetween(
                user.getId(), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(feb).hasSize(1);
    }

    @Test
    void findByUserIdAndCategoryAndEntryDateBetween_combinesFilters() {
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("100.00"),
                "Telefon", LocalDate.of(2026, 3, 1)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("200.00"),
                "Telefon", LocalDate.of(2026, 4, 1)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("300.00"),
                "Internet", LocalDate.of(2026, 3, 15)));

        List<ExpenseEntry> marchTelefon = expenseEntryRepository
                .findByUserIdAndCategoryAndEntryDateBetween(
                        user.getId(), "Telefon",
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(marchTelefon).hasSize(1);
        assertThat(marchTelefon.getFirst().getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void sumCentsFreiberufByDateRange_returnsCorrectTotal() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Split 60/30/10", (short) 60, (short) 30, (short) 10));

        // 1000.00 EUR = 100000 cents; Freiberuf share = 100000 * 60 / 100 = 60000
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("1000.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule, null, null));
        // 500.00 EUR = 50000 cents; Freiberuf share = 50000 * 60 / 100 = 30000
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("500.00"),
                "Internet", LocalDate.of(2026, 3, 15), rule, null, null));
        expenseEntryRepository.flush();

        Long total = expenseEntryRepository.sumCentsFreiberufByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // 60000 + 30000 = 90000 cents
        assertThat(total).isEqualTo(90000L);
    }

    @Test
    void sumCentsGewerbeByDateRange_returnsCorrectTotal() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Split 60/30/10", (short) 60, (short) 30, (short) 10));

        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("1000.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule, null, null));
        expenseEntryRepository.flush();

        Long total = expenseEntryRepository.sumCentsGewerbeByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // 100000 * 30 / 100 = 30000
        assertThat(total).isEqualTo(30000L);
    }

    @Test
    void sumCentsPersonalByDateRange_returnsCorrectTotal() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Split 60/30/10", (short) 60, (short) 30, (short) 10));

        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("1000.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule, null, null));
        expenseEntryRepository.flush();

        Long total = expenseEntryRepository.sumCentsPersonalByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // 100000 * 10 / 100 = 10000
        assertThat(total).isEqualTo(10000L);
    }

    @Test
    void sumCentsByDateRange_returnsTotalGross() {
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("1000.00"),
                "Miete", LocalDate.of(2026, 3, 1)));
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("500.50"),
                "Internet", LocalDate.of(2026, 6, 15)));
        expenseEntryRepository.flush();

        Long total = expenseEntryRepository.sumCentsByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // 1000.00 + 500.50 = 1500.50 EUR = 150050 cents
        assertThat(total).isEqualTo(150050L);
    }

    @Test
    void sumCentsFreiberuf_excludesUnallocatedExpenses() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Full Freiberuf", (short) 100, (short) 0, (short) 0));

        // Allocated expense
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("1000.00"),
                "Miete", LocalDate.of(2026, 3, 1), rule, null, null));
        // Unallocated expense (no rule) — should be excluded from stream sum
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("500.00"),
                "Sonstiges", LocalDate.of(2026, 3, 15)));
        expenseEntryRepository.flush();

        Long freiberuf = expenseEntryRepository.sumCentsFreiberufByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // Only the allocated 1000.00 at 100% = 100000 cents
        assertThat(freiberuf).isEqualTo(100000L);

        // But gross total includes both
        Long gross = expenseEntryRepository.sumCentsByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(gross).isEqualTo(150000L);
    }

    @Test
    void sumCentsFreiberuf_returnsNull_whenNoAllocatedEntries() {
        // Only unallocated expenses
        expenseEntryRepository.save(new ExpenseEntry(user, new BigDecimal("500.00"),
                "Sonstiges", LocalDate.of(2026, 3, 15)));
        expenseEntryRepository.flush();

        Long total = expenseEntryRepository.sumCentsFreiberufByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(total).isNull();
    }

    @Test
    void sumCentsByDateRange_returnsNull_whenNoEntries() {
        Long total = expenseEntryRepository.sumCentsByDateRange(
                user.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(total).isNull();
    }
}
