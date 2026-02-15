package de.dreistrom.tax.service;

import de.dreistrom.tax.dto.AnnualTaxPackage;
import de.dreistrom.tax.dto.EuerResult;
import de.dreistrom.tax.dto.GewerbesteuerResult;
import de.dreistrom.tax.dto.TaxCalculationResult;
import de.dreistrom.tax.elster.EStElsterXml;
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
 * Generates ELSTER-compatible XML and CSV exports for the annual
 * Einkommensteuererklaerung (income tax return).
 */
@Service
public class AnnualTaxExportService {

    private static final DateTimeFormatter ELSTER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JAXBContext jaxbContext;

    public AnnualTaxExportService() {
        try {
            this.jaxbContext = JAXBContext.newInstance(EStElsterXml.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialise JAXB context for ESt", e);
        }
    }

    /**
     * Generate ELSTER-compatible XML for the annual tax return.
     *
     * @param pkg         the assembled annual tax package
     * @param displayName the taxpayer's name
     * @return UTF-8 encoded XML bytes
     */
    public byte[] generateElsterXml(AnnualTaxPackage pkg, String displayName) {
        EStElsterXml elster = buildElsterXml(pkg, displayName);

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

            StringWriter writer = new StringWriter();
            marshaller.marshal(elster, writer);
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal ESt ELSTER XML", e);
        }
    }

    /**
     * Generate semicolon-separated CSV for Steuerberater.
     */
    public byte[] generateCsv(AnnualTaxPackage pkg) {
        StringBuilder sb = new StringBuilder();
        sb.append("Anlage;Position;Betrag EUR\n");
        sb.append(";Steuerjahr;").append(pkg.taxYear()).append('\n');
        sb.append('\n');

        // Anlage N
        AnnualTaxPackage.AnlageN n = pkg.anlageN();
        sb.append("Anlage N;Bruttoarbeitslohn;").append(fmt(n.bruttoarbeitslohn())).append('\n');
        sb.append("Anlage N;Lohnsteuer;").append(fmt(n.lohnsteuer())).append('\n');
        sb.append("Anlage N;Solidaritaetszuschlag;").append(fmt(n.solidaritaetszuschlag())).append('\n');
        sb.append("Anlage N;Kirchensteuer;").append(fmt(n.kirchensteuer())).append('\n');
        sb.append("Anlage N;Werbungskostenpauschale;").append(fmt(n.werbungskostenpauschale())).append('\n');
        sb.append("Anlage N;Fahrtkostenpauschale;").append(fmt(n.fahrtkostenpauschale())).append('\n');
        sb.append('\n');

        // Anlage S
        AnnualTaxPackage.AnlageS s = pkg.anlageS();
        sb.append("Anlage S;Einnahmen;").append(fmt(s.einnahmen())).append('\n');
        sb.append("Anlage S;Betriebsausgaben;").append(fmt(s.betriebsausgaben())).append('\n');
        sb.append("Anlage S;Gewinn;").append(fmt(s.gewinn())).append('\n');
        sb.append("Anlage S;AfA;").append(fmt(s.afaBetrag())).append('\n');
        sb.append('\n');

        // Anlage G
        AnnualTaxPackage.AnlageG g = pkg.anlageG();
        sb.append("Anlage G;Einnahmen;").append(fmt(g.einnahmen())).append('\n');
        sb.append("Anlage G;Betriebsausgaben;").append(fmt(g.betriebsausgaben())).append('\n');
        sb.append("Anlage G;Gewinn;").append(fmt(g.gewinn())).append('\n');
        sb.append("Anlage G;AfA;").append(fmt(g.afaBetrag())).append('\n');
        sb.append("Anlage G;Gewerbesteuer;").append(fmt(g.gewerbesteuer())).append('\n');
        sb.append("Anlage G;§35 Anrechnung;").append(fmt(g.paragraph35Anrechnung())).append('\n');
        sb.append('\n');

        // EÜR Freiberuf
        EuerResult fb = pkg.euerFreiberuf();
        sb.append("EUeR Freiberuf;Betriebseinnahmen;").append(fmt(fb.totalIncome())).append('\n');
        sb.append("EUeR Freiberuf;Betriebsausgaben;").append(fmt(fb.directExpenses())).append('\n');
        sb.append("EUeR Freiberuf;AfA;").append(fmt(fb.depreciation())).append('\n');
        sb.append("EUeR Freiberuf;Ausgaben gesamt;").append(fmt(fb.totalExpenses())).append('\n');
        sb.append("EUeR Freiberuf;Gewinn;").append(fmt(fb.profit())).append('\n');
        sb.append('\n');

        // EÜR Gewerbe
        EuerResult gw = pkg.euerGewerbe();
        sb.append("EUeR Gewerbe;Betriebseinnahmen;").append(fmt(gw.totalIncome())).append('\n');
        sb.append("EUeR Gewerbe;Betriebsausgaben;").append(fmt(gw.directExpenses())).append('\n');
        sb.append("EUeR Gewerbe;AfA;").append(fmt(gw.depreciation())).append('\n');
        sb.append("EUeR Gewerbe;Ausgaben gesamt;").append(fmt(gw.totalExpenses())).append('\n');
        sb.append("EUeR Gewerbe;Gewinn;").append(fmt(gw.profit())).append('\n');
        sb.append('\n');

        // Vorsorgeaufwand
        AnnualTaxPackage.AnlageVorsorgeaufwand v = pkg.vorsorgeaufwand();
        sb.append("Vorsorgeaufwand;Krankenversicherung;").append(fmt(v.krankenversicherung())).append('\n');
        sb.append("Vorsorgeaufwand;Pflegeversicherung;").append(fmt(v.pflegeversicherung())).append('\n');
        sb.append("Vorsorgeaufwand;Rentenversicherung;").append(fmt(v.rentenversicherung())).append('\n');
        sb.append("Vorsorgeaufwand;Arbeitslosenversicherung;").append(fmt(v.arbeitslosenversicherung())).append('\n');
        sb.append("Vorsorgeaufwand;Gesamt;").append(fmt(v.gesamtVorsorge())).append('\n');
        sb.append('\n');

        // Tax totals
        TaxCalculationResult tax = pkg.taxCalculation();
        sb.append("Festsetzung;Zu versteuerndes Einkommen;").append(fmt(tax.taxableIncome())).append('\n');
        sb.append("Festsetzung;Einkommensteuer;").append(fmt(tax.incomeTax())).append('\n');
        sb.append("Festsetzung;Solidaritaetszuschlag;").append(fmt(tax.solidaritaetszuschlag())).append('\n');

        GewerbesteuerResult gewSt = pkg.gewerbesteuer();
        sb.append("Festsetzung;Gewerbesteuer;").append(fmt(gewSt.gewerbesteuer())).append('\n');

        BigDecimal totalBurden = tax.totalTax().add(gewSt.netGewerbesteuer());
        sb.append("Festsetzung;Gesamtsteuerbelastung;").append(fmt(totalBurden)).append('\n');

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ── Internal XML builder ─────────────────────────────────────────────

    EStElsterXml buildElsterXml(AnnualTaxPackage pkg, String displayName) {
        EStElsterXml elster = new EStElsterXml();

        // Transfer Header
        EStElsterXml.TransferHeader header = new EStElsterXml.TransferHeader();
        header.setDatenLieferant(displayName);
        header.setErstellungsdatum(LocalDate.now().format(ELSTER_DATE));
        elster.setTransferHeader(header);

        // Einkommensteuer
        EStElsterXml.Einkommensteuer est = new EStElsterXml.Einkommensteuer();
        est.setVersion(String.valueOf(pkg.taxYear()));
        est.setJahr(String.valueOf(pkg.taxYear()));

        // Anlage N
        EStElsterXml.AnlageN xmlN = new EStElsterXml.AnlageN();
        xmlN.setBruttoarbeitslohn(fmt(pkg.anlageN().bruttoarbeitslohn()));
        xmlN.setLohnsteuer(fmt(pkg.anlageN().lohnsteuer()));
        xmlN.setSolidaritaetszuschlag(fmt(pkg.anlageN().solidaritaetszuschlag()));
        xmlN.setKirchensteuer(fmt(pkg.anlageN().kirchensteuer()));
        xmlN.setWerbungskostenpauschale(fmt(pkg.anlageN().werbungskostenpauschale()));
        est.setAnlageN(xmlN);

        // Anlage S
        EStElsterXml.AnlageS xmlS = new EStElsterXml.AnlageS();
        xmlS.setEinnahmen(fmt(pkg.anlageS().einnahmen()));
        xmlS.setBetriebsausgaben(fmt(pkg.anlageS().betriebsausgaben()));
        xmlS.setGewinn(fmt(pkg.anlageS().gewinn()));
        xmlS.setAfa(fmt(pkg.anlageS().afaBetrag()));
        est.setAnlageS(xmlS);

        // Anlage G
        EStElsterXml.AnlageG xmlG = new EStElsterXml.AnlageG();
        xmlG.setEinnahmen(fmt(pkg.anlageG().einnahmen()));
        xmlG.setBetriebsausgaben(fmt(pkg.anlageG().betriebsausgaben()));
        xmlG.setGewinn(fmt(pkg.anlageG().gewinn()));
        xmlG.setAfa(fmt(pkg.anlageG().afaBetrag()));
        xmlG.setGewerbesteuer(fmt(pkg.anlageG().gewerbesteuer()));
        xmlG.setParagraph35Anrechnung(fmt(pkg.anlageG().paragraph35Anrechnung()));
        est.setAnlageG(xmlG);

        // Anlage EÜR Freiberuf
        est.setAnlageEuerFreiberuf(buildXmlEuer(pkg.euerFreiberuf()));

        // Anlage EÜR Gewerbe
        est.setAnlageEuerGewerbe(buildXmlEuer(pkg.euerGewerbe()));

        // Anlage Vorsorgeaufwand
        EStElsterXml.AnlageVorsorgeaufwand xmlV = new EStElsterXml.AnlageVorsorgeaufwand();
        xmlV.setKrankenversicherung(fmt(pkg.vorsorgeaufwand().krankenversicherung()));
        xmlV.setPflegeversicherung(fmt(pkg.vorsorgeaufwand().pflegeversicherung()));
        xmlV.setRentenversicherung(fmt(pkg.vorsorgeaufwand().rentenversicherung()));
        xmlV.setArbeitslosenversicherung(fmt(pkg.vorsorgeaufwand().arbeitslosenversicherung()));
        xmlV.setGesamtVorsorge(fmt(pkg.vorsorgeaufwand().gesamtVorsorge()));
        est.setAnlageVorsorgeaufwand(xmlV);

        // Festsetzung (tax assessment totals)
        EStElsterXml.Festsetzung festsetzung = new EStElsterXml.Festsetzung();
        festsetzung.setZuVersteuerndesEinkommen(fmt(pkg.taxCalculation().taxableIncome()));
        festsetzung.setEinkommensteuer(fmt(pkg.taxCalculation().incomeTax()));
        festsetzung.setSolidaritaetszuschlag(fmt(pkg.taxCalculation().solidaritaetszuschlag()));
        festsetzung.setGewerbesteuer(fmt(pkg.gewerbesteuer().gewerbesteuer()));
        BigDecimal totalBurden = pkg.taxCalculation().totalTax()
                .add(pkg.gewerbesteuer().netGewerbesteuer());
        festsetzung.setGesamtSteuerbelastung(fmt(totalBurden));
        est.setFestsetzung(festsetzung);

        // Wire nested structure
        EStElsterXml.Nutzdaten nutzdaten = new EStElsterXml.Nutzdaten();
        nutzdaten.setEinkommensteuer(est);

        EStElsterXml.Empfaenger empfaenger = new EStElsterXml.Empfaenger();
        EStElsterXml.NutzdatenHeader nutzdatenHeader = new EStElsterXml.NutzdatenHeader();
        nutzdatenHeader.setEmpfaenger(empfaenger);

        EStElsterXml.Nutzdatenblock block = new EStElsterXml.Nutzdatenblock();
        block.setNutzdatenHeader(nutzdatenHeader);
        block.setNutzdaten(nutzdaten);

        EStElsterXml.DatenTeil datenTeil = new EStElsterXml.DatenTeil();
        datenTeil.setNutzdatenblock(block);
        elster.setDatenTeil(datenTeil);

        return elster;
    }

    private EStElsterXml.AnlageEUeR buildXmlEuer(EuerResult euer) {
        EStElsterXml.AnlageEUeR xml = new EStElsterXml.AnlageEUeR();
        xml.setStream(euer.stream().name());
        xml.setBetriebseinnahmen(fmt(euer.totalIncome()));
        xml.setBetriebsausgaben(fmt(euer.directExpenses()));
        xml.setAfa(fmt(euer.depreciation()));
        xml.setAusgabenGesamt(fmt(euer.totalExpenses()));
        xml.setGewinn(fmt(euer.profit()));
        return xml;
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
