package de.dreistrom.integration.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.integration.domain.AppStorePayout;
import de.dreistrom.integration.domain.PayoutPlatform;
import de.dreistrom.integration.repository.AppStorePayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses Google Play Console earnings reports (CSV format).
 * Extracts revenue, service fee, VAT per §3 Abs. 3a UStG + §18a UStG.
 * Auto-classifies as Gewerbe income. Flags B2B reverse charge for ZM.
 *
 * Google Play earnings report CSV columns (typical):
 * Transaction Date (ISO yyyy-MM-dd), Transaction Type, Product ID, Product Title,
 * Country of Buyer, Buyer Currency, Amount (Buyer Currency),
 * Currency Conversion Rate, Merchant Currency, Amount (Merchant Currency),
 * Description
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlayPayoutImporter {

    private static final BigDecimal GOOGLE_SERVICE_FEE_RATE = new BigDecimal("0.30");
    private static final BigDecimal GOOGLE_REDUCED_FEE_RATE = new BigDecimal("0.15");
    private static final DateTimeFormatter GOOGLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AppStorePayoutRepository payoutRepository;

    /**
     * Import Google Play earnings report CSV data.
     * Returns list of imported payouts.
     */
    @Transactional
    public List<AppStorePayout> importCsv(AppUser user, String csvContent, boolean reducedFeeProgram) {
        String batchId = UUID.randomUUID().toString();
        BigDecimal feeRate = reducedFeeProgram ? GOOGLE_REDUCED_FEE_RATE : GOOGLE_SERVICE_FEE_RATE;

        List<AppStorePayout> results = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(csvContent)).build()) {
            String[] header = reader.readNext();
            if (header == null) return results;

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 10) continue;

                AppStorePayout payout = parseLine(user, line, feeRate, batchId);
                if (payout != null) {
                    results.add(payoutRepository.save(payout));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Google Play earnings report CSV", e);
            throw new IllegalArgumentException("Invalid Google Play CSV format: " + e.getMessage());
        }

        log.info("Imported {} Google Play payout records (batch {})", results.size(), batchId);
        return results;
    }

    private AppStorePayout parseLine(AppUser user, String[] fields,
                                     BigDecimal feeRate, String batchId) {
        try {
            String transactionDate = fields[0].trim();
            String transactionType = fields[1].trim();
            String productId = fields[2].trim();
            String productTitle = fields[3].trim();
            String countryOfBuyer = fields[4].trim();
            String merchantCurrency = fields[8].trim();
            String merchantAmount = fields[9].trim();

            // Only process charges (sales)
            if (!"Charge".equalsIgnoreCase(transactionType)) return null;

            LocalDate reportDate = LocalDate.parse(transactionDate, GOOGLE_DATE_FORMAT);
            BigDecimal netAmount = new BigDecimal(merchantAmount);

            // Calculate gross and service fee
            // net = gross * (1 - feeRate) → gross = net / (1 - feeRate)
            BigDecimal grossAmount = netAmount.divide(
                    BigDecimal.ONE.subtract(feeRate), 2, RoundingMode.HALF_UP);
            BigDecimal serviceFee = grossAmount.subtract(netAmount);

            // VAT: Google as marketplace handles B2C VAT under §3 Abs. 3a UStG
            // B2B reverse charge (§18a UStG) flagged via region = non-DE EU
            BigDecimal vatAmount = BigDecimal.ZERO;

            String rawLine = String.join(",", fields);

            return new AppStorePayout(user, PayoutPlatform.GOOGLE, reportDate,
                    countryOfBuyer, merchantCurrency,
                    grossAmount, serviceFee, netAmount, vatAmount,
                    productId, productTitle, 1, batchId, rawLine);
        } catch (Exception e) {
            log.warn("Skipping unparseable Google Play CSV line: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if a payout requires reverse charge reporting for ZM (§18a UStG).
     * Applies to B2B sales within EU (non-DE).
     */
    public boolean isReverseChargeRequired(AppStorePayout payout) {
        if (payout.getPlatform() != PayoutPlatform.GOOGLE) return false;
        String region = payout.getRegion();
        return region != null && !region.isEmpty()
                && !"DE".equalsIgnoreCase(region)
                && isEuCountry(region);
    }

    private boolean isEuCountry(String countryCode) {
        return EU_COUNTRIES.contains(countryCode.toUpperCase());
    }

    private static final java.util.Set<String> EU_COUNTRIES = java.util.Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );
}
