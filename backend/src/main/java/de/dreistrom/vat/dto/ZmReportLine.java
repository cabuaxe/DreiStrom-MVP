package de.dreistrom.vat.dto;

import java.math.BigDecimal;

/**
 * A single line in the Zusammenfassende Meldung (ZM) report.
 * Aggregates net revenue by country and USt-IdNr per §18a UStG.
 */
public record ZmReportLine(
        String country,
        String ustIdNr,
        String clientName,
        BigDecimal netTotal,
        int invoiceCount
) {}
