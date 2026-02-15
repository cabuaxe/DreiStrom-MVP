package de.dreistrom.vat.dto;

import java.math.BigDecimal;

/**
 * Result of a VAT calculation for a given user and period.
 * All amounts in EUR.
 */
public record VatSummary(
        BigDecimal outputVat,
        BigDecimal freiberufOutputVat,
        BigDecimal gewerbeOutputVat,
        BigDecimal inputVat,
        BigDecimal freiberufInputVat,
        BigDecimal gewerbeInputVat,
        BigDecimal netPayable,
        boolean kleinunternehmer
) {
    public static VatSummary zero() {
        return new VatSummary(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, true);
    }
}
