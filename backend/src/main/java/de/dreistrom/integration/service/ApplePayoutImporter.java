package de.dreistrom.integration.service;

import com.opencsv.CSVParserBuilder;
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

import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses Apple App Store Connect financial reports (CSV format).
 * Extracts revenue, commission, VAT per §3 Abs. 3a UStG marketplace rules.
 * Auto-classifies as Gewerbe income.
 *
 * Apple financial report CSV columns (typical):
 * Start Date, End Date, UPC, ISRC/ISBN, Vendor Identifier, Quantity,
 * Partner Share, Extended Partner Share, Partner Share Currency,
 * Sales or Return, Apple Identifier, Artist/Show/Developer/Author,
 * Title, Label/Studio/Network/Developer/Publisher, Grid, Product Type Identifier,
 * ISAN/Other Identifier, Country Of Sale, Pre-order Flag, Promo Code,
 * Customer Price, Customer Currency
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplePayoutImporter {

    private static final BigDecimal APPLE_COMMISSION_RATE = new BigDecimal("0.30");
    private static final BigDecimal APPLE_SMALL_BUSINESS_RATE = new BigDecimal("0.15");
    private static final DateTimeFormatter APPLE_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final AppStorePayoutRepository payoutRepository;

    /**
     * Import Apple financial report CSV data.
     * Returns list of imported payouts.
     */
    @Transactional
    public List<AppStorePayout> importCsv(AppUser user, String csvContent, boolean smallBusinessProgram) {
        String batchId = UUID.randomUUID().toString();
        BigDecimal commissionRate = smallBusinessProgram ? APPLE_SMALL_BUSINESS_RATE : APPLE_COMMISSION_RATE;

        List<AppStorePayout> results = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(csvContent))
                .withCSVParser(new CSVParserBuilder().withSeparator('\t').build())
                .build()) {

            String[] header = reader.readNext(); // Skip header
            if (header == null) return results;

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 17) continue;

                AppStorePayout payout = parseLine(user, line, commissionRate, batchId);
                if (payout != null) {
                    results.add(payoutRepository.save(payout));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Apple financial report CSV", e);
            throw new IllegalArgumentException("Invalid Apple CSV format: " + e.getMessage());
        }

        log.info("Imported {} Apple payout records (batch {})", results.size(), batchId);
        return results;
    }

    private AppStorePayout parseLine(AppUser user, String[] fields,
                                     BigDecimal commissionRate, String batchId) {
        try {
            LocalDate startDate = LocalDate.parse(fields[0].trim(), APPLE_DATE_FORMAT);
            String quantity = fields[5].trim();
            String partnerShare = fields[7].trim(); // Extended Partner Share
            String currency = fields[8].trim();
            String salesOrReturn = fields[9].trim();
            String productId = fields[1].trim(); // UPC
            String title = fields[13].trim();
            String countryOfSale = fields[17].trim();

            // Skip returns
            if (!"S".equalsIgnoreCase(salesOrReturn)) return null;

            int qty = Integer.parseInt(quantity);
            BigDecimal netAmount = new BigDecimal(partnerShare);

            // Calculate gross and commission from net (partner share)
            // net = gross * (1 - commissionRate) → gross = net / (1 - commissionRate)
            BigDecimal grossAmount = netAmount.divide(
                    BigDecimal.ONE.subtract(commissionRate), 2, RoundingMode.HALF_UP);
            BigDecimal commissionAmount = grossAmount.subtract(netAmount);

            // VAT: Apple as marketplace handles B2C VAT under §3 Abs. 3a UStG
            // Developer receives net-of-VAT, VAT = 0 for developer's books
            BigDecimal vatAmount = BigDecimal.ZERO;

            String rawLine = String.join("\t", fields);

            return new AppStorePayout(user, PayoutPlatform.APPLE, startDate,
                    countryOfSale, currency,
                    grossAmount, commissionAmount, netAmount, vatAmount,
                    productId, title, qty, batchId, rawLine);
        } catch (Exception e) {
            log.warn("Skipping unparseable Apple CSV line: {}", e.getMessage());
            return null;
        }
    }
}
