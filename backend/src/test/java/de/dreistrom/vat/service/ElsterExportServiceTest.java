package de.dreistrom.vat.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.invoicing.domain.*;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ElsterExportServiceTest {

    @Autowired private ElsterExportService elsterExportService;
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
                "elster-test@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Max Mustermann"));

        freiberufClient = clientRepository.save(
                new Client(user, "Test Client", IncomeStream.FREIBERUF));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("elster-test@dreistrom.de", null, List.of()));
    }

    private void createInvoice(BigDecimal net, BigDecimal vat, BigDecimal gross, LocalDate date) {
        invoiceRepository.save(new Invoice(
                user, InvoiceStream.FREIBERUF, "INV-" + System.nanoTime(), freiberufClient,
                date, List.of(new LineItem("Service", BigDecimal.ONE, net, new BigDecimal("19"))),
                net, vat, gross, VatTreatment.REGULAR));
    }

    // ── XML Export ──────────────────────────────────────────────────────

    @Nested
    class XmlExport {

        @Test
        void generatesValidElsterXml() {
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            assertThat(xmlStr).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"");
            assertThat(xmlStr).contains("<Elster");
            assertThat(xmlStr).contains("elster.de/elsterxml/schema/v11");
        }

        @Test
        void xmlContainsTransferHeader() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            assertThat(xmlStr).contains("<Verfahren>ElsterAnmeldung</Verfahren>");
            assertThat(xmlStr).contains("<DatenArt>UStVA</DatenArt>");
            assertThat(xmlStr).contains("<Vorgang>send</Vorgang>");
            assertThat(xmlStr).contains("<Testmerker>700000004</Testmerker>");
            assertThat(xmlStr).contains("<DatenLieferant>Max Mustermann</DatenLieferant>");
        }

        @Test
        void xmlContainsCorrectKennzahlen() {
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            // Kz81: net revenue = 190 * 100/19 = 1000.00
            assertThat(xmlStr).contains("<Kz81>1000.00</Kz81>");
            // Kz66: input VAT (no expenses → 0)
            assertThat(xmlStr).contains("<Kz66>0.00</Kz66>");
            // Kz83: net payable = 190 - 0 = 190.00
            assertThat(xmlStr).contains("<Kz83>190.00</Kz83>");
        }

        @Test
        void xmlContainsPeriodInfo() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 3, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            assertThat(xmlStr).contains("<Jahr>2026</Jahr>");
            assertThat(xmlStr).contains("<Zeitraum>03</Zeitraum>");
            assertThat(xmlStr).contains("art=\"UStVA\"");
            assertThat(xmlStr).contains("version=\"202603\"");
        }

        @Test
        void xmlHandlesQuarterlyPeriod() {
            createInvoice(new BigDecimal("5000.00"), new BigDecimal("950.00"),
                    new BigDecimal("5950.00"), LocalDate.of(2026, 2, 15));

            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.QUARTERLY, 1, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            assertThat(xmlStr).contains("<Zeitraum>41</Zeitraum>");
            assertThat(xmlStr).contains("<Kz81>5000.00</Kz81>");
        }
    }

    // ── CSV Export ──────────────────────────────────────────────────────

    @Nested
    class CsvExport {

        @Test
        void generatesCsvWithCorrectFormat() {
            createInvoice(new BigDecimal("1000.00"), new BigDecimal("190.00"),
                    new BigDecimal("1190.00"), LocalDate.of(2026, 1, 15));

            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] csv = elsterExportService.generateCsv(vr);
            String csvStr = new String(csv, StandardCharsets.UTF_8);

            assertThat(csvStr).contains("Kennzahl;Beschreibung;Betrag EUR");
            assertThat(csvStr).contains(";Jahr;2026");
            assertThat(csvStr).contains(";Zeitraum;01");
            assertThat(csvStr).contains("81;Steuerpflichtige Umsaetze 19% (Bemessungsgrundlage);1000.00");
            assertThat(csvStr).contains("66;Vorsteuerbetraege;0.00");
            assertThat(csvStr).contains("83;Verbleibende USt-Vorauszahlung;190.00");
        }

        @Test
        void csvIncludesVatAmount() {
            createInvoice(new BigDecimal("2000.00"), new BigDecimal("380.00"),
                    new BigDecimal("2380.00"), LocalDate.of(2026, 3, 10));

            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.QUARTERLY, 1, false);

            byte[] csv = elsterExportService.generateCsv(vr);
            String csvStr = new String(csv, StandardCharsets.UTF_8);

            assertThat(csvStr).contains(";Umsatzsteuer 19%;380.00");
        }
    }

    // ── Zeitraum Formatting ─────────────────────────────────────────────

    @Nested
    class ZeitraumFormatting {

        @Test
        void monthlyFormat() {
            assertThat(ElsterExportService.formatZeitraum(PeriodType.MONTHLY, (short) 1))
                    .isEqualTo("01");
            assertThat(ElsterExportService.formatZeitraum(PeriodType.MONTHLY, (short) 12))
                    .isEqualTo("12");
        }

        @Test
        void quarterlyFormat() {
            assertThat(ElsterExportService.formatZeitraum(PeriodType.QUARTERLY, (short) 1))
                    .isEqualTo("41");
            assertThat(ElsterExportService.formatZeitraum(PeriodType.QUARTERLY, (short) 4))
                    .isEqualTo("44");
        }

        @Test
        void annualFormat() {
            assertThat(ElsterExportService.formatZeitraum(PeriodType.ANNUAL, (short) 1))
                    .isEqualTo("12");
        }
    }

    // ── Zero / Empty ────────────────────────────────────────────────────

    @Nested
    class ZeroAmounts {

        @Test
        void handlesZeroAmounts() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] xml = elsterExportService.generateElsterXml(vr, "Max Mustermann");
            String xmlStr = new String(xml, StandardCharsets.UTF_8);

            assertThat(xmlStr).contains("<Kz81>0.00</Kz81>");
            assertThat(xmlStr).contains("<Kz66>0.00</Kz66>");
            assertThat(xmlStr).contains("<Kz83>0.00</Kz83>");
        }

        @Test
        void csvHandlesZeroAmounts() {
            VatReturn vr = vatReturnService.generateForPeriod(
                    user, 2026, PeriodType.MONTHLY, 1, false);

            byte[] csv = elsterExportService.generateCsv(vr);
            String csvStr = new String(csv, StandardCharsets.UTF_8);

            assertThat(csvStr).contains("81;Steuerpflichtige Umsaetze 19% (Bemessungsgrundlage);0.00");
            assertThat(csvStr).contains("83;Verbleibende USt-Vorauszahlung;0.00");
        }
    }
}
