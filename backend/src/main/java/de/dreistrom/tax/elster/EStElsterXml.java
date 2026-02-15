package de.dreistrom.tax.elster;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JAXB model for ELSTER-compatible Einkommensteuererklaerung XML.
 * Covers the annual tax return forms (Anlage N, S, G, EÜR, Vorsorgeaufwand).
 */
@XmlRootElement(name = "Elster")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @Setter @NoArgsConstructor
public class EStElsterXml {

    @XmlElement(name = "TransferHeader")
    private TransferHeader transferHeader;

    @XmlElement(name = "DatenTeil")
    private DatenTeil datenTeil;

    // ── TransferHeader ──────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class TransferHeader {

        @XmlElement(name = "Verfahren")
        private String verfahren = "ElsterAnmeldung";

        @XmlElement(name = "DatenArt")
        private String datenArt = "ESt";

        @XmlElement(name = "Vorgang")
        private String vorgang = "send";

        @XmlElement(name = "Testmerker")
        private String testmerker = "700000004";

        @XmlElement(name = "HerstellerID")
        private String herstellerID = "74931";

        @XmlElement(name = "DatenLieferant")
        private String datenLieferant;

        @XmlElement(name = "Erstellungsdatum")
        private String erstellungsdatum;
    }

    // ── DatenTeil ───────────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class DatenTeil {

        @XmlElement(name = "Nutzdatenblock")
        private Nutzdatenblock nutzdatenblock;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Nutzdatenblock {

        @XmlElement(name = "NutzdatenHeader")
        private NutzdatenHeader nutzdatenHeader;

        @XmlElement(name = "Nutzdaten")
        private Nutzdaten nutzdaten;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class NutzdatenHeader {

        @XmlElement(name = "NutzdatenTicket")
        private String nutzdatenTicket = "1";

        @XmlElement(name = "Empfaenger")
        private Empfaenger empfaenger;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Empfaenger {

        @XmlAttribute(name = "id")
        private String id = "F";

        @XmlValue
        private String value = "9198";
    }

    // ── Nutzdaten ───────────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Nutzdaten {

        @XmlElement(name = "Einkommensteuer")
        private Einkommensteuer einkommensteuer;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Einkommensteuer {

        @XmlAttribute(name = "art")
        private String art = "ESt";

        @XmlAttribute(name = "version")
        private String version;

        @XmlElement(name = "Jahr")
        private String jahr;

        @XmlElement(name = "AnlageN")
        private AnlageN anlageN;

        @XmlElement(name = "AnlageS")
        private AnlageS anlageS;

        @XmlElement(name = "AnlageG")
        private AnlageG anlageG;

        @XmlElement(name = "AnlageEUeR_Freiberuf")
        private AnlageEUeR anlageEuerFreiberuf;

        @XmlElement(name = "AnlageEUeR_Gewerbe")
        private AnlageEUeR anlageEuerGewerbe;

        @XmlElement(name = "AnlageVorsorgeaufwand")
        private AnlageVorsorgeaufwand anlageVorsorgeaufwand;

        @XmlElement(name = "Festsetzung")
        private Festsetzung festsetzung;
    }

    // ── Anlage N (Employment §19 EStG) ──────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class AnlageN {

        @XmlElement(name = "Bruttoarbeitslohn")
        private String bruttoarbeitslohn;

        @XmlElement(name = "Lohnsteuer")
        private String lohnsteuer;

        @XmlElement(name = "Solidaritaetszuschlag")
        private String solidaritaetszuschlag;

        @XmlElement(name = "Kirchensteuer")
        private String kirchensteuer;

        @XmlElement(name = "Werbungskostenpauschale")
        private String werbungskostenpauschale;
    }

    // ── Anlage S (Freiberuf §18 EStG) ───────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class AnlageS {

        @XmlElement(name = "Einnahmen")
        private String einnahmen;

        @XmlElement(name = "Betriebsausgaben")
        private String betriebsausgaben;

        @XmlElement(name = "Gewinn")
        private String gewinn;

        @XmlElement(name = "AfA")
        private String afa;
    }

    // ── Anlage G (Gewerbe §15 EStG) ─────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class AnlageG {

        @XmlElement(name = "Einnahmen")
        private String einnahmen;

        @XmlElement(name = "Betriebsausgaben")
        private String betriebsausgaben;

        @XmlElement(name = "Gewinn")
        private String gewinn;

        @XmlElement(name = "AfA")
        private String afa;

        @XmlElement(name = "Gewerbesteuer")
        private String gewerbesteuer;

        @XmlElement(name = "Paragraph35Anrechnung")
        private String paragraph35Anrechnung;
    }

    // ── Anlage EÜR ──────────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class AnlageEUeR {

        @XmlElement(name = "Stream")
        private String stream;

        @XmlElement(name = "Betriebseinnahmen")
        private String betriebseinnahmen;

        @XmlElement(name = "Betriebsausgaben")
        private String betriebsausgaben;

        @XmlElement(name = "AfA")
        private String afa;

        @XmlElement(name = "AusgabenGesamt")
        private String ausgabenGesamt;

        @XmlElement(name = "Gewinn")
        private String gewinn;
    }

    // ── Anlage Vorsorgeaufwand ───────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class AnlageVorsorgeaufwand {

        @XmlElement(name = "Krankenversicherung")
        private String krankenversicherung;

        @XmlElement(name = "Pflegeversicherung")
        private String pflegeversicherung;

        @XmlElement(name = "Rentenversicherung")
        private String rentenversicherung;

        @XmlElement(name = "Arbeitslosenversicherung")
        private String arbeitslosenversicherung;

        @XmlElement(name = "GesamtVorsorge")
        private String gesamtVorsorge;
    }

    // ── Festsetzung (Tax assessment totals) ─────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Festsetzung {

        @XmlElement(name = "ZuVersteuerndesEinkommen")
        private String zuVersteuerndesEinkommen;

        @XmlElement(name = "Einkommensteuer")
        private String einkommensteuer;

        @XmlElement(name = "Solidaritaetszuschlag")
        private String solidaritaetszuschlag;

        @XmlElement(name = "Gewerbesteuer")
        private String gewerbesteuer;

        @XmlElement(name = "GesamtSteuerbelastung")
        private String gesamtSteuerbelastung;
    }
}
