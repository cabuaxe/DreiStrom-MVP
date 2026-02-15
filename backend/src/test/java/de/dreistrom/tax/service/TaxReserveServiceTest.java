package de.dreistrom.tax.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.dto.TaxReserveRecommendation;
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

@SpringBootTest
@Transactional
class TaxReserveServiceTest {

    @Autowired private TaxReserveService taxReserveService;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        incomeEntryRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "reserve@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Reserve Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("reserve@dreistrom.de", null, List.of()));
    }

    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    @Nested
    class BasicCalculation {

        @Test
        void zeroIncomeReturnsZeroReserve() {
            TaxReserveRecommendation result = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, BigDecimal.ZERO);

            assertThat(result.netSelfEmployedProfit()).isEqualByComparingTo("0");
            assertThat(result.monthlyReserve()).isEqualByComparingTo("0.00");
            assertThat(result.annualReserve()).isEqualByComparingTo("0.00");
        }

        @Test
        void calculatesReserveFromFreiberufIncome() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("30000"),
                    LocalDate.of(CURRENT_YEAR, 1, 15), "Consulting", null, null));

            TaxReserveRecommendation result = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, BigDecimal.ZERO);

            assertThat(result.netSelfEmployedProfit()).isEqualByComparingTo("30000");
            assertThat(result.reserveRatePercent()).isEqualByComparingTo("30");
            assertThat(result.annualReserve().signum()).isPositive();
            assertThat(result.monthlyReserve().signum()).isPositive();
        }

        @Test
        void deductsBusinessExpenses() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("50000"),
                    LocalDate.of(CURRENT_YEAR, 1, 15), "Project", null, null));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("10000"), "Office", LocalDate.of(CURRENT_YEAR, 1, 10),
                    rule, null, null));

            TaxReserveRecommendation result = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, BigDecimal.ZERO);

            // Net profit = 50000 - 10000 = 40000
            assertThat(result.netSelfEmployedProfit()).isEqualByComparingTo("40000");
        }

        @Test
        void excludesEmploymentIncome() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.EMPLOYMENT, new BigDecimal("50000"),
                    LocalDate.of(CURRENT_YEAR, 1, 30), "Salary", null, null));

            TaxReserveRecommendation result = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, BigDecimal.ZERO);

            // Employment income is not self-employed → no reserve needed
            assertThat(result.netSelfEmployedProfit()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class AlreadyReserved {

        @Test
        void reducesRemainingAmount() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("60000"),
                    LocalDate.of(CURRENT_YEAR, 1, 15), "Consulting", null, null));

            TaxReserveRecommendation withoutReserve = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, BigDecimal.ZERO);
            TaxReserveRecommendation withReserve = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, new BigDecimal("5000"));

            assertThat(withReserve.remainingToReserve())
                    .isLessThan(withoutReserve.remainingToReserve());
            assertThat(withReserve.alreadyReserved()).isEqualByComparingTo("5000");
        }

        @Test
        void overReservedResultsInZeroMonthly() {
            incomeEntryRepository.save(new IncomeEntry(
                    user, IncomeStream.FREIBERUF, new BigDecimal("10000"),
                    LocalDate.of(CURRENT_YEAR, 1, 15), "Small project", null, null));

            TaxReserveRecommendation result = taxReserveService.calculate(
                    user.getId(), CURRENT_YEAR, new BigDecimal("999999"));

            assertThat(result.remainingToReserve()).isEqualByComparingTo("0.00");
            assertThat(result.monthlyReserve()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    class CustomRate {

        @Test
        void computeWithCustomRate25Percent() {
            TaxReserveRecommendation result = taxReserveService.computeWithRate(
                    new BigDecimal("60000"), new BigDecimal("25"),
                    BigDecimal.ZERO, CURRENT_YEAR);

            // Annual = 60000 * 25% (but projected, depends on current date)
            assertThat(result.reserveRatePercent()).isEqualByComparingTo("25");
            assertThat(result.annualReserve().signum()).isPositive();
        }

        @Test
        void computeWithCustomRate35Percent() {
            TaxReserveRecommendation result = taxReserveService.computeWithRate(
                    new BigDecimal("60000"), new BigDecimal("35"),
                    BigDecimal.ZERO, CURRENT_YEAR);

            TaxReserveRecommendation result25 = taxReserveService.computeWithRate(
                    new BigDecimal("60000"), new BigDecimal("25"),
                    BigDecimal.ZERO, CURRENT_YEAR);

            // 35% rate should yield higher reserve than 25%
            assertThat(result.annualReserve()).isGreaterThan(result25.annualReserve());
        }
    }
}
