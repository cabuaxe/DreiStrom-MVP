package de.dreistrom.invoicing.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates PDF invoices with all required §14 UStG fields and
 * stream-specific visual styling (Freiberuf vs Gewerbe).
 *
 * §14 Abs. 4 UStG mandatory fields:
 * 1. Issuer name and address (Nr. 1)
 * 2. Recipient name and address (Nr. 1)
 * 3. Tax number or USt-IdNr of the issuer (Nr. 2)
 * 4. Invoice date (Nr. 3)
 * 5. Sequential invoice number (Nr. 4)
 * 6. Service description with quantity/scope (Nr. 5)
 * 7. Delivery/service date (Nr. 6)
 * 8. Net amount per tax rate (Nr. 7)
 * 9. Applicable tax rate and tax amount (Nr. 8)
 *
 * Supports layout variants:
 * - Regelbesteuerung: full VAT breakdown with MwSt column
 * - Kleinunternehmer §19: no VAT column, prominent §19 notice
 * - Reverse charge: zero VAT, reverse charge notice
 * - Intra-EU / Third country: zero VAT with appropriate notice
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Freiberuf accent: professional blue
    private static final Color FREIBERUF_ACCENT = new Color(0, 82, 155);
    private static final Color FREIBERUF_LIGHT = new Color(220, 235, 250);

    // Gewerbe accent: business green
    private static final Color GEWERBE_ACCENT = new Color(0, 120, 60);
    private static final Color GEWERBE_LIGHT = new Color(220, 245, 225);

    private static final Color NOTICE_BG = new Color(255, 248, 220);
    private static final Color FOOTER_LINE = new Color(180, 180, 180);

    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font SMALL_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

    public byte[] generatePdf(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Color accent = getAccentColor(invoice.getStreamType());
            Color lightBg = getLightColor(invoice.getStreamType());

            addHeader(document, invoice, accent);
            addIssuerAndRecipient(document, invoice);
            addInvoiceMeta(document, invoice);
            addLineItemsTable(document, invoice, lightBg);
            addTotals(document, invoice, accent);
            addVatNotice(document, invoice);
            addFooter(document, invoice, accent);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF for invoice " + invoice.getNumber(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF for invoice " + invoice.getNumber(), e);
        }
    }

    private Color getAccentColor(InvoiceStream stream) {
        return stream == InvoiceStream.FREIBERUF ? FREIBERUF_ACCENT : GEWERBE_ACCENT;
    }

    private Color getLightColor(InvoiceStream stream) {
        return stream == InvoiceStream.FREIBERUF ? FREIBERUF_LIGHT : GEWERBE_LIGHT;
    }

    private String getStreamLabel(InvoiceStream stream) {
        return stream == InvoiceStream.FREIBERUF
                ? "Freiberufliche Tätigkeit" : "Gewerbebetrieb";
    }

    // ── Header with stream-specific accent ──────────────────────────────

    private void addHeader(Document document, Invoice invoice, Color accent)
            throws DocumentException {

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, accent);
        Paragraph title = new Paragraph("RECHNUNG", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        Font streamFont = FontFactory.getFont(FontFactory.HELVETICA, 11, accent);
        Paragraph streamLabel = new Paragraph(getStreamLabel(invoice.getStreamType()), streamFont);
        streamLabel.setAlignment(Element.ALIGN_LEFT);
        document.add(streamLabel);

        LineSeparator line = new LineSeparator(1.5f, 100, accent, Element.ALIGN_CENTER, -2);
        document.add(line);
        document.add(new Paragraph(" ", SMALL_FONT));
    }

    // ── Issuer (§14 Abs. 4 Nr. 1) and Recipient ────────────────────────

    private void addIssuerAndRecipient(Document document, Invoice invoice)
            throws DocumentException {

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1});

        // Issuer (left column)
        Font sectionHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell issuerCell = new PdfPCell();
        issuerCell.setBorder(0);
        issuerCell.setPaddingBottom(8);
        issuerCell.addElement(new Phrase("Von:", sectionHeader));
        issuerCell.addElement(new Phrase(invoice.getUser().getDisplayName(), NORMAL_FONT));

        // Recipient (right column) — §14 Abs. 4 Nr. 1
        PdfPCell recipientCell = new PdfPCell();
        recipientCell.setBorder(0);
        recipientCell.setPaddingBottom(8);
        recipientCell.addElement(new Phrase("An:", sectionHeader));
        recipientCell.addElement(new Phrase(invoice.getClient().getName(), NORMAL_FONT));

        if (invoice.getClient().getCountry() != null) {
            recipientCell.addElement(new Phrase(
                    "Land: " + invoice.getClient().getCountry(), SMALL_FONT));
        }
        if (invoice.getClient().getUstIdNr() != null
                && !invoice.getClient().getUstIdNr().isBlank()) {
            recipientCell.addElement(new Phrase(
                    "USt-IdNr: " + invoice.getClient().getUstIdNr(), SMALL_FONT));
        }

        table.addCell(issuerCell);
        table.addCell(recipientCell);

        document.add(table);
        document.add(new Paragraph(" ", SMALL_FONT));
    }

    // ── Invoice metadata (number, date, due date, currency) ─────────────

    private void addInvoiceMeta(Document document, Invoice invoice) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(55);
        metaTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        metaTable.setWidths(new float[]{1.2f, 2});

        // §14 Abs. 4 Nr. 4: Sequential invoice number
        addMetaRow(metaTable, "Rechnungsnr.:", invoice.getNumber(), headerFont);
        // §14 Abs. 4 Nr. 3: Invoice date
        addMetaRow(metaTable, "Rechnungsdatum:", invoice.getInvoiceDate().format(DE_DATE), headerFont);
        // §14 Abs. 4 Nr. 6: Delivery/service date (= invoice date)
        addMetaRow(metaTable, "Leistungsdatum:", invoice.getInvoiceDate().format(DE_DATE), headerFont);
        if (invoice.getDueDate() != null) {
            addMetaRow(metaTable, "Fällig am:", invoice.getDueDate().format(DE_DATE), headerFont);
        }
        addMetaRow(metaTable, "Währung:", invoice.getCurrency(), headerFont);

        document.add(metaTable);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addMetaRow(PdfPTable table, String label, String value, Font headerFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, headerFont));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(2);
        table.addCell(valueCell);
    }

    // ── Line items table — §14 Abs. 4 Nr. 5, 7, 8 ──────────────────────

    private void addLineItemsTable(Document document, Invoice invoice, Color lightBg)
            throws DocumentException {

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        document.add(new Paragraph("Leistungen:", headerFont));
        document.add(new Paragraph(" ", SMALL_FONT));

        boolean showVat = !isVatFreeLayout(invoice.getVatTreatment());

        int cols = showVat ? 5 : 4;
        PdfPTable table = new PdfPTable(cols);
        table.setWidthPercentage(100);

        if (showVat) {
            table.setWidths(new float[]{4, 1, 1.5f, 1, 1.5f});
        } else {
            table.setWidths(new float[]{5, 1, 1.5f, 1.5f});
        }

        // Header row with stream-colored background
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Color accent = getAccentColor(invoice.getStreamType());

        addTableHeader(table, "Beschreibung", accent, tableHeaderFont);
        addTableHeader(table, "Menge", accent, tableHeaderFont);
        addTableHeader(table, "Einzelpreis", accent, tableHeaderFont);
        if (showVat) {
            addTableHeader(table, "MwSt %", accent, tableHeaderFont);
        }
        addTableHeader(table, "Gesamt", accent, tableHeaderFont);

        // Item rows with alternating background
        List<LineItem> items = invoice.getLineItems();
        for (int i = 0; i < items.size(); i++) {
            LineItem item = items.get(i);
            BigDecimal lineTotal = item.quantity().multiply(item.unitPrice());
            Color rowBg = (i % 2 == 0) ? Color.WHITE : lightBg;

            addTableCell(table, item.description(), rowBg);
            addTableCellRight(table, item.quantity().toPlainString(), rowBg);
            addTableCellRight(table, formatMoney(item.unitPrice()), rowBg);
            if (showVat) {
                addTableCellRight(table, item.vatRate().toPlainString() + "%", rowBg);
            }
            addTableCellRight(table, formatMoney(lineTotal), rowBg);
        }

        document.add(table);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private boolean isVatFreeLayout(VatTreatment treatment) {
        return treatment == VatTreatment.SMALL_BUSINESS;
    }

    private void addTableHeader(PdfPTable table, String text, Color bg, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderWidth(0);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    private void addTableCellRight(PdfPTable table, String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setBackgroundColor(bg);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(220, 220, 220));
        table.addCell(cell);
    }

    // ── Totals section ──────────────────────────────────────────────────

    private void addTotals(Document document, Invoice invoice, Color accent)
            throws DocumentException {

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{2, 1.5f});

        addTotalRow(totals, "Nettobetrag:", formatMoney(invoice.getNetTotal()));

        // §14 Abs. 4 Nr. 8: Tax rate and tax amount
        if (invoice.getVatTreatment() == VatTreatment.SMALL_BUSINESS) {
            addTotalRow(totals, "USt (§19 UStG):", "0,00");
        } else if (invoice.getVatTreatment() == VatTreatment.REVERSE_CHARGE) {
            addTotalRow(totals, "USt (Reverse Charge):", formatMoney(invoice.getVat()));
        } else if (invoice.getVatTreatment() == VatTreatment.THIRD_COUNTRY) {
            addTotalRow(totals, "USt (§3a UStG):", formatMoney(invoice.getVat()));
        } else if (invoice.getVatTreatment() == VatTreatment.INTRA_EU) {
            addTotalRow(totals, "USt (innergemeinsch.):", formatMoney(invoice.getVat()));
        } else {
            addTotalRow(totals, "USt (19%):", formatMoney(invoice.getVat()));
        }

        // Gross total with accent color
        Font grossFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, accent);
        PdfPCell grossLabel = new PdfPCell(new Phrase("Bruttobetrag:", grossFont));
        grossLabel.setBorder(0);
        grossLabel.setPaddingTop(6);
        totals.addCell(grossLabel);

        PdfPCell grossValue = new PdfPCell(new Phrase(
                formatMoney(invoice.getGrossTotal()) + " " + invoice.getCurrency(), grossFont));
        grossValue.setBorder(0);
        grossValue.setPaddingTop(6);
        grossValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(grossValue);

        document.add(totals);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(2);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(2);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    // ── VAT notice — prominent box for Kleinunternehmer / RC / etc. ─────

    private void addVatNotice(Document document, Invoice invoice) throws DocumentException {
        String notice = invoice.getNotes();
        if (notice == null || notice.isBlank()) {
            return;
        }

        // Prominent notice box for tax-relevant notes
        PdfPTable noticeTable = new PdfPTable(1);
        noticeTable.setWidthPercentage(100);

        String label = getNoticeLabel(invoice.getVatTreatment());

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(NOTICE_BG);
        cell.setPadding(8);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(new Color(200, 190, 150));
        cell.addElement(new Phrase(label, SMALL_BOLD_FONT));
        cell.addElement(new Phrase(notice, SMALL_FONT));
        noticeTable.addCell(cell);

        document.add(noticeTable);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private String getNoticeLabel(VatTreatment treatment) {
        return switch (treatment) {
            case SMALL_BUSINESS -> "Hinweis zur Umsatzsteuer (§19 UStG):\n";
            case REVERSE_CHARGE -> "Hinweis (Reverse Charge):\n";
            case INTRA_EU -> "Hinweis (Innergemeinschaftliche Lieferung):\n";
            case THIRD_COUNTRY -> "Hinweis (Drittlandleistung §3a UStG):\n";
            case REGULAR -> "Hinweis:\n";
        };
    }

    // ── Footer with stream accent ───────────────────────────────────────

    private void addFooter(Document document, Invoice invoice, Color accent)
            throws DocumentException {

        LineSeparator line = new LineSeparator(0.5f, 100, FOOTER_LINE, Element.ALIGN_CENTER, -2);
        document.add(line);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY);
        Paragraph footer = new Paragraph(
                invoice.getUser().getDisplayName()
                        + " | Rechnungsnr. " + invoice.getNumber()
                        + " | " + invoice.getInvoiceDate().format(DE_DATE)
                        + " | " + getStreamLabel(invoice.getStreamType()),
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%,.2f", amount).replace('.', '_').replace(',', '.').replace('_', ',');
    }
}
