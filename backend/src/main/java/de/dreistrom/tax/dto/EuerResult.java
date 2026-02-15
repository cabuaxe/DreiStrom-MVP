package de.dreistrom.tax.dto;

import de.dreistrom.common.domain.IncomeStream;

import java.math.BigDecimal;
import java.util.List;

/**
 * Einnahmen-Ueberschuss-Rechnung (EÜR) per §4 Abs. 3 EStG.
 * Separate P&L statement for Freiberuf or Gewerbe activity.
 * All monetary amounts in EUR.
 */
public record EuerResult(
        int taxYear,
        IncomeStream stream,

        // ── Betriebseinnahmen (Business income) ─────────────────────────
        BigDecimal totalIncome,

        // ── Betriebsausgaben (Business expenses) ────────────────────────
        BigDecimal directExpenses,
        BigDecimal allocatedSharedExpenses,
        BigDecimal depreciation,
        BigDecimal totalExpenses,

        // ── Gewinn / Verlust (Profit / Loss) ────────────────────────────
        BigDecimal profit
) {}
