package de.dreistrom.workingtime.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklySummary(
        LocalDate weekStart,
        LocalDate weekEnd,
        BigDecimal employmentHours,
        BigDecimal freiberufHours,
        BigDecimal gewerbeHours,
        BigDecimal totalHours,
        BigDecimal selfEmployedHours
) {}
