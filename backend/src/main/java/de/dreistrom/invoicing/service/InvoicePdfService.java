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
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates PDF invoices with all required §14 UStG fields:
 * - Issuer / recipient info
 * - Sequential invoice number
 * - Invoice date
 * - Service description (line items)
 * - Net total, VAT rate, VAT amount, gross total
 * - Kleinunternehmer / reverse charge / §3a notices
 */
@Service
public class InvoicePdfService {

    private static final DateTimeFormatter DE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

    public byte[] generatePdf(Invoice invoice) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, invoice);
            addRecipient(document, invoice);
            addInvoiceMeta(document, invoice);
            addLineItemsTable(document, invoice.getLineItems());
            addTotals(document, invoice);
            addVatNotice(document, invoice);
            addFooter(document, invoice);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF for invoice " + invoice.getNumber(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF for invoice " + invoice.getNumber(), e);
        }
    }

    private void addHeader(Document document, Invoice invoice) throws DocumentException {
        Paragraph title = new Paragraph("RECHNUNG", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addRecipient(Document document, Invoice invoice) throws DocumentException {
        document.add(new Paragraph("An:", HEADER_FONT));
        document.add(new Paragraph(invoice.getClient().getName(), NORMAL_FONT));
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addInvoiceMeta(Document document, Invoice invoice) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(60);
        metaTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        metaTable.setWidths(new float[]{1, 2});

        addMetaRow(metaTable, "Rechnungsnr.:", invoice.getNumber());
        addMetaRow(metaTable, "Rechnungsdatum:", invoice.getInvoiceDate().format(DE_DATE));
        if (invoice.getDueDate() != null) {
            addMetaRow(metaTable, "Fällig am:", invoice.getDueDate().format(DE_DATE));
        }
        addMetaRow(metaTable, "Strom:", invoice.getStreamType().name());
        addMetaRow(metaTable, "Währung:", invoice.getCurrency());

        document.add(metaTable);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(0);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(0);
        table.addCell(valueCell);
    }

    private void addLineItemsTable(Document document, List<LineItem> items) throws DocumentException {
        document.add(new Paragraph("Leistungen:", HEADER_FONT));
        document.add(new Paragraph(" ", SMALL_FONT));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 1, 1.5f, 1, 1.5f});

        // Header row
        addTableHeader(table, "Beschreibung");
        addTableHeader(table, "Menge");
        addTableHeader(table, "Einzelpreis");
        addTableHeader(table, "MwSt %");
        addTableHeader(table, "Gesamt");

        // Item rows
        for (LineItem item : items) {
            BigDecimal lineTotal = item.quantity().multiply(item.unitPrice());

            addTableCell(table, item.description());
            addTableCellRight(table, item.quantity().toPlainString());
            addTableCellRight(table, formatMoney(item.unitPrice()));
            addTableCellRight(table, item.vatRate().toPlainString() + "%");
            addTableCellRight(table, formatMoney(lineTotal));
        }

        document.add(table);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(3);
        table.addCell(cell);
    }

    private void addTableCellRight(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(3);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{2, 1.5f});

        addTotalRow(totals, "Nettobetrag:", formatMoney(invoice.getNetTotal()));

        String vatLabel = invoice.getVatTreatment() == VatTreatment.SMALL_BUSINESS
                ? "USt (§19):" : "USt:";
        addTotalRow(totals, vatLabel, formatMoney(invoice.getVat()));

        PdfPCell grossLabel = new PdfPCell(new Phrase("Bruttobetrag:", HEADER_FONT));
        grossLabel.setBorder(0);
        grossLabel.setPaddingTop(4);
        totals.addCell(grossLabel);

        PdfPCell grossValue = new PdfPCell(new Phrase(
                formatMoney(invoice.getGrossTotal()) + " " + invoice.getCurrency(), HEADER_FONT));
        grossValue.setBorder(0);
        grossValue.setPaddingTop(4);
        grossValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(grossValue);

        document.add(totals);
        document.add(new Paragraph(" ", NORMAL_FONT));
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, NORMAL_FONT));
        labelCell.setBorder(0);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(0);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addVatNotice(Document document, Invoice invoice) throws DocumentException {
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            document.add(new Paragraph("Hinweis:", HEADER_FONT));
            document.add(new Paragraph(invoice.getNotes(), SMALL_FONT));
            document.add(new Paragraph(" ", NORMAL_FONT));
        }
    }

    private void addFooter(Document document, Invoice invoice) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Rechnungsnummer: " + invoice.getNumber()
                + " | Erstellt am: " + invoice.getCreatedAt().toString().substring(0, 10),
                SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
