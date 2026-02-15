package de.dreistrom.integration.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.integration.domain.AppStorePayout;
import de.dreistrom.integration.domain.PayoutPlatform;
import de.dreistrom.integration.repository.AppStorePayoutRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GooglePlayPayoutImporterTest {

    @Mock
    private AppStorePayoutRepository payoutRepository;

    @InjectMocks
    private GooglePlayPayoutImporter importer;

    private final AppUser user = new AppUser("dev@dreistrom.de", "hash", "Developer");

    private static final String HEADER = "Transaction Date,Transaction Type,Product ID,Product Title," +
            "Country of Buyer,Buyer Currency,Amount (Buyer Currency)," +
            "Currency Conversion Rate,Merchant Currency,Amount (Merchant Currency),Description";

    private static final String CHARGE_LINE = "2026-02-10,Charge,com.app.premium,Premium Upgrade," +
            "DE,EUR,4.99,1.0,EUR,6.99,Subscription";

    private static final String REFUND_LINE = "2026-02-10,Charge refund,com.app.premium,Premium Upgrade," +
            "DE,EUR,-4.99,1.0,EUR,-6.99,Refund";

    @Nested
    class Import {
        @Test
        void parsesChargeLines() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + CHARGE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).hasSize(1);
            AppStorePayout payout = results.get(0);
            assertThat(payout.getPlatform()).isEqualTo(PayoutPlatform.GOOGLE);
            assertThat(payout.getRegion()).isEqualTo("DE");
            assertThat(payout.getNetRevenue()).isEqualByComparingTo("6.99");
            assertThat(payout.getProductName()).isEqualTo("Premium Upgrade");
        }

        @Test
        void skipsRefundLines() {
            String csv = HEADER + "\n" + REFUND_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).isEmpty();
        }

        @Test
        void calculatesServiceFeeAt30Percent() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + CHARGE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            AppStorePayout payout = results.get(0);
            // gross = 6.99 / 0.70 = 9.99, fee = 9.99 - 6.99 = 3.00
            assertThat(payout.getGrossRevenue()).isEqualByComparingTo("9.99");
            assertThat(payout.getCommission()).isEqualByComparingTo("3.00");
        }

        @Test
        void appliesReducedFee15Percent() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + CHARGE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, true);

            AppStorePayout payout = results.get(0);
            // gross = 6.99 / 0.85 = 8.22, fee = 8.22 - 6.99 = 1.23
            assertThat(payout.getGrossRevenue()).isEqualByComparingTo("8.22");
            assertThat(payout.getCommission()).isEqualByComparingTo("1.23");
        }

        @Test
        void setsVatToZero() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + CHARGE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results.get(0).getVat()).isEqualByComparingTo("0");
        }

        @Test
        void handlesEmptyFile() {
            List<AppStorePayout> results = importer.importCsv(user, "", false);
            assertThat(results).isEmpty();
        }
    }

    @Nested
    class ReverseCharge {
        @Test
        void flagsEuNonDeCountry() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Use FR (France) as EU non-DE country
            String frLine = "2026-02-10,Charge,com.app.premium,Premium Upgrade," +
                    "FR,EUR,4.99,1.0,EUR,6.99,Subscription";
            String csv = HEADER + "\n" + frLine;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(importer.isReverseChargeRequired(results.get(0))).isTrue();
        }

        @Test
        void doesNotFlagDeCountry() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + CHARGE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(importer.isReverseChargeRequired(results.get(0))).isFalse();
        }

        @Test
        void doesNotFlagNonEuCountry() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String usLine = "2026-02-10,Charge,com.app.premium,Premium Upgrade," +
                    "US,USD,4.99,0.92,EUR,4.59,Subscription";
            String csv = HEADER + "\n" + usLine;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(importer.isReverseChargeRequired(results.get(0))).isFalse();
        }
    }
}
