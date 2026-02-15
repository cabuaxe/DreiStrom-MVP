package de.dreistrom.vat.elster;

import jakarta.xml.bind.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JAXB model for ERiC-compatible ELSTER XML (Umsatzsteuervoranmeldung).
 * Structure follows the ELSTER XML schema v11.
 */
@XmlRootElement(name = "Elster")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @Setter @NoArgsConstructor
public class ElsterXml {

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
        private String datenArt = "UStVA";

        @XmlElement(name = "Vorgang")
        private String vorgang = "send";

        @XmlElement(name = "Testmerker")
        private String testmerker = "700000004";

        @XmlElement(name = "HerstellerID")
        private String herstellerID = "74931";

        @XmlElement(name = "DatenLieferant")
        private String datenLieferant;

        @XmlElement(name = "Eingangsdatum")
        private String eingangsdatum = "";

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

        @XmlElement(name = "Anmeldungssteuern")
        private Anmeldungssteuern anmeldungssteuern;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Anmeldungssteuern {

        @XmlAttribute(name = "art")
        private String art = "UStVA";

        @XmlAttribute(name = "version")
        private String version;

        @XmlElement(name = "Steuerfall")
        private Steuerfall steuerfall;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Steuerfall {

        @XmlElement(name = "Umsatzsteuervoranmeldung")
        private Umsatzsteuervoranmeldung umsatzsteuervoranmeldung;
    }

    // ── USt-VA Kennzahlen ───────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Getter @Setter @NoArgsConstructor
    public static class Umsatzsteuervoranmeldung {

        @XmlElement(name = "Jahr")
        private String jahr;

        @XmlElement(name = "Zeitraum")
        private String zeitraum;

        /** Kz81: Steuerpflichtige Lieferungen/Leistungen zum Steuersatz 19% (Bemessungsgrundlage) */
        @XmlElement(name = "Kz81")
        private String kz81;

        /** Kz86: Steuerpflichtige Lieferungen/Leistungen zum Steuersatz 7% (Bemessungsgrundlage) */
        @XmlElement(name = "Kz86")
        private String kz86;

        /** Kz66: Vorsteuerbeträge aus Rechnungen von anderen Unternehmern */
        @XmlElement(name = "Kz66")
        private String kz66;

        /** Kz83: Verbleibende Umsatzsteuer-Vorauszahlung / Überschuss */
        @XmlElement(name = "Kz83")
        private String kz83;
    }
}
