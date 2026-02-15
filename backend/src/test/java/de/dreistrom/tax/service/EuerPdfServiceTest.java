package de.dreistrom.tax.service;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.tax.dto.EuerResult;
import de.dreistrom.tax.service.EuerService.DualStreamEuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EuerPdfServiceTest {

    private EuerPdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new EuerPdfService();
    }

    @Test
    void generatesSingleStreamPdf() {
        EuerResult euer = new EuerResult(
                2024, IncomeStream.FREIBERUF,
                new BigDecimal("80000"),
                new BigDecimal("15000"),
                BigDecimal.ZERO,
                new BigDecimal("2000"),
                new BigDecimal("17000"),
                new BigDecimal("63000")
        );

        byte[] pdf = pdfService.generateSingleStream(euer);

        assertThat(pdf).isNotEmpty();
        // PDF starts with %PDF magic bytes
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    @Test
    void generatesSingleStreamPdfForGewerbe() {
        EuerResult euer = new EuerResult(
                2024, IncomeStream.GEWERBE,
                new BigDecimal("45000"),
                new BigDecimal("8000"),
                BigDecimal.ZERO,
                new BigDecimal("1500"),
                new BigDecimal("9500"),
                new BigDecimal("35500")
        );

        byte[] pdf = pdfService.generateSingleStream(euer);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    @Test
    void generatesDualStreamPdf() {
        EuerResult fb = new EuerResult(
                2024, IncomeStream.FREIBERUF,
                new BigDecimal("60000"),
                new BigDecimal("10000"),
                BigDecimal.ZERO,
                new BigDecimal("1000"),
                new BigDecimal("11000"),
                new BigDecimal("49000")
        );
        EuerResult gw = new EuerResult(
                2024, IncomeStream.GEWERBE,
                new BigDecimal("40000"),
                new BigDecimal("5000"),
                BigDecimal.ZERO,
                new BigDecimal("500"),
                new BigDecimal("5500"),
                new BigDecimal("34500")
        );
        DualStreamEuer dual = new DualStreamEuer(fb, gw);

        byte[] pdf = pdfService.generateDualStream(dual);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    @Test
    void handlesLossScenario() {
        EuerResult euer = new EuerResult(
                2024, IncomeStream.GEWERBE,
                new BigDecimal("5000"),
                new BigDecimal("15000"),
                BigDecimal.ZERO,
                new BigDecimal("2000"),
                new BigDecimal("17000"),
                new BigDecimal("-12000")
        );

        byte[] pdf = pdfService.generateSingleStream(euer);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void handlesZeroValues() {
        EuerResult euer = new EuerResult(
                2024, IncomeStream.FREIBERUF,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        byte[] pdf = pdfService.generateSingleStream(euer);

        assertThat(pdf).isNotEmpty();
    }
}
