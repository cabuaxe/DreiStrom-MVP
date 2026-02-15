package de.dreistrom.tax.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import de.dreistrom.tax.dto.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Generates a comprehensive PDF for the annual tax return package
 * (Einkommensteuererklaerung) covering all Anlagen.
 */
@Service
public class AnnualTaxPdfService {

    private static final Color TITLE_COLOR = new Color(30, 60, 120);
    private static final Color SECTION_COLOR = new Color(0, 82, 155);
    private static final Color GEWERBE_COLOR = new Color(0, 120, 60);
    private static final Color HEADER_BG = new Color(240, 240, 240);
    private static final Color ROW_ALT_BG = new Color(250, 250, 250);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font TOTAL_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

    private static final DecimalFormat EUR_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        EUR_FORMAT = new DecimalFormat("#,##0.00 €", symbols);
    }

    /**
     * Generate the complete annual tax return PDF.
     */
    public byte[] generate(AnnualTaxPackage pkg) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 45, 45, 45, 45);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addDocumentTitle(doc, pkg.taxYear());

            addAnlageN(doc, pkg.anlageN());
            addAnlageS(doc, pkg.anlageS());
            addAnlageG(doc, pkg.anlageG());
            addAnlageEuer(doc, "Anlage EÜR — Freiberufliche Tätigkeit", pkg.euerFreiberuf());
            addAnlageEuer(doc, "Anlage EÜR — Gewerbliche Tätigkeit", pkg.euerGewerbe());
            addAnlageVorsorgeaufwand(doc, pkg.vorsorgeaufwand());
            addFestsetzung(doc, pkg.taxCalculation(), pkg.gewerbesteuer());

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate annual tax PDF", e);
        }
    }

    // ── Document header ──────────────────────────────────────────────────

    private void addDocumentTitle(Document doc, int year) throws DocumentException {
        Font font = new Font(TITLE_FONT);
        font.setColor(TITLE_COLOR);
        Paragraph title = new Paragraph("Einkommensteuererklaerung " + year, font);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(5);
        doc.add(title);

        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph note = new Paragraph(
                "Jahressteuerpaket — Anlage N, S, G, EÜR, Vorsorgeaufwand", noteFont);
        note.setAlignment(Element.ALIGN_CENTER);
        note.setSpacingAfter(20);
        doc.add(note);
    }

    // ── Anlage N ─────────────────────────────────────────────────────────

    private void addAnlageN(Document doc, AnnualTaxPackage.AnlageN n) throws DocumentException {
        addSectionTitle(doc, "Anlage N — Einkünfte aus nichtselbständiger Arbeit (§19 EStG)");

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Bruttoarbeitslohn", n.bruttoarbeitslohn(), false);
        addRow(table, "  Lohnsteuer (einbehalten)", n.lohnsteuer(), true);
        addRow(table, "  Solidaritätszuschlag", n.solidaritaetszuschlag(), true);
        addRow(table, "  Kirchensteuer", n.kirchensteuer(), true);
        addSep(table);
        addRow(table, "Werbungskostenpauschale", n.werbungskostenpauschale(), false);
        addRow(table, "Fahrtkostenpauschale", n.fahrtkostenpauschale(), false);
        doc.add(table);

        addSpacing(doc);
    }

    // ── Anlage S ─────────────────────────────────────────────────────────

    private void addAnlageS(Document doc, AnnualTaxPackage.AnlageS s) throws DocumentException {
        addSectionTitle(doc, "Anlage S — Einkünfte aus selbständiger Arbeit (§18 EStG)");

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Betriebseinnahmen", s.einnahmen(), false);
        addRow(table, "  Betriebsausgaben", s.betriebsausgaben(), true);
        addRow(table, "  Absetzung für Abnutzung (AfA)", s.afaBetrag(), true);
        addSep(table);
        addTotalRow(table, s.gewinn().signum() >= 0 ? "Gewinn" : "Verlust", s.gewinn());
        doc.add(table);

        addSpacing(doc);
    }

    // ── Anlage G ─────────────────────────────────────────────────────────

    private void addAnlageG(Document doc, AnnualTaxPackage.AnlageG g) throws DocumentException {
        addSectionTitle(doc, "Anlage G — Einkünfte aus Gewerbebetrieb (§15 EStG)");

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Betriebseinnahmen", g.einnahmen(), false);
        addRow(table, "  Betriebsausgaben", g.betriebsausgaben(), true);
        addRow(table, "  Absetzung für Abnutzung (AfA)", g.afaBetrag(), true);
        addSep(table);
        addTotalRow(table, g.gewinn().signum() >= 0 ? "Gewinn" : "Verlust", g.gewinn());
        addSep(table);
        addRow(table, "Gewerbesteuer", g.gewerbesteuer(), false);
        addRow(table, "§35 EStG Anrechnung", g.paragraph35Anrechnung(), false);
        doc.add(table);

        addSpacing(doc);
    }

    // ── Anlage EÜR ──────────────────────────────────────────────────────

    private void addAnlageEuer(Document doc, String title, EuerResult euer) throws DocumentException {
        addSectionTitle(doc, title);

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Betriebseinnahmen", euer.totalIncome(), false);
        addRow(table, "  Betriebsausgaben (direkt)", euer.directExpenses(), true);
        addRow(table, "  Absetzung für Abnutzung (AfA)", euer.depreciation(), true);
        addRow(table, "Betriebsausgaben gesamt", euer.totalExpenses(), false);
        addSep(table);
        addTotalRow(table, euer.profit().signum() >= 0 ? "Gewinn" : "Verlust", euer.profit());
        doc.add(table);

        addSpacing(doc);
    }

    // ── Anlage Vorsorgeaufwand ───────────────────────────────────────────

    private void addAnlageVorsorgeaufwand(Document doc,
                                           AnnualTaxPackage.AnlageVorsorgeaufwand v) throws DocumentException {
        addSectionTitle(doc, "Anlage Vorsorgeaufwand — Versicherungsbeiträge");

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Krankenversicherung", v.krankenversicherung(), false);
        addRow(table, "Pflegeversicherung", v.pflegeversicherung(), false);
        addRow(table, "Rentenversicherung", v.rentenversicherung(), false);
        addRow(table, "Arbeitslosenversicherung", v.arbeitslosenversicherung(), false);
        addSep(table);
        addTotalRow(table, "Vorsorgeaufwendungen gesamt", v.gesamtVorsorge());
        doc.add(table);

        addSpacing(doc);
    }

    // ── Festsetzung (Tax assessment totals) ─────────────────────────────

    private void addFestsetzung(Document doc, TaxCalculationResult tax,
                                 GewerbesteuerResult gewSt) throws DocumentException {
        addSectionTitle(doc, "Steuerfestsetzung — Gesamtübersicht");

        PdfPTable table = createTable();
        addHeaderRow(table);
        addRow(table, "Zu versteuerndes Einkommen (zvE)", tax.taxableIncome(), false);
        addSep(table);
        addRow(table, "Einkommensteuer (§32a EStG)", tax.incomeTax(), false);
        addRow(table, "Solidaritätszuschlag (§5 SolZG)", tax.solidaritaetszuschlag(), false);
        addRow(table, "Gewerbesteuer", gewSt.gewerbesteuer(), false);
        addRow(table, "  ./. §35 EStG Anrechnung", gewSt.paragraph35Credit(), true);
        addRow(table, "  = Netto-Gewerbesteuer", gewSt.netGewerbesteuer(), true);
        addSep(table);

        BigDecimal totalBurden = tax.totalTax().add(gewSt.netGewerbesteuer());
        addTotalRow(table, "Gesamtsteuerbelastung", totalBurden);
        doc.add(table);

        // Rates
        Paragraph rates = new Paragraph(
                String.format("Grenzsteuersatz: %s%%  |  Effektiver Steuersatz: %s%%",
                        tax.marginalRate().toPlainString(),
                        tax.effectiveRate().toPlainString()),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
        rates.setAlignment(Element.ALIGN_RIGHT);
        rates.setSpacingBefore(8);
        doc.add(rates);
    }

    // ── Table helpers ────────────────────────────────────────────────────

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        Font font = new Font(SECTION_FONT);
        font.setColor(SECTION_COLOR);
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(15);
        p.setSpacingAfter(8);
        doc.add(p);
    }

    private PdfPTable createTable() throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(95);
        table.setWidths(new float[]{70, 30});
        return table;
    }

    private void addHeaderRow(PdfPTable table) {
        PdfPCell c1 = new PdfPCell(new Phrase("Position", HEADER_FONT));
        c1.setBackgroundColor(HEADER_BG);
        c1.setPadding(6);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase("Betrag", HEADER_FONT));
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

        Font amountFont = new Font(TOTAL_FONT);
        amountFont.setColor(amount.signum() >= 0 ? new Color(0, 100, 0) : Color.RED);
        PdfPCell c2 = new PdfPCell(new Phrase(EUR_FORMAT.format(amount), amountFont));
        c2.setBorderWidthTop(1.5f);
        c2.setBorderWidthBottom(0);
        c2.setBorderWidthLeft(0);
        c2.setBorderWidthRight(0);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(6);
        table.addCell(c2);
    }

    private void addSep(PdfPTable table) {
        for (int i = 0; i < 2; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(""));
            cell.setBorder(0);
            cell.setFixedHeight(3);
            table.addCell(cell);
        }
    }

    private void addSpacing(Document doc) throws DocumentException {
        doc.add(new Paragraph(" "));
    }
}
