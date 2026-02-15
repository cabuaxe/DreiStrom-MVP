package de.dreistrom.tax.dto;

import java.math.BigDecimal;

/**
 * Result of a Gewerbesteuer (trade tax) calculation.
 * All monetary amounts in EUR.
 */
public record GewerbesteuerResult(
        BigDecimal gewerbeProfit,
        BigDecimal freibetrag,
        BigDecimal taxableProfit,
        BigDecimal steuermesszahl,
        BigDecimal steuermessbetrag,
        int hebesatz,
        BigDecimal gewerbesteuer,
        BigDecimal paragraph35Credit,
        BigDecimal netGewerbesteuer
) {}
