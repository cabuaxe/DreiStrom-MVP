package de.dreistrom.socialinsurance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Social insurance status assessment per §5 Abs. 5 SGB V.
 * Self-employment is secondary (no mandatory insurance) if
 * employment is the main activity: >20h/week AND higher income.
 */
public record SocialInsuranceStatus(
        int year,
        BigDecimal avgEmploymentHoursWeekly,
        BigDecimal avgSelfEmployedHoursWeekly,
        BigDecimal totalEmploymentIncome,
        BigDecimal totalSelfEmployedIncome,
        boolean hoursRiskFlag,        // SE hours >20h/week
        boolean incomeRiskFlag,       // SE income > employment income
        RiskLevel riskLevel,
        String riskMessage,
        List<MonthlyBreakdown> monthlyData
) {
    public enum RiskLevel {
        SAFE,       // Employment clearly primary
        WARNING,    // One dimension near threshold
        CRITICAL    // Both dimensions indicate reclassification risk
    }

    public record MonthlyBreakdown(
            int month,
            BigDecimal employmentHoursWeekly,
            BigDecimal selfEmployedHoursWeekly,
            BigDecimal employmentIncome,
            BigDecimal selfEmployedIncome
    ) {}
}
