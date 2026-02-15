package de.dreistrom.tax.dto;

import java.math.BigDecimal;

public record AnnualTaxPackage(
        int taxYear,

        // ── Anlage N (Employment §19 EStG) ────────────────────────────────
        AnlageN anlageN,

        // ── Anlage S (Freiberuf §18 EStG) ─────────────────────────────────
        AnlageS anlageS,

        // ── Anlage G (Gewerbe §15 EStG) ───────────────────────────────────
        AnlageG anlageG,

        // ── Anlage EÜR (Freiberuf) ────────────────────────────────────────
        EuerResult euerFreiberuf,

        // ── Anlage EÜR (Gewerbe) ──────────────────────────────────────────
        EuerResult euerGewerbe,

        // ── Anlage Vorsorgeaufwand ─────────────────────────────────────────
        AnlageVorsorgeaufwand vorsorgeaufwand,

        // ── Tax totals ────────────────────────────────────────────────────
        TaxCalculationResult taxCalculation,
        GewerbesteuerResult gewerbesteuer
) {

    public record AnlageN(
            BigDecimal bruttoarbeitslohn,
            BigDecimal lohnsteuer,
            BigDecimal solidaritaetszuschlag,
            BigDecimal kirchensteuer,
            BigDecimal werbungskostenpauschale,
            BigDecimal fahrtkostenpauschale
    ) {}

    public record AnlageS(
            BigDecimal einnahmen,
            BigDecimal betriebsausgaben,
            BigDecimal gewinn,
            BigDecimal afaBetrag
    ) {}

    public record AnlageG(
            BigDecimal einnahmen,
            BigDecimal betriebsausgaben,
            BigDecimal gewinn,
            BigDecimal afaBetrag,
            BigDecimal gewerbesteuer,
            BigDecimal paragraph35Anrechnung
    ) {}

    public record AnlageVorsorgeaufwand(
            BigDecimal krankenversicherung,
            BigDecimal pflegeversicherung,
            BigDecimal rentenversicherung,
            BigDecimal arbeitslosenversicherung,
            BigDecimal gesamtVorsorge
    ) {}
}
