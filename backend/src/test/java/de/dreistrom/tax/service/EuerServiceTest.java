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
import de.dreistrom.tax.dto.EuerResult;
import de.dreistrom.tax.service.EuerService.DualStreamEuer;
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
class EuerServiceTest {

    @Autowired private EuerService euerService;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        incomeEntryRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "euer@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "EÜR Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("euer@dreistrom.de", null, List.of()));
    }

    @Nested
    class FreiberufEuer {

        @Test
        void emptyData_zeroProfit() {
            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            assertThat(result.stream()).isEqualTo(IncomeStream.FREIBERUF);
            assertThat(result.taxYear()).isEqualTo(2024);
            assertThat(result.totalIncome()).isEqualByComparingTo("0");
            assertThat(result.totalExpenses()).isEqualByComparingTo("0");
            assertThat(result.profit()).isEqualByComparingTo("0");
        }

        @Test
        void incomeOnlyWithoutExpenses() {
            Client client = clientRepository.save(
                    new Client(user, "IT Beratung", IncomeStream.FREIBERUF));
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("60000"),
                    LocalDate.of(2024, 6, 15), "Beratung", client, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            assertThat(result.totalIncome()).isEqualByComparingTo("60000");
            assertThat(result.totalExpenses()).isEqualByComparingTo("0");
            assertThat(result.profit()).isEqualByComparingTo("60000");
        }

        @Test
        void incomeAndExpenses() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("80000"),
                    LocalDate.of(2024, 6, 15), "Project", null, null));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("15000"), "Office", LocalDate.of(2024, 3, 1),
                    rule, null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            assertThat(result.totalIncome()).isEqualByComparingTo("80000");
            assertThat(result.directExpenses()).isEqualByComparingTo("15000");
            assertThat(result.profit()).isEqualByComparingTo("65000");
        }

        @Test
        void sharedExpensesAllocatedByRatio() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                    LocalDate.of(2024, 6, 15), "Consulting", null, null));

            // 60% Freiberuf, 40% Gewerbe allocation
            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Shared Office", (short) 60, (short) 40, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("10000"), "Office", LocalDate.of(2024, 4, 1),
                    rule, null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            // 60% of 10000 = 6000 allocated to Freiberuf
            assertThat(result.directExpenses()).isEqualByComparingTo("6000");
            assertThat(result.profit()).isEqualByComparingTo("44000");
        }

        @Test
        void excludesGewerbeIncome() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("30000"),
                    LocalDate.of(2024, 3, 15), "Consulting", null, null));
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.GEWERBE, new BigDecimal("20000"),
                    LocalDate.of(2024, 6, 15), "Onlineshop", null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            // Only Freiberuf income should appear
            assertThat(result.totalIncome()).isEqualByComparingTo("30000");
        }
    }

    @Nested
    class GewerbeEuer {

        @Test
        void gewerbeIncomeAndExpenses() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.GEWERBE, new BigDecimal("45000"),
                    LocalDate.of(2024, 8, 15), "Onlineshop", null, null));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Warehouse", (short) 0, (short) 100, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("8000"), "Warehouse", LocalDate.of(2024, 5, 1),
                    rule, null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.GEWERBE, 2024);

            assertThat(result.stream()).isEqualTo(IncomeStream.GEWERBE);
            assertThat(result.totalIncome()).isEqualByComparingTo("45000");
            assertThat(result.directExpenses()).isEqualByComparingTo("8000");
            assertThat(result.profit()).isEqualByComparingTo("37000");
        }

        @Test
        void lossWhenExpensesExceedIncome() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.GEWERBE, new BigDecimal("5000"),
                    LocalDate.of(2024, 6, 15), "Sales", null, null));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Equipment", (short) 0, (short) 100, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("15000"), "Equipment", LocalDate.of(2024, 3, 1),
                    rule, null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.GEWERBE, 2024);

            assertThat(result.profit()).isNegative();
        }
    }

    @Nested
    class DualStream {

        @Test
        void generatesBothStreams() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("60000"),
                    LocalDate.of(2024, 6, 15), "Beratung", null, null));
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.GEWERBE, new BigDecimal("40000"),
                    LocalDate.of(2024, 8, 15), "Verkauf", null, null));

            DualStreamEuer dual = euerService.generateDual(user.getId(), 2024);

            assertThat(dual.freiberuf().stream()).isEqualTo(IncomeStream.FREIBERUF);
            assertThat(dual.freiberuf().totalIncome()).isEqualByComparingTo("60000");
            assertThat(dual.gewerbe().stream()).isEqualTo(IncomeStream.GEWERBE);
            assertThat(dual.gewerbe().totalIncome()).isEqualByComparingTo("40000");
            assertThat(dual.combinedProfit()).isEqualByComparingTo("100000");
        }
    }

    @Nested
    class Validation {

        @Test
        void rejectsEmploymentStream() {
            assertThatThrownBy(() ->
                    euerService.generate(user.getId(), IncomeStream.EMPLOYMENT, 2024))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("EMPLOYMENT");
        }
    }

    @Nested
    class YearIsolation {

        @Test
        void excludesIncomeFromOtherYears() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                    LocalDate.of(2023, 6, 15), "Prior year", null, null));
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("20000"),
                    LocalDate.of(2024, 3, 15), "This year", null, null));

            EuerResult result = euerService.generate(
                    user.getId(), IncomeStream.FREIBERUF, 2024);

            assertThat(result.totalIncome()).isEqualByComparingTo("20000");
        }
    }
}
