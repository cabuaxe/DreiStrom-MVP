package de.dreistrom.tax.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Annual advance tax payment schedule with deviation analysis.
 */
public record VorauszahlungSchedule(
        int year,
        BigDecimal assessmentBasis,
        BigDecimal quarterlyAmount,
        BigDecimal annualTotal,
        List<QuarterPayment> payments,
        AdjustmentSuggestion adjustmentSuggestion
) {

    public record QuarterPayment(
            int quarter,
            String dueDate,
            BigDecimal amount,
            BigDecimal paid,
            String status
    ) {}

    /**
     * Suggested adjustment when actual income deviates >25% from assessment basis.
     */
    public record AdjustmentSuggestion(
            boolean recommended,
            BigDecimal actualIncome,
            BigDecimal assessmentBasis,
            BigDecimal deviationPercent,
            BigDecimal suggestedQuarterlyAmount
    ) {
        public static AdjustmentSuggestion none() {
            return new AdjustmentSuggestion(false,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
