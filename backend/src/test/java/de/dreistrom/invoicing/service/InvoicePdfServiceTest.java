package de.dreistrom.invoicing.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InvoicePdfServiceTest {

    private final InvoicePdfService pdfService = new InvoicePdfService();

    private AppUser user;
    private Client germanClient;
    private Client euClient;

    @BeforeEach
    void setUp() {
        user = new AppUser("test@dreistrom.de", "hash", "Max Mustermann IT-Beratung");

        germanClient = new Client(user, "Musterfirma GmbH", IncomeStream.FREIBERUF);

        euClient = new Client(user, "EU Partner BV", IncomeStream.FREIBERUF,
                ClientType.B2B, "NL", "NL123456789B01");
    }

    @Nested
    class StreamStyling {

        @Test
        void freiberufInvoice_containsFreiberufLabel() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            byte[] pdf = pdfService.generatePdf(invoice);
            String text = extractText(pdf);

            assertThat(text).contains("Freiberufliche");
            assertThat(text).doesNotContain("Gewerbebetrieb");
        }

        @Test
        void gewerbeInvoice_containsGewerbeLabel() throws Exception {
            Client gewerbeClient = new Client(user, "Gewerbe Kunde", IncomeStream.GEWERBE);
            Invoice invoice = createInvoice(InvoiceStream.GEWERBE, VatTreatment.REGULAR,
                    gewerbeClient, new BigDecimal("500"), new BigDecimal("95"),
                    new BigDecimal("595"), null);
            byte[] pdf = pdfService.generatePdf(invoice);
            String text = extractText(pdf);

            assertThat(text).contains("Gewerbebetrieb");
            assertThat(text).doesNotContain("Freiberufliche");
        }

        @Test
        void bothStreams_produceValidPdf() throws Exception {
            Invoice freiberuf = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            Invoice gewerbe = createInvoice(InvoiceStream.GEWERBE, VatTreatment.REGULAR,
                    new Client(user, "Gewerbe GmbH", IncomeStream.GEWERBE),
                    new BigDecimal("1000"), new BigDecimal("190"),
                    new BigDecimal("1190"), null);

            byte[] pdfFreiberuf = pdfService.generatePdf(freiberuf);
            byte[] pdfGewerbe = pdfService.generatePdf(gewerbe);

            assertThat(pdfFreiberuf).isNotEmpty();
            assertThat(pdfGewerbe).isNotEmpty();
            // Different PDFs due to styling
            assertThat(pdfFreiberuf).isNotEqualTo(pdfGewerbe);
        }
    }

    @Nested
    class UStG14Fields {

        @Test
        void pdf_containsIssuerName() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Max Mustermann IT-Beratung");
        }

        @Test
        void pdf_containsRecipientName() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Musterfirma GmbH");
        }

        @Test
        void pdf_containsInvoiceNumber() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("FB-2026-0001");
        }

        @Test
        void pdf_containsInvoiceDate() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("15.03.2026");
        }

        @Test
        void pdf_containsLeistungsdatum() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Leistungsdatum");
        }

        @Test
        void pdf_containsLineItemDescription() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("IT-Beratung");
        }

        @Test
        void pdf_containsNetAndGrossAmounts() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Nettobetrag");
            assertThat(text).contains("Bruttobetrag");
        }

        @Test
        void pdf_containsDueDate() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("14.04.2026");
        }

        @Test
        void pdf_containsClientUstIdNr_whenPresent() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR,
                    euClient, new BigDecimal("1000"), new BigDecimal("190"),
                    new BigDecimal("1190"), null);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("USt-IdNr");
            assertThat(text).contains("NL123456789B01");
        }

        @Test
        void pdf_containsClientCountry() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR,
                    euClient, new BigDecimal("1000"), new BigDecimal("190"),
                    new BigDecimal("1190"), null);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("NL");
        }
    }

    @Nested
    class KleinunternehmerLayout {

        @Test
        void kleinunternehmer_containsSection19Label() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.SMALL_BUSINESS,
                    germanClient, new BigDecimal("500"), BigDecimal.ZERO,
                    new BigDecimal("500"),
                    "Gemäß §19 UStG wird keine Umsatzsteuer berechnet.");
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("§19 UStG");
        }

        @Test
        void kleinunternehmer_noVatColumnInLineItems() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.SMALL_BUSINESS,
                    germanClient, new BigDecimal("500"), BigDecimal.ZERO,
                    new BigDecimal("500"),
                    "Gemäß §19 UStG wird keine Umsatzsteuer berechnet.");
            String text = extractText(pdfService.generatePdf(invoice));

            // Kleinunternehmer layout should NOT have MwSt % column header
            assertThat(text).doesNotContain("MwSt %");
        }

        @Test
        void kleinunternehmer_containsNoticeText() throws Exception {
            String notice = "Gemäß §19 UStG wird keine Umsatzsteuer berechnet.";
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.SMALL_BUSINESS,
                    germanClient, new BigDecimal("500"), BigDecimal.ZERO,
                    new BigDecimal("500"), notice);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("§19 UStG");
            assertThat(text).contains("keine Umsatzsteuer");
        }
    }

    @Nested
    class RegelbesteuerungLayout {

        @Test
        void regular_containsVatColumn() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("MwSt %");
            assertThat(text).contains("19%");
        }

        @Test
        void regular_containsVatAmount() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("USt (19%)");
        }
    }

    @Nested
    class VatTreatmentNotices {

        @Test
        void reverseCharge_containsNoticeLabel() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REVERSE_CHARGE,
                    euClient, new BigDecimal("1000"), BigDecimal.ZERO,
                    new BigDecimal("1000"),
                    "Steuerschuldnerschaft des Leistungsempfängers (Reverse Charge).");
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Reverse Charge");
        }

        @Test
        void intraEU_containsNoticeLabel() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.INTRA_EU,
                    euClient, new BigDecimal("1000"), BigDecimal.ZERO,
                    new BigDecimal("1000"),
                    "Innergemeinschaftliche Lieferung, steuerfrei nach §4 Nr. 1b UStG.");
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Innergemeinschaftliche Lieferung");
        }

        @Test
        void thirdCountry_containsNoticeLabel() throws Exception {
            Client usClient = new Client(user, "US Corp", IncomeStream.FREIBERUF,
                    ClientType.B2B, "US", null);
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.THIRD_COUNTRY,
                    usClient, new BigDecimal("2000"), BigDecimal.ZERO,
                    new BigDecimal("2000"),
                    "Leistungsort im Drittland, nicht steuerbar nach §3a UStG.");
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("§3a UStG");
        }

        @Test
        void regular_withoutNotes_noNoticeBox() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).doesNotContain("Hinweis zur Umsatzsteuer");
            assertThat(text).doesNotContain("Hinweis (Reverse Charge)");
        }
    }

    @Nested
    class FooterContent {

        @Test
        void footer_containsIssuerAndInvoiceNumber() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            assertThat(text).contains("Max Mustermann IT-Beratung");
            assertThat(text).contains("FB-2026-0001");
        }

        @Test
        void footer_containsStreamLabel() throws Exception {
            Invoice invoice = createInvoice(InvoiceStream.FREIBERUF, VatTreatment.REGULAR);
            String text = extractText(pdfService.generatePdf(invoice));

            // Footer includes stream label
            assertThat(text).contains("Freiberufliche");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Invoice createInvoice(InvoiceStream stream, VatTreatment treatment) {
        return createInvoice(stream, treatment, germanClient,
                new BigDecimal("1000"), new BigDecimal("190"),
                new BigDecimal("1190"), null);
    }

    private Invoice createInvoice(InvoiceStream stream, VatTreatment treatment,
                                   Client client, BigDecimal net, BigDecimal vat,
                                   BigDecimal gross, String notes) {
        List<LineItem> lineItems = List.of(
                new LineItem("IT-Beratung", new BigDecimal("10"),
                        new BigDecimal("100"), new BigDecimal("19")));

        Invoice invoice = new Invoice(user, stream, stream == InvoiceStream.FREIBERUF
                ? "FB-2026-0001" : "GW-2026-0001",
                client, LocalDate.of(2026, 3, 15), lineItems, net, vat, gross, treatment);

        // Set dueDate and notes via update
        invoice.update(client, LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 14),
                lineItems, net, vat, gross, treatment, notes);

        return invoice;
    }

    private String extractText(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(extractor.getTextFromPage(i));
        }
        reader.close();
        return sb.toString();
    }
}
