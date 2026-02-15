package de.dreistrom.integration.dto;

import de.dreistrom.integration.domain.AppStorePayout;
import de.dreistrom.integration.domain.PayoutPlatform;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayoutResponse(
        Long id,
        PayoutPlatform platform,
        LocalDate reportDate,
        String region,
        String currency,
        BigDecimal grossRevenue,
        BigDecimal commission,
        BigDecimal netRevenue,
        BigDecimal vat,
        String productId,
        String productName,
        int quantity,
        String importBatchId
) {
    public static PayoutResponse from(AppStorePayout payout) {
        return new PayoutResponse(
                payout.getId(),
                payout.getPlatform(),
                payout.getReportDate(),
                payout.getRegion(),
                payout.getCurrency(),
                payout.getGrossRevenue(),
                payout.getCommission(),
                payout.getNetRevenue(),
                payout.getVat(),
                payout.getProductId(),
                payout.getProductName(),
                payout.getQuantity(),
                payout.getImportBatchId()
        );
    }
}
