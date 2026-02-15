package de.dreistrom.tax.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a tax assessment is computed for a user and year.
 * Consumers can use this to update TaxPeriod estimated_tax_cents
 * or trigger dashboard refreshes.
 */
@Getter
public class TaxCalculated {

    private final Long userId;
    private final int taxYear;
    private final BigDecimal totalTax;
    private final BigDecimal effectiveRate;
    private final Instant occurredAt;

    public TaxCalculated(Long userId, int taxYear,
                         BigDecimal totalTax, BigDecimal effectiveRate) {
        this.userId = userId;
        this.taxYear = taxYear;
        this.totalTax = totalTax;
        this.effectiveRate = effectiveRate;
        this.occurredAt = Instant.now();
    }
}
