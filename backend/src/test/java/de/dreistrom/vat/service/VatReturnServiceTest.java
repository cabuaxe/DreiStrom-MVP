package de.dreistrom.vat.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.invoicing.domain.*;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.domain.VatReturnStatus;
import de.dreistrom.vat.repository.VatReturnRepository;
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
class VatReturnServiceTest {

    @Autowired private VatReturnService vatReturnService;
    @Autowired private VatReturnRepository vatReturnRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;
    private Client freiberufClient;

    @BeforeEach
    void setUp() {
        vatReturnRepository.deleteAll();
        invoiceRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "vat-return@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "VAT Return Tester"));

        freiberufClient = clientRepository.save(
                new Client(user, "Test Client", IncomeStream.FREIBERUF));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vat-return@dreistrom.de", null, List.of()));
    }

    private void createInvoice(BigDecimal net, BigDecimal vat, BigDecimal gross, LocalDate date) {
        invoiceRepository.save(new Invoice(
                user, InvoiceStream.FREIBERUF, "INV-" + System.nanoTime(), freiberufClient,
                date, List.of(new LineItem("Service", BigDecimal.ONE, net, new BigDecimal("19"))),
                net, vat, gross, VatTreatment.REGULAR));
    }

    private void createExpense(BigDecimal amount, LocalDate date, AllocationRule rule) {
        expenseEntryRepository.save(new ExpenseEntry(
                user, amount, "Office", date, rule, null, null));
    }

    // ── Period generation ─────────────────────────────────────────────

    @Nested
    class PeriodGeneration {

        @Test
        void generatesMonthlyReturn() {
            // Invoice in January: net €1000, VAT €190
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            assertThat(result.getYear()).isEqualTo((short) 2026);
            assertThat(result.getPeriodType()).isEqualTo(PeriodType.MONTHLY);
            assertThat(result.getPeriodNumber()).isEqualTo((short) 1);
            assertThat(result.getOutputVat()).isEqualByComparingTo("190.00");
            assertThat(result.getStatus()).isEqualTo(VatReturnStatus.DRAFT);
            assertThat(result.getId()).isNotNull();
        }

        @Test
        void generatesQuarterlyReturn() {
            // Invoices in Q1 (Jan-Mar)
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));
            createInvoice(new BigDecimal("2000.00"), new BigDecimal("380.00"),
                    new BigDecimal("2380.00"), LocalDate.of(2026, 3, 20));

            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.QUARTERLY, 1, false);

            assertThat(result.getOutputVat()).isEqualByComparingTo("570.00");
        }

        @Test
        void generatesAnnualReturn() {
            createInvoice(new BigDecimal("5000.00"), new BigDecimal("950.00"),
                    new BigDecimal("5950.00"), LocalDate.of(2026, 6, 1));

            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.ANNUAL, 1, false);

            assertThat(result.getPeriodType()).isEqualTo(PeriodType.ANNUAL);
            assertThat(result.getOutputVat()).isEqualByComparingTo("950.00");
        }

        @Test
        void excludesInvoicesFromOtherPeriods() {
            // Invoice in February
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 2, 15));

            // Generate January return
            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            assertThat(result.getOutputVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void includesInputVatFromExpenses() {
            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));

            // Invoice: VAT €190
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));
            // Expense: gross €595, Vorsteuer = 595*19/119 = €95
            createExpense(new BigDecimal("595.00"), LocalDate.of(2026, 1, 20), rule);

            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            assertThat(result.getOutputVat()).isEqualByComparingTo("190.00");
            assertThat(result.getInputVat()).isEqualByComparingTo("95.00");
            assertThat(result.getNetPayable()).isEqualByComparingTo("95.00");
        }
    }

    // ── Generate for year ─────────────────────────────────────────────

    @Nested
    class GenerateForYear {

        @Test
        void generates12MonthlyReturns() {
            List<VatReturn> returns = vatReturnService.generateForYear(
                    user, 2026, PeriodType.MONTHLY, false);

            assertThat(returns).hasSize(12);
            assertThat(returns.get(0).getPeriodNumber()).isEqualTo((short) 1);
            assertThat(returns.get(11).getPeriodNumber()).isEqualTo((short) 12);
        }

        @Test
        void generates4QuarterlyReturns() {
            List<VatReturn> returns = vatReturnService.generateForYear(
                    user, 2026, PeriodType.QUARTERLY, false);

            assertThat(returns).hasSize(4);
        }

        @Test
        void generates1AnnualReturn() {
            List<VatReturn> returns = vatReturnService.generateForYear(
                    user, 2026, PeriodType.ANNUAL, false);

            assertThat(returns).hasSize(1);
        }
    }

    // ── Update / Regenerate ───────────────────────────────────────────

    @Nested
    class UpdateRegenerate {

        @Test
        void regeneratesExistingDraftReturn() {
            // First generation
            vatReturnService.generateForPeriod(user, 2026, PeriodType.MONTHLY, 1, false);

            // Add invoice
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            // Regenerate
            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            assertThat(result.getOutputVat()).isEqualByComparingTo("190.00");
        }

        @Test
        void cannotRegenerateSubmittedReturn() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);
            vatReturnService.submit(vr.getId(), user.getId(), LocalDate.of(2026, 2, 10));

            assertThatThrownBy(() -> vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SUBMITTED");
        }
    }

    // ── Status tracking ───────────────────────────────────────────────

    @Nested
    class StatusTracking {

        @Test
        void submitSetsStatusAndDate() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            VatReturn submitted = vatReturnService.submit(
                    vr.getId(), user.getId(), LocalDate.of(2026, 2, 10));

            assertThat(submitted.getStatus()).isEqualTo(VatReturnStatus.SUBMITTED);
            assertThat(submitted.getSubmissionDate()).isEqualTo(LocalDate.of(2026, 2, 10));
        }

        @Test
        void cannotSubmitNonDraft() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);
            vatReturnService.submit(vr.getId(), user.getId(), LocalDate.of(2026, 2, 10));

            assertThatThrownBy(() -> vatReturnService.submit(
                    vr.getId(), user.getId(), LocalDate.of(2026, 2, 15)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void updateStatusWorks() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);
            vatReturnService.submit(vr.getId(), user.getId(), LocalDate.of(2026, 2, 10));

            VatReturn accepted = vatReturnService.updateStatus(
                    vr.getId(), user.getId(), VatReturnStatus.ACCEPTED);

            assertThat(accepted.getStatus()).isEqualTo(VatReturnStatus.ACCEPTED);
        }
    }

    // ── Kleinunternehmer ──────────────────────────────────────────────

    @Nested
    class Kleinunternehmer {

        @Test
        void zeroesAllAmounts() {
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            VatReturn result = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, true);

            assertThat(result.getOutputVat()).isEqualByComparingTo("0.00");
            assertThat(result.getInputVat()).isEqualByComparingTo("0.00");
            assertThat(result.getNetPayable()).isEqualByComparingTo("0.00");
        }
    }

    // ── Period helpers ────────────────────────────────────────────────

    @Nested
    class PeriodHelpers {

        @Test
        void monthlyPeriodBoundaries() {
            assertThat(VatReturnService.periodStart(2026, PeriodType.MONTHLY, 1))
                    .isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(VatReturnService.periodEnd(2026, PeriodType.MONTHLY, 1))
                    .isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(VatReturnService.periodEnd(2026, PeriodType.MONTHLY, 2))
                    .isEqualTo(LocalDate.of(2026, 2, 28));
        }

        @Test
        void quarterlyPeriodBoundaries() {
            assertThat(VatReturnService.periodStart(2026, PeriodType.QUARTERLY, 1))
                    .isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(VatReturnService.periodEnd(2026, PeriodType.QUARTERLY, 1))
                    .isEqualTo(LocalDate.of(2026, 3, 31));
            assertThat(VatReturnService.periodStart(2026, PeriodType.QUARTERLY, 2))
                    .isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(VatReturnService.periodEnd(2026, PeriodType.QUARTERLY, 4))
                    .isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        void annualPeriodBoundaries() {
            assertThat(VatReturnService.periodStart(2026, PeriodType.ANNUAL, 1))
                    .isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(VatReturnService.periodEnd(2026, PeriodType.ANNUAL, 1))
                    .isEqualTo(LocalDate.of(2026, 12, 31));
        }
    }

    // ── List / Get ────────────────────────────────────────────────────

    @Nested
    class ListAndGet {

        @Test
        void listByYear() {
            vatReturnService.generateForYear(user, 2026, PeriodType.QUARTERLY, false);

            List<VatReturn> results = vatReturnService.listByYear(user.getId(), 2026);
            assertThat(results).hasSize(4);
        }

        @Test
        void getByIdValidatesOwnership() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            assertThat(vatReturnService.getById(vr.getId(), user.getId())).isNotNull();

            assertThatThrownBy(() -> vatReturnService.getById(vr.getId(), 99999L))
                    .isInstanceOf(de.dreistrom.common.controller.EntityNotFoundException.class);
        }
    }
}
