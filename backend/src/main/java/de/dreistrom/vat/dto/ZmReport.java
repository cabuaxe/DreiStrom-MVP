package de.dreistrom.vat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Zusammenfassende Meldung (ZM) report for a reporting period.
 * Required under §18a UStG for EU B2B reverse charge transactions.
 */
public record ZmReport(
        LocalDate periodFrom,
        LocalDate periodTo,
        List<ZmReportLine> lines,
        BigDecimal totalNet,
        int totalInvoices
) {}
