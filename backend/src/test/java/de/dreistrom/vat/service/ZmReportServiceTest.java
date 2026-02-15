package de.dreistrom.vat.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.invoicing.domain.*;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.invoicing.service.InvoiceService;
import de.dreistrom.vat.dto.ZmReport;
import de.dreistrom.vat.dto.ZmReportLine;
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
class ZmReportServiceTest {

    @Autowired private ZmReportService zmReportService;
    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;
    private Client apple;
    private Client google;
    private Client deClient;
    private Client austrian;

    private static final List<LineItem> ZERO_VAT_ITEMS = List.of(
            new LineItem("App Revenue", new BigDecimal("1"), new BigDecimal("5000.00"), BigDecimal.ZERO));

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        incomeEntryRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "zm-test@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "ZM Tester"));

        apple = clientRepository.save(new Client(user, "Apple Distribution International",
                IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE9700053D"));
        google = clientRepository.save(new Client(user, "Google Ireland Ltd",
                IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE6388047V"));
        deClient = clientRepository.save(new Client(user, "DE GmbH", IncomeStream.FREIBERUF));
        austrian = clientRepository.save(new Client(user, "Austrian Corp",
                IncomeStream.FREIBERUF, ClientType.B2B, "AT", "ATU12345678"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("zm-test@dreistrom.de", null, List.of()));
    }

    private Invoice createReverseChargeInvoice(Client client, BigDecimal net, LocalDate date) {
        return invoiceService.create(user, InvoiceStream.FREIBERUF, client.getId(),
                date, null,
                List.of(new LineItem("Service", BigDecimal.ONE, net, BigDecimal.ZERO)),
                net, BigDecimal.ZERO, net,
                VatTreatment.REVERSE_CHARGE, null);
    }

    private Invoice createRegularInvoice(Client client, BigDecimal net, BigDecimal vat,
                                          BigDecimal gross, LocalDate date) {
        return invoiceService.create(user, InvoiceStream.FREIBERUF, client.getId(),
                date, null,
                List.of(new LineItem("Service", BigDecimal.ONE, net, new BigDecimal("19"))),
                net, vat, gross,
                VatTreatment.REGULAR, null);
    }

    // ── App Store Payout Classification ─────────────────────────────────

    @Nested
    class AppStorePayoutClassification {

        @Test
        void applePayoutClassifiedAsReverseCharge() {
            Invoice inv = createReverseChargeInvoice(apple,
                    new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));

            assertThat(inv.getVatTreatment()).isEqualTo(VatTreatment.REVERSE_CHARGE);
            assertThat(inv.isZmReportable()).isTrue();
            assertThat(inv.getVat()).isEqualByComparingTo("0.00");
        }

        @Test
        void googlePayoutClassifiedAsReverseCharge() {
            Invoice inv = createReverseChargeInvoice(google,
                    new BigDecimal("3000.00"), LocalDate.of(2026, 1, 20));

            assertThat(inv.getVatTreatment()).isEqualTo(VatTreatment.REVERSE_CHARGE);
            assertThat(inv.isZmReportable()).isTrue();
        }

        @Test
        void domesticInvoiceNotZmReportable() {
            Invoice inv = createRegularInvoice(deClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 10));

            assertThat(inv.isZmReportable()).isFalse();
        }
    }

    // ── ZM Report Generation ────────────────────────────────────────────

    @Nested
    class ReportGeneration {

        @Test
        void aggregatesByCountryAndUstIdNr() {
            // Two Apple invoices + one Google invoice
            createReverseChargeInvoice(apple, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));
            createReverseChargeInvoice(apple, new BigDecimal("3000.00"), LocalDate.of(2026, 2, 15));
            createReverseChargeInvoice(google, new BigDecimal("2000.00"), LocalDate.of(2026, 1, 20));

            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            assertThat(report.lines()).hasSize(2); // Apple + Google
            assertThat(report.totalInvoices()).isEqualTo(3);
            assertThat(report.totalNet()).isEqualByComparingTo("10000.00");
        }

        @Test
        void appleLinesAggregated() {
            createReverseChargeInvoice(apple, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));
            createReverseChargeInvoice(apple, new BigDecimal("3000.00"), LocalDate.of(2026, 2, 15));

            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            ZmReportLine appleLine = report.lines().stream()
                    .filter(l -> l.ustIdNr().equals("IE9700053D"))
                    .findFirst().orElseThrow();

            assertThat(appleLine.country()).isEqualTo("IE");
            assertThat(appleLine.clientName()).isEqualTo("Apple Distribution International");
            assertThat(appleLine.netTotal()).isEqualByComparingTo("8000.00");
            assertThat(appleLine.invoiceCount()).isEqualTo(2);
        }

        @Test
        void multipleCountriesInReport() {
            createReverseChargeInvoice(apple, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));
            createReverseChargeInvoice(austrian, new BigDecimal("2000.00"), LocalDate.of(2026, 2, 10));

            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            assertThat(report.lines()).hasSize(2);
            // Sorted by country: AT before IE
            assertThat(report.lines().get(0).country()).isEqualTo("AT");
            assertThat(report.lines().get(1).country()).isEqualTo("IE");
        }

        @Test
        void excludesDomesticInvoices() {
            createReverseChargeInvoice(apple, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));
            createRegularInvoice(deClient,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 10));

            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            assertThat(report.lines()).hasSize(1);
            assertThat(report.lines().get(0).clientName()).isEqualTo("Apple Distribution International");
        }

        @Test
        void respectsDateRange() {
            createReverseChargeInvoice(apple, new BigDecimal("5000.00"), LocalDate.of(2026, 1, 15));
            createReverseChargeInvoice(apple, new BigDecimal("3000.00"), LocalDate.of(2026, 4, 15));

            // Only Q1
            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            assertThat(report.totalInvoices()).isEqualTo(1);
            assertThat(report.totalNet()).isEqualByComparingTo("5000.00");
        }

        @Test
        void emptyReportWhenNoZmTransactions() {
            ZmReport report = zmReportService.generate(user.getId(),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

            assertThat(report.lines()).isEmpty();
            assertThat(report.totalInvoices()).isZero();
            assertThat(report.totalNet()).isEqualByComparingTo("0");
        }
    }
}
