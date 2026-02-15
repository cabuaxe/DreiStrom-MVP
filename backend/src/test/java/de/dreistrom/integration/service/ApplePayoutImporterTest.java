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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplePayoutImporterTest {

    @Mock
    private AppStorePayoutRepository payoutRepository;

    @InjectMocks
    private ApplePayoutImporter importer;

    private final AppUser user = new AppUser("dev@dreistrom.de", "hash", "Developer");

    // Tab-separated Apple financial report format
    private static final String HEADER = "Start Date\tEnd Date\tUPC\tISRC\tVendor ID\tQuantity\t" +
            "Partner Share\tExtended Partner Share\tPartner Share Currency\t" +
            "Sales or Return\tApple ID\tArtist\tDeveloper\tTitle\tLabel\t" +
            "Product Type\tISAN\tCountry Of Sale\tPre-order\tPromo Code\t" +
            "Customer Price\tCustomer Currency";

    private static final String SALE_LINE = "02/10/2026\t02/10/2026\tUPC001\t\tVND001\t5\t" +
            "3.50\t3.50\tEUR\tS\tAPP001\tDev Studio\tDev Studio\tMy App\tDev Studio\t" +
            "1\t\tDE\t\t\t4.99\tEUR";

    private static final String RETURN_LINE = "02/10/2026\t02/10/2026\tUPC001\t\tVND001\t-1\t" +
            "-0.70\t-0.70\tEUR\tR\tAPP001\tDev Studio\tDev Studio\tMy App\tDev Studio\t" +
            "1\t\tDE\t\t\t4.99\tEUR";

    @Nested
    class Import {
        @Test
        void parsesSaleLines() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + SALE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).hasSize(1);
            AppStorePayout payout = results.get(0);
            assertThat(payout.getPlatform()).isEqualTo(PayoutPlatform.APPLE);
            assertThat(payout.getRegion()).isEqualTo("DE");
            assertThat(payout.getCurrency()).isEqualTo("EUR");
            assertThat(payout.getNetRevenue()).isEqualByComparingTo("3.50");
            assertThat(payout.getQuantity()).isEqualTo(5);
            assertThat(payout.getProductName()).isEqualTo("My App");
        }

        @Test
        void skipsReturnLines() {
            String csv = HEADER + "\n" + RETURN_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).isEmpty();
        }

        @Test
        void calculatesCommissionAt30Percent() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + SALE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            AppStorePayout payout = results.get(0);
            // gross = 3.50 / 0.70 = 5.00, commission = 5.00 - 3.50 = 1.50
            assertThat(payout.getGrossRevenue()).isEqualByComparingTo("5.00");
            assertThat(payout.getCommission()).isEqualByComparingTo("1.50");
        }

        @Test
        void appliesSmallBusinessRate15Percent() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + SALE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, true);

            AppStorePayout payout = results.get(0);
            // gross = 3.50 / 0.85 = 4.12, commission = 4.12 - 3.50 = 0.62
            assertThat(payout.getGrossRevenue()).isEqualByComparingTo("4.12");
            assertThat(payout.getCommission()).isEqualByComparingTo("0.62");
        }

        @Test
        void setsVatToZeroForDeveloper() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String csv = HEADER + "\n" + SALE_LINE;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            // Under §3 Abs. 3a UStG, Apple handles B2C VAT
            assertThat(results.get(0).getVat()).isEqualByComparingTo("0");
        }

        @Test
        void handlesEmptyFile() {
            String csv = "";
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).isEmpty();
        }

        @Test
        void handlesMultipleLines() {
            when(payoutRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            String line2 = "02/11/2026\t02/11/2026\tUPC002\t\tVND001\t10\t" +
                    "7.00\t7.00\tEUR\tS\tAPP002\tDev Studio\tDev Studio\tApp 2\tDev Studio\t" +
                    "1\t\tUS\t\t\t9.99\tUSD";

            String csv = HEADER + "\n" + SALE_LINE + "\n" + line2;
            List<AppStorePayout> results = importer.importCsv(user, csv, false);

            assertThat(results).hasSize(2);
        }
    }
}
