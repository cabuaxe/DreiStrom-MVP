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
import de.dreistrom.vat.dto.VatSummary;
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
class VatCalculationServiceTest {

    @Autowired private VatCalculationService vatService;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private ExpenseEntryRepository expenseEntryRepository;
    @Autowired private AllocationRuleRepository allocationRuleRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;
    private Client freiberufClient;
    private Client gewerbeClient;

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "vat@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "VAT Tester"));

        freiberufClient = clientRepository.save(
                new Client(user, "Freiberuf Kunde", IncomeStream.FREIBERUF));
        gewerbeClient = clientRepository.save(
                new Client(user, "Gewerbe Kunde", IncomeStream.GEWERBE));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("vat@dreistrom.de", null, List.of()));
    }

    private Invoice createInvoice(InvoiceStream stream, Client client,
                                  BigDecimal net, BigDecimal vat, BigDecimal gross,
                                  LocalDate date) {
        return invoiceRepository.save(new Invoice(
                user, stream, "INV-" + System.nanoTime(), client, date,
                List.of(new LineItem("Service", BigDecimal.ONE, net, new BigDecimal("19"))),
                net, vat, gross, VatTreatment.REGULAR));
    }

    private Invoice createInvoice(InvoiceStream stream, Client client,
                                  BigDecimal net, BigDecimal vat, BigDecimal gross,
                                  LocalDate date, VatTreatment treatment) {
        return invoiceRepository.save(new Invoice(
                user, stream, "INV-" + System.nanoTime(), client, date,
                List.of(new LineItem("Service", BigDecimal.ONE, net, BigDecimal.ZERO)),
                net, vat, gross, treatment));
    }

    // ── Output VAT ────────────────────────────────────────────────────

    @Nested
    class OutputVat {

        @Test
        void sumsVatFromFreiberufInvoices() {
            // Net €1000, VAT €190, Gross €1190
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 2, 1));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.freiberufOutputVat()).isEqualByComparingTo("190.00");
            assertThat(result.gewerbeOutputVat()).isEqualByComparingTo("0.00");
            assertThat(result.outputVat()).isEqualByComparingTo("190.00");
        }

        @Test
        void sumsBothStreams() {
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));
            createInvoice(InvoiceStream.GEWERBE, gewerbeClient,
                    new BigDecimal("2000.00"), new BigDecimal("380.00"),
                    new BigDecimal("2380.00"), LocalDate.of(2026, 2, 15));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.freiberufOutputVat()).isEqualByComparingTo("190.00");
            assertThat(result.gewerbeOutputVat()).isEqualByComparingTo("380.00");
            assertThat(result.outputVat()).isEqualByComparingTo("570.00");
        }

        @Test
        void excludesCancelledInvoices() {
            Invoice inv = createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 2, 1));
            inv.updateStatus(InvoiceStatus.CANCELLED);

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.outputVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void excludesInvoicesOutsidePeriod() {
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 5, 1));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.outputVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void reverseChargeInvoicesHaveZeroVat() {
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), BigDecimal.ZERO,
                    new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1),
                    VatTreatment.REVERSE_CHARGE);

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.outputVat()).isEqualByComparingTo("0.00");
        }
    }

    // ── Input VAT (Vorsteuer) ─────────────────────────────────────────

    @Nested
    class InputVat {

        @Test
        void calculatesVorsteuerFromAllocatedExpenses() {
            // Allocation: 60% Freiberuf, 40% Gewerbe
            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 60, (short) 40, (short) 0));

            // Gross expense €1190 → allocated: Freiberuf €714, Gewerbe €476
            // Vorsteuer: Freiberuf 714*19/119 = €114.00, Gewerbe 476*19/119 = €76.00
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("1190.00"), "Office", LocalDate.of(2026, 2, 1),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.freiberufInputVat()).isEqualByComparingTo("114.00");
            assertThat(result.gewerbeInputVat()).isEqualByComparingTo("76.00");
            assertThat(result.inputVat()).isEqualByComparingTo("190.00");
        }

        @Test
        void personalAllocationNotDeductible() {
            // 100% personal → no input VAT deduction
            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Personal", (short) 0, (short) 0, (short) 100));

            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("1190.00"), "Personal", LocalDate.of(2026, 2, 1),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.inputVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void unallocatedExpensesNotIncluded() {
            // No allocation rule → not included in stream-specific input VAT
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("1190.00"), "Misc", LocalDate.of(2026, 2, 1)));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.inputVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void excludesExpensesOutsidePeriod() {
            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));

            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("1190.00"), "Office", LocalDate.of(2026, 5, 1),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.inputVat()).isEqualByComparingTo("0.00");
        }
    }

    // ── Net Payable ───────────────────────────────────────────────────

    @Nested
    class NetPayable {

        @Test
        void positiveWhenOutputExceedsInput() {
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("5000.00"), new BigDecimal("950.00"),
                    new BigDecimal("5950.00"), LocalDate.of(2026, 2, 1));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("595.00"), "Office", LocalDate.of(2026, 2, 15),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            // Output: 950, Input: 595 * 19/119 = 95.00
            // Net payable: 950 - 95 = 855.00
            assertThat(result.outputVat()).isEqualByComparingTo("950.00");
            assertThat(result.inputVat()).isEqualByComparingTo("95.00");
            assertThat(result.netPayable()).isEqualByComparingTo("855.00");
            assertThat(result.netPayable().signum()).isPositive();
        }

        @Test
        void negativeWhenInputExceedsOutput_meansRefund() {
            // Small invoice, large expense
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("100.00"), new BigDecimal("19.00"),
                    new BigDecimal("119.00"), LocalDate.of(2026, 2, 1));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Equipment", (short) 100, (short) 0, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("11900.00"), "Equipment", LocalDate.of(2026, 2, 15),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            // Output: 19, Input: 11900 * 19/119 = 1900
            // Net payable: 19 - 1900 = -1881 (refund)
            assertThat(result.netPayable()).isNegative();
        }
    }

    // ── Kleinunternehmer ──────────────────────────────────────────────

    @Nested
    class Kleinunternehmer {

        @Test
        void zeroesAllVat() {
            createInvoice(InvoiceStream.FREIBERUF, freiberufClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 2, 1));

            AllocationRule rule = allocationRuleRepository.save(
                    new AllocationRule(user, "Office", (short) 100, (short) 0, (short) 0));
            expenseEntryRepository.save(new ExpenseEntry(
                    user, new BigDecimal("1190.00"), "Office", LocalDate.of(2026, 2, 1),
                    rule, null, null));

            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, true);

            assertThat(result.outputVat()).isEqualByComparingTo("0.00");
            assertThat(result.inputVat()).isEqualByComparingTo("0.00");
            assertThat(result.netPayable()).isEqualByComparingTo("0.00");
            assertThat(result.kleinunternehmer()).isTrue();
        }
    }

    // ── Rate calculations ─────────────────────────────────────────────

    @Nested
    class RateCalculations {

        @Test
        void extractVat_standardRate() {
            // Gross €119 at 19% → VAT = 119 * 19/119 = €19.00
            BigDecimal vat = vatService.extractVat(
                    new BigDecimal("119.00"), VatCalculationService.STANDARD_RATE);
            assertThat(vat).isEqualByComparingTo("19.00");
        }

        @Test
        void extractVat_reducedRate() {
            // Gross €107 at 7% → VAT = 107 * 7/107 = €7.00
            BigDecimal vat = vatService.extractVat(
                    new BigDecimal("107.00"), VatCalculationService.REDUCED_RATE);
            assertThat(vat).isEqualByComparingTo("7.00");
        }

        @Test
        void extractVat_zeroRate() {
            BigDecimal vat = vatService.extractVat(
                    new BigDecimal("1000.00"), BigDecimal.ZERO);
            assertThat(vat).isEqualByComparingTo("0.00");
        }

        @Test
        void netFromGross_standardRate() {
            // Gross €119 at 19% → Net = 119 * 100/119 = €100.00
            BigDecimal net = vatService.netFromGross(
                    new BigDecimal("119.00"), VatCalculationService.STANDARD_RATE);
            assertThat(net).isEqualByComparingTo("100.00");
        }

        @Test
        void grossFromNet_standardRate() {
            // Net €100 at 19% → Gross = 100 * 119/100 = €119.00
            BigDecimal gross = vatService.grossFromNet(
                    new BigDecimal("100.00"), VatCalculationService.STANDARD_RATE);
            assertThat(gross).isEqualByComparingTo("119.00");
        }

        @Test
        void nullAndZeroInputsReturnZero() {
            assertThat(vatService.extractVat(null, VatCalculationService.STANDARD_RATE))
                    .isEqualByComparingTo("0.00");
            assertThat(vatService.extractVat(BigDecimal.ZERO, VatCalculationService.STANDARD_RATE))
                    .isEqualByComparingTo("0.00");
            assertThat(vatService.netFromGross(null, VatCalculationService.STANDARD_RATE))
                    .isEqualByComparingTo("0.00");
            assertThat(vatService.grossFromNet(null, VatCalculationService.STANDARD_RATE))
                    .isEqualByComparingTo("0.00");
        }
    }

    // ── Empty data ────────────────────────────────────────────────────

    @Nested
    class EmptyData {

        @Test
        void returnsZerosWhenNoData() {
            VatSummary result = vatService.calculate(
                    user.getId(), PERIOD_START, PERIOD_END, false);

            assertThat(result.outputVat()).isEqualByComparingTo("0.00");
            assertThat(result.inputVat()).isEqualByComparingTo("0.00");
            assertThat(result.netPayable()).isEqualByComparingTo("0.00");
            assertThat(result.kleinunternehmer()).isFalse();
        }
    }
}
