package de.dreistrom.tax.service;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.tax.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnnualTaxPdfServiceTest {

    private AnnualTaxPdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new AnnualTaxPdfService();
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

    @Test
    void generatesPdf() {
        byte[] pdf = pdfService.generate(samplePackage());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

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

        byte[] pdf = pdfService.generate(zeroPkg);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    @Test
    void handlesLossScenario() {
        AnnualTaxPackage lossPkg = new AnnualTaxPackage(
                2025,
                new AnnualTaxPackage.AnlageN(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new AnnualTaxPackage.AnlageS(
                        new BigDecimal("5000"),
                        new BigDecimal("15000"),
                        new BigDecimal("-10000"),
                        new BigDecimal("1000")
                ),
                new AnnualTaxPackage.AnlageG(
                        new BigDecimal("3000"),
                        new BigDecimal("8000"),
                        new BigDecimal("-5000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                new EuerResult(2025, IncomeStream.FREIBERUF,
                        new BigDecimal("5000"),
                        new BigDecimal("15000"),
                        BigDecimal.ZERO,
                        new BigDecimal("1000"),
                        new BigDecimal("16000"),
                        new BigDecimal("-11000")
                ),
                new EuerResult(2025, IncomeStream.GEWERBE,
                        new BigDecimal("3000"),
                        new BigDecimal("8000"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("8000"),
                        new BigDecimal("-5000")
                ),
                new AnnualTaxPackage.AnlageVorsorgeaufwand(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new TaxCalculationResult(
                        2025, BigDecimal.ZERO,
                        new BigDecimal("5000"), new BigDecimal("3000"),
                        new BigDecimal("8000"),
                        new DeductionBreakdown(
                                new BigDecimal("15000"), new BigDecimal("8000"),
                                BigDecimal.ZERO, new BigDecimal("36"),
                                new BigDecimal("23036")),
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO),
                new GewerbesteuerResult(
                        BigDecimal.ZERO, new BigDecimal("24500"), BigDecimal.ZERO,
                        new BigDecimal("0.035"), BigDecimal.ZERO, 410,
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        );

        byte[] pdf = pdfService.generate(lossPkg);

        assertThat(pdf).isNotEmpty();
    }
}
