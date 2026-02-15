package de.dreistrom.invoicing.domain;

/**
 * VAT treatment options per German tax law.
 */
public enum VatTreatment {
    /** Regelbesteuerung – standard VAT */
    REGULAR,
    /** Reverse-Charge – VAT liability shifts to recipient */
    REVERSE_CHARGE,
    /** Kleinunternehmerregelung §19 UStG – no VAT charged */
    SMALL_BUSINESS,
    /** Innergemeinschaftliche Lieferung/Leistung – EU cross-border */
    INTRA_EU,
    /** Drittland – non-EU, no VAT per §3a UStG */
    THIRD_COUNTRY
}
