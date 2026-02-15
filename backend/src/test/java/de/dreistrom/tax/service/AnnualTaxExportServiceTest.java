package de.dreistrom.tax.service;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.tax.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnnualTaxExportServiceTest {

    private AnnualTaxExportService exportService;

    @BeforeEach
    void setUp() {
        exportService = new AnnualTaxExportService();
    }

    private AnnualTaxPackage samplePackage() {
        return new AnnualTaxPackage(
                2025,
                new AnnualTaxPackage.AnlageN(
                        new BigDecimal("55000"),
                        new BigDecimal("8500"),
                        new BigDecimal("467.50"),
                        BigDecimal.ZERO,
                        new BigDecimal("1230"),
                        BigDecimal.ZERO
                ),
                new AnnualTaxPackage.AnlageS(
                        new BigDecimal("60000"),
                        new BigDecimal("12000"),
                        new BigDecimal("48000"),
                        new BigDecimal("1500")
                ),
                new AnnualTaxPackage.AnlageG(
                        new BigDecimal("40000"),
                        new BigDecimal("8000"),
                        new BigDecimal("32000"),
                        new BigDecimal("1000"),
                        new BigDecimal("924.00"),
                        new BigDecimal("924.00")
                ),
                new EuerResult(2025, IncomeStream.FREIBERUF,
                        new BigDecimal("60000"),
                        new BigDecimal("12000"),
                        BigDecimal.ZERO,
                        new BigDecimal("1500"),
                        new BigDecimal("13500"),
                        new BigDecimal("46500")
                ),
                new EuerResult(2025, IncomeStream.GEWERBE,
                        new BigDecimal("40000"),
                        new BigDecimal("8000"),
                        BigDecimal.ZERO,
                        new BigDecimal("1000"),
                        new BigDecimal("9000"),
                        new BigDecimal("31000")
                ),
                new AnnualTaxPackage.AnlageVorsorgeaufwand(
                        new BigDecimal("4015.00"),
                        new BigDecimal("935.00"),
                        new BigDecimal("5115.00"),
                        new BigDecimal("715.00"),
                        new BigDecimal("10780.00")
                ),
                new TaxCalculationResult(
                        2025,
                        new BigDecimal("55000"),
                        new BigDecimal("60000"),
                        new BigDecimal("40000"),
                        new BigDecimal("155000"),
                        new DeductionBreakdown(
                                new BigDecimal("12000"),
                                new BigDecimal("8000"),
                                new BigDecimal("1230"),
                                new BigDecimal("36"),
                                new BigDecimal("21266")
                        ),
                        new BigDecimal("133734"),
                        new BigDecimal("42650.00"),
                        new BigDecimal("2345.75"),
                        new BigDecimal("44995.75"),
                        new BigDecimal("42.00"),
                        new BigDecimal("29.03")
                ),
                new GewerbesteuerResult(
                        new BigDecimal("32000"),
                        new BigDecimal("24500"),
                        new BigDecimal("7500"),
                        new BigDecimal("0.035"),
                        new BigDecimal("262.50"),
                        410,
                        new BigDecimal("1076.25"),
                        new BigDecimal("1050.00"),
                        new BigDecimal("26.25")
                )
        );
    }

    // ── XML ──────────────────────────────────────────────────────────────

    @Test
    void generatesElsterXml() {
        byte[] xml = exportService.generateElsterXml(samplePackage(), "Max Mustermann");

        assertThat(xml).isNotEmpty();
        String content = new String(xml);
        assertThat(content).contains("<?xml");
        assertThat(content).contains("<Elster>");
        assertThat(content).contains("<DatenArt>ESt</DatenArt>");
        assertThat(content).contains("<Jahr>2025</Jahr>");
    }

    @Test
    void xmlContainsAllAnlagen() {
        byte[] xml = exportService.generateElsterXml(samplePackage(), "Max Mustermann");
        String content = new String(xml);

        assertThat(content).contains("<AnlageN>");
        assertThat(content).contains("<AnlageS>");
        assertThat(content).contains("<AnlageG>");
        assertThat(content).contains("<AnlageEUeR_Freiberuf>");
        assertThat(content).contains("<AnlageEUeR_Gewerbe>");
        assertThat(content).contains("<AnlageVorsorgeaufwand>");
        assertThat(content).contains("<Festsetzung>");
    }

    @Test
    void xmlContainsCorrectAmounts() {
        byte[] xml = exportService.generateElsterXml(samplePackage(), "Max Mustermann");
        String content = new String(xml);

        assertThat(content).contains("<Bruttoarbeitslohn>55000.00</Bruttoarbeitslohn>");
        assertThat(content).contains("<DatenLieferant>Max Mustermann</DatenLieferant>");
    }

    // ── CSV ──────────────────────────────────────────────────────────────

    @Test
    void generatesCsv() {
        byte[] csv = exportService.generateCsv(samplePackage());

        assertThat(csv).isNotEmpty();
        String content = new String(csv);
        assertThat(content).contains("Anlage;Position;Betrag EUR");
        assertThat(content).contains(";Steuerjahr;2025");
    }

    @Test
    void csvContainsAllSections() {
        byte[] csv = exportService.generateCsv(samplePackage());
        String content = new String(csv);

        assertThat(content).contains("Anlage N;Bruttoarbeitslohn;55000.00");
        assertThat(content).contains("Anlage S;Einnahmen;60000.00");
        assertThat(content).contains("Anlage G;Einnahmen;40000.00");
        assertThat(content).contains("EUeR Freiberuf;Betriebseinnahmen;60000.00");
        assertThat(content).contains("EUeR Gewerbe;Betriebseinnahmen;40000.00");
        assertThat(content).contains("Vorsorgeaufwand;Krankenversicherung;4015.00");
        assertThat(content).contains("Festsetzung;Einkommensteuer;42650.00");
    }

    @Test
    void csvUsesCorrectSeparator() {
        byte[] csv = exportService.generateCsv(samplePackage());
        String firstLine = new String(csv).split("\n")[0];

        assertThat(firstLine).isEqualTo("Anlage;Position;Betrag EUR");
    }

    // ── Edge cases ───────────────────────────────────────────────────────

    @Test
    void handlesZeroValues() {
        AnnualTaxPackage zeroPkg = new AnnualTaxPackage(
                2025,
                new AnnualTaxPackage.AnlageN(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AnnualTaxPackage.AnlageS(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AnnualTaxPackage.AnlageG(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new EuerResult(2025, IncomeStream.FREIBERUF,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new EuerResult(2025, IncomeStream.GEWERBE,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AnnualTaxPackage.AnlageVorsorgeaufwand(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new TaxCalculationResult(
                        2025, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        new DeductionBreakdown(
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new GewerbesteuerResult(
                        BigDecimal.ZERO, new BigDecimal("24500"), BigDecimal.ZERO,
                        new BigDecimal("0.035"), BigDecimal.ZERO, 410,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        byte[] xml = exportService.generateElsterXml(zeroPkg, "Test User");
        byte[] csv = exportService.generateCsv(zeroPkg);

        assertThat(xml).isNotEmpty();
        assertThat(csv).isNotEmpty();
    }
}
