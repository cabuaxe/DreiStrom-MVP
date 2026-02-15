package de.dreistrom.tax.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.dto.TaxCalculationResult;
import de.dreistrom.tax.event.TaxCalculated;
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
class TaxAssessmentServiceTest {

    @Autowired private TaxAssessmentService taxAssessmentService;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ApplicationEvents events;

    private AppUser user;

    @BeforeEach
    void setUp() {
        incomeEntryRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "tax@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Tax Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tax@dreistrom.de", null, List.of()));
    }

    @Test
    void assessesWithNoData() {
        TaxCalculationResult result = taxAssessmentService.assess(user.getId(), 2024);

        assertThat(result.totalGrossIncome()).isEqualByComparingTo("0");
        assertThat(result.taxableIncome()).isEqualByComparingTo("0");
        assertThat(result.incomeTax()).isEqualByComparingTo("0.00");
        assertThat(result.totalTax()).isEqualByComparingTo("0.00");
    }

    @Test
    void assessesFreiberufIncomeWithExpenses() {
        Client client = clientRepository.save(
                new Client(user, "IT Kunde", IncomeStream.FREIBERUF));

        // Freiberuf income: 3 entries totalling €60,000
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("20000"),
                LocalDate.of(2024, 3, 15), "Project A", client, null));
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("25000"),
                LocalDate.of(2024, 6, 15), "Project B", client, null));
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("15000"),
                LocalDate.of(2024, 9, 15), "Project C", client, null));

        // Business expenses: €12,000 allocated 100% Freiberuf
        AllocationRule rule = allocationRuleRepository.save(
                new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));
        expenseEntryRepository.save(new ExpenseEntry(
                user, new BigDecimal("12000"), "Office", LocalDate.of(2024, 6, 1),
                rule, null, null));

        TaxCalculationResult result = taxAssessmentService.assess(user.getId(), 2024);

        assertThat(result.totalGrossIncome()).isEqualByComparingTo("60000");
        assertThat(result.freiberufIncome()).isEqualByComparingTo("60000");
        assertThat(result.deductions().businessExpensesFreiberuf()).isEqualByComparingTo("12000");
        assertThat(result.taxableIncome().signum()).isPositive();
        assertThat(result.incomeTax().signum()).isPositive();
    }

    @Test
    void assessesMultiStreamIncome() {
        Client freiberufClient = clientRepository.save(
                new Client(user, "IT Beratung", IncomeStream.FREIBERUF));
        Client gewerbeClient = clientRepository.save(
                new Client(user, "Onlineshop", IncomeStream.GEWERBE));

        // Employment: €45,000
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.EMPLOYMENT, new BigDecimal("45000"),
                LocalDate.of(2024, 6, 30), "Gehalt", null, null));

        // Freiberuf: €20,000
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("20000"),
                LocalDate.of(2024, 4, 15), "Beratung", freiberufClient, null));

        // Gewerbe: €15,000
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.GEWERBE, new BigDecimal("15000"),
                LocalDate.of(2024, 8, 15), "Verkauf", gewerbeClient, null));

        TaxCalculationResult result = taxAssessmentService.assess(user.getId(), 2024);

        assertThat(result.employmentIncome()).isEqualByComparingTo("45000");
        assertThat(result.freiberufIncome()).isEqualByComparingTo("20000");
        assertThat(result.gewerbeIncome()).isEqualByComparingTo("15000");
        assertThat(result.totalGrossIncome()).isEqualByComparingTo("80000");
        // Employment triggers Werbungskostenpauschale
        assertThat(result.deductions().werbungskostenpauschale()).isEqualByComparingTo("1230");
    }

    @Test
    void publishesTaxCalculatedEvent() {
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                LocalDate.of(2024, 6, 15), "Beratung", null, null));

        taxAssessmentService.assess(user.getId(), 2024);

        assertThat(events.stream(TaxCalculated.class).count()).isEqualTo(1);
        TaxCalculated event = events.stream(TaxCalculated.class).findFirst().orElseThrow();
        assertThat(event.getUserId()).isEqualTo(user.getId());
        assertThat(event.getTaxYear()).isEqualTo(2024);
        assertThat(event.getTotalTax().signum()).isPositive();
    }

    @Test
    void excludesIncomeFromOtherYears() {
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                LocalDate.of(2023, 6, 15), "Prior year", null, null));
        incomeEntryRepository.save(new IncomeEntry(
                user, IncomeStream.FREIBERUF, new BigDecimal("10000"),
                LocalDate.of(2024, 3, 15), "This year", null, null));

        TaxCalculationResult result = taxAssessmentService.assess(user.getId(), 2024);

        assertThat(result.freiberufIncome()).isEqualByComparingTo("10000");
    }
}
