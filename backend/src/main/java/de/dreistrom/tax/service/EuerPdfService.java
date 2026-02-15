package de.dreistrom.tax.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.tax.dto.EuerResult;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Generates EÜR (Einnahmen-Überschuss-Rechnung) PDF per §4 Abs. 3 EStG.
 * Supports single-stream and dual-stream comparison views.
 */
@Service
public class EuerPdfService {

    private static final Color FREIBERUF_ACCENT = new Color(0, 82, 155);
    private static final Color GEWERBE_ACCENT = new Color(0, 120, 60);
    private static final Color HEADER_BG = new Color(240, 240, 240);
    private static final Color ROW_ALT_BG = new Color(250, 250, 250);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

    private static final DecimalFormat EUR_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        EUR_FORMAT = new DecimalFormat("#,##0.00 €", symbols);
    }

    /**
     * Generate PDF for a single stream EÜR.
     */
    public byte[] generateSingleStream(EuerResult euer) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Color accent = euer.stream() == IncomeStream.FREIBERUF
                    ? FREIBERUF_ACCENT : GEWERBE_ACCENT;
            String streamLabel = euer.stream() == IncomeStream.FREIBERUF
                    ? "Freiberufliche Tätigkeit (§18 EStG)"
                    : "Gewerbliche Tätigkeit (§15 EStG)";

            addTitle(doc, "Einnahmen-Überschuss-Rechnung", accent);
            addSubtitle(doc, streamLabel + " — " + euer.taxYear());
            addLegalNote(doc);

            addEuerTable(doc, euer);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate EÜR PDF", e);
        }
    }

    /**
     * Generate PDF comparing Freiberuf and Gewerbe EÜR side by side.
     */
    public byte[] generateDualStream(EuerService.DualStreamEuer dual) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, "Einnahmen-Überschuss-Rechnung — Gesamtübersicht",
                    new Color(50, 50, 50));
            addSubtitle(doc, "Steuerjahr " + dual.freiberuf().taxYear());
            addLegalNote(doc);

            addComparisonTable(doc, dual);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate dual EÜR PDF", e);
        }
    }

    private void addTitle(Document doc, String text, Color color) throws DocumentException {
        Font font = new Font(TITLE_FONT);
        font.setColor(color);
        Paragraph title = new Paragraph(text, font);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        doc.add(title);
    }

    private void addSubtitle(Document doc, String text) throws DocumentException {
        Paragraph sub = new Paragraph(text, SUBTITLE_FONT);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(5);
        doc.add(sub);
    }

    private void addLegalNote(Document doc) throws DocumentException {
        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph note = new Paragraph(
                "Gewinnermittlung nach §4 Abs. 3 EStG (Einnahmen-Überschuss-Rechnung)", noteFont);
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingAfter(20);
        doc.add(note);
    }

    private void addEuerTable(Document doc, EuerResult euer) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(90);
        table.setWidths(new float[]{70, 30});

        addHeaderRow(table, "Position", "Betrag");
        addRow(table, "Betriebseinnahmen", euer.totalIncome(), false);
        addSeparator(table);
        addRow(table, "  Betriebsausgaben (direkt)", euer.directExpenses(), true);
        addRow(table, "  Absetzung für Abnutzung (AfA)", euer.depreciation(), true);
        addRow(table, "Betriebsausgaben gesamt", euer.totalExpenses(), false);
        addSeparator(table);
        addTotalRow(table, euer.profit().signum() >= 0 ? "Gewinn" : "Verlust", euer.profit());

        doc.add(table);
    }

    private void addComparisonTable(Document doc, EuerService.DualStreamEuer dual) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(95);
        table.setWidths(new float[]{50, 25, 25});

        addComparisonHeader(table);

        EuerResult fb = dual.freiberuf();
        EuerResult gw = dual.gewerbe();

        addComparisonRow(table, "Betriebseinnahmen", fb.totalIncome(), gw.totalIncome(), false);
        addComparisonSep(table);
        addComparisonRow(table, "  Betriebsausgaben", fb.directExpenses(), gw.directExpenses(), true);
        addComparisonRow(table, "  AfA", fb.depreciation(), gw.depreciation(), true);
        addComparisonRow(table, "Ausgaben gesamt", fb.totalExpenses(), gw.totalExpenses(), false);
        addComparisonSep(table);
        addComparisonTotalRow(table, fb.profit(), gw.profit());

        doc.add(table);

        Paragraph combined = new Paragraph(
                "Gesamtgewinn/-verlust: " + EUR_FORMAT.format(dual.combinedProfit()), TOTAL_FONT);
        combined.setAlignment(Element.ALIGN_RIGHT);
        combined.setSpacingBefore(15);
        doc.add(combined);
    }

    private void addHeaderRow(PdfPTable table, String col1, String col2) {
        PdfPCell c1 = new PdfPCell(new Phrase(col1, HEADER_FONT));
        c1.setBackgroundColor(HEADER_BG);
        c1.setPadding(6);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(col2, HEADER_FONT));
        c2.setBackgroundColor(HEADER_BG);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        table.addCell(c2);
    }

    private void addRow(PdfPTable table, String label, BigDecimal amount, boolean indent) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, BODY_FONT));
        c1.setBorder(0);
        c1.setPadding(4);
        if (indent) c1.setBackgroundColor(ROW_ALT_BG);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(EUR_FORMAT.format(amount), BODY_FONT));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(4);
        if (indent) c2.setBackgroundColor(ROW_ALT_BG);
        table.addCell(c2);
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal amount) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, TOTAL_FONT));
        c1.setBorderWidthTop(1.5f);
        c1.setBorderWidthBottom(0);
        c1.setBorderWidthLeft(0);
        c1.setBorderWidthRight(0);
        c1.setPadding(6);
        table.addCell(c1);

        Font totalAmountFont = new Font(TOTAL_FONT);
        totalAmountFont.setColor(amount.signum() >= 0 ? new Color(0, 100, 0) : Color.RED);
        PdfPCell c2 = new PdfPCell(new Phrase(EUR_FORMAT.format(amount), totalAmountFont));
        c2.setBorderWidthTop(1.5f);
        c2.setBorderWidthBottom(0);
        c2.setBorderWidthLeft(0);
        c2.setBorderWidthRight(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        table.addCell(c2);
    }

    private void addSeparator(PdfPTable table) {
        for (int i = 0; i < 2; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(""));
            cell.setBorder(0);
            cell.setFixedHeight(3);
            table.addCell(cell);
        }
    }

    private void addComparisonHeader(PdfPTable table) {
        PdfPCell c1 = new PdfPCell(new Phrase("Position", HEADER_FONT));
        c1.setBackgroundColor(HEADER_BG);
        c1.setPadding(6);
        table.addCell(c1);

        Font fbFont = new Font(HEADER_FONT);
        fbFont.setColor(FREIBERUF_ACCENT);
        PdfPCell c2 = new PdfPCell(new Phrase("Freiberuf", fbFont));
        c2.setBackgroundColor(HEADER_BG);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        table.addCell(c2);

        Font gwFont = new Font(HEADER_FONT);
        gwFont.setColor(GEWERBE_ACCENT);
        PdfPCell c3 = new PdfPCell(new Phrase("Gewerbe", gwFont));
        c3.setBackgroundColor(HEADER_BG);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(6);
        table.addCell(c3);
    }

    private void addComparisonRow(PdfPTable table, String label,
                                   BigDecimal fb, BigDecimal gw, boolean indent) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, BODY_FONT));
        c1.setBorder(0);
        c1.setPadding(4);
        if (indent) c1.setBackgroundColor(ROW_ALT_BG);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(EUR_FORMAT.format(fb), BODY_FONT));
        c2.setBorder(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(4);
        if (indent) c2.setBackgroundColor(ROW_ALT_BG);
        table.addCell(c2);

        PdfPCell c3 = new PdfPCell(new Phrase(EUR_FORMAT.format(gw), BODY_FONT));
        c3.setBorder(0);
        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c3.setPadding(4);
        if (indent) c3.setBackgroundColor(ROW_ALT_BG);
        table.addCell(c3);
    }

    private void addComparisonTotalRow(PdfPTable table, BigDecimal fb, BigDecimal gw) {
        PdfPCell c1 = new PdfPCell(new Phrase("Gewinn/Verlust", TOTAL_FONT));
        c1.setBorderWidthTop(1.5f);
        c1.setBorderWidthBottom(0);
        c1.setBorderWidthLeft(0);
        c1.setBorderWidthRight(0);
        c1.setPadding(6);
        table.addCell(c1);

        for (BigDecimal amount : new BigDecimal[]{fb, gw}) {
            Font font = new Font(TOTAL_FONT);
            font.setColor(amount.signum() >= 0 ? new Color(0, 100, 0) : Color.RED);
            PdfPCell cell = new PdfPCell(new Phrase(EUR_FORMAT.format(amount), font));
            cell.setBorderWidthTop(1.5f);
            cell.setBorderWidthBottom(0);
            cell.setBorderWidthLeft(0);
            cell.setBorderWidthRight(0);
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addComparisonSep(PdfPTable table) {
        for (int i = 0; i < 3; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(""));
            cell.setBorder(0);
            cell.setFixedHeight(3);
            table.addCell(cell);
        }
    }
}
