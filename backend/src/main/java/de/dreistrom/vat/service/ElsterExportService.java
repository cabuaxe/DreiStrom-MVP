package de.dreistrom.vat.service;

import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.elster.ElsterXml;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates ERiC-compatible ELSTER XML and CSV fallback for Umsatzsteuervoranmeldung.
 */
@Service
public class ElsterExportService {

    private static final DateTimeFormatter ELSTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final BigDecimal STANDARD_RATE = new BigDecimal("19");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final JAXBContext jaxbContext;

    public ElsterExportService() {
        try {
            this.jaxbContext = JAXBContext.newInstance(ElsterXml.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialise JAXB context", e);
        }
    }

    /**
     * Generate ERiC-compatible ELSTER XML for a VAT return.
     *
     * @param vatReturn   the VAT return data
     * @param displayName the taxpayer's display name (DatenLieferant)
     * @return UTF-8 encoded XML bytes
     */
    public byte[] generateElsterXml(VatReturn vatReturn, String displayName) {
        ElsterXml elster = buildElsterXml(vatReturn, displayName);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            marshaller.marshal(elster, writer);
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal ELSTER XML", e);
        }
    }

    /**
     * Generate semicolon-separated CSV for Steuerberater.
     */
    public byte[] generateCsv(VatReturn vatReturn) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kennzahl;Beschreibung;Betrag EUR\n");
        sb.append(";Jahr;").append(vatReturn.getYear()).append('\n');
        sb.append(";Zeitraum;").append(formatZeitraum(vatReturn.getPeriodType(),
                vatReturn.getPeriodNumber())).append('\n');
        sb.append(";Zeitraumtyp;").append(vatReturn.getPeriodType()).append('\n');
        sb.append("81;Steuerpflichtige Umsaetze 19% (Bemessungsgrundlage);")
                .append(formatAmount(netFromVat(vatReturn.getOutputVat()))).append('\n');
        sb.append(";Umsatzsteuer 19%;")
                .append(formatAmount(vatReturn.getOutputVat())).append('\n');
        sb.append("66;Vorsteuerbetraege;")
                .append(formatAmount(vatReturn.getInputVat())).append('\n');
        sb.append("83;Verbleibende USt-Vorauszahlung;")
                .append(formatAmount(vatReturn.getNetPayable())).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Internal ────────────────────────────────────────────────────────

    ElsterXml buildElsterXml(VatReturn vatReturn, String displayName) {
        ElsterXml elster = new ElsterXml();

        // Transfer Header
        ElsterXml.TransferHeader header = new ElsterXml.TransferHeader();
        header.setDatenLieferant(displayName);
        header.setErstellungsdatum(LocalDate.now().format(ELSTER_DATE));
        elster.setTransferHeader(header);

        // USt-VA Kennzahlen
        ElsterXml.Umsatzsteuervoranmeldung ustva = new ElsterXml.Umsatzsteuervoranmeldung();
        ustva.setJahr(String.valueOf(vatReturn.getYear()));
        ustva.setZeitraum(formatZeitraum(vatReturn.getPeriodType(), vatReturn.getPeriodNumber()));
        ustva.setKz81(formatAmount(netFromVat(vatReturn.getOutputVat())));
        ustva.setKz66(formatAmount(vatReturn.getInputVat()));
        ustva.setKz83(formatAmount(vatReturn.getNetPayable()));

        // Wire nested structure
        ElsterXml.Steuerfall steuerfall = new ElsterXml.Steuerfall();
        steuerfall.setUmsatzsteuervoranmeldung(ustva);

        ElsterXml.Anmeldungssteuern anmeldung = new ElsterXml.Anmeldungssteuern();
        anmeldung.setVersion(vatReturn.getYear()
                + String.format("%02d", vatReturn.getPeriodNumber()));
        anmeldung.setSteuerfall(steuerfall);

        ElsterXml.Nutzdaten nutzdaten = new ElsterXml.Nutzdaten();
        nutzdaten.setAnmeldungssteuern(anmeldung);

        ElsterXml.Empfaenger empfaenger = new ElsterXml.Empfaenger();
        ElsterXml.NutzdatenHeader nutzdatenHeader = new ElsterXml.NutzdatenHeader();
        nutzdatenHeader.setEmpfaenger(empfaenger);

        ElsterXml.Nutzdatenblock block = new ElsterXml.Nutzdatenblock();
        block.setNutzdatenHeader(nutzdatenHeader);
        block.setNutzdaten(nutzdaten);

        ElsterXml.DatenTeil datenTeil = new ElsterXml.DatenTeil();
        datenTeil.setNutzdatenblock(block);
        elster.setDatenTeil(datenTeil);

        return elster;
    }

    /**
     * ELSTER Zeitraum encoding:
     * Monthly → "01".."12", Quarterly → "41".."44", Annual → "12"
     */
    static String formatZeitraum(PeriodType type, short periodNumber) {
        return switch (type) {
            case MONTHLY -> String.format("%02d", periodNumber);
            case QUARTERLY -> String.valueOf(40 + periodNumber);
            case ANNUAL -> "12";
        };
    }

    /**
     * Back-calculate net revenue from VAT amount at standard 19% rate.
     * Kz81 requires net (Bemessungsgrundlage), not the VAT amount.
     */
    private BigDecimal netFromVat(BigDecimal vatAmount) {
        if (vatAmount == null || vatAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return vatAmount.multiply(HUNDRED)
                .divide(STANDARD_RATE, 2, RoundingMode.HALF_UP);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
