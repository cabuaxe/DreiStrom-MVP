package de.dreistrom.vat.dto;

import java.math.BigDecimal;

/**
 * Kleinunternehmer (§19 UStG) threshold status for dashboard widget.
 */
public record KleinunternehmerStatus(
        int year,
        BigDecimal currentRevenue,
        BigDecimal currentYearLimit,
        BigDecimal currentRatio,
        BigDecimal projectedRevenue,
        BigDecimal projectedYearLimit,
        BigDecimal projectedRatio,
        boolean currentExceeded,
        boolean projectedExceeded
) {}
