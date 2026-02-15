package de.dreistrom.invoicing.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.invoicing.domain.VatTreatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReverseChargeServiceTest {

    private ReverseChargeService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new ReverseChargeService();
        user = new AppUser("test@dreistrom.de", "pw", "Tester");
    }

    @Nested
    class DetermineVatTreatment {

        @Test
        void deClient_returnsRegular() {
            Client client = new Client(user, "DE GmbH", IncomeStream.FREIBERUF,
                    ClientType.B2B, "DE", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void euB2B_withUstIdNr_returnsReverseCharge() {
            Client client = new Client(user, "Austrian Corp", IncomeStream.FREIBERUF,
                    ClientType.B2B, "AT", "ATU12345678");
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.REVERSE_CHARGE);
        }

        @Test
        void euB2B_withoutUstIdNr_returnsRegular() {
            Client client = new Client(user, "French SME", IncomeStream.FREIBERUF,
                    ClientType.B2B, "FR", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void euB2C_returnsRegular() {
            Client client = new Client(user, "Private Person", IncomeStream.FREIBERUF,
                    ClientType.B2C, "IT", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void nonEU_returnsThirdCountry() {
            Client client = new Client(user, "US Corp", IncomeStream.FREIBERUF,
                    ClientType.B2B, "US", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.THIRD_COUNTRY);
        }

        @Test
        void switzerland_returnsThirdCountry() {
            Client client = new Client(user, "Swiss AG", IncomeStream.FREIBERUF,
                    ClientType.B2B, "CH", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.THIRD_COUNTRY);
        }

        @Test
        void uk_returnsThirdCountry() {
            Client client = new Client(user, "London Ltd", IncomeStream.FREIBERUF,
                    ClientType.B2B, "GB", null);
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.THIRD_COUNTRY);
        }

        @Test
        void nullClient_returnsRegular() {
            assertThat(service.determineVatTreatment(null)).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void ireland_b2b_withUstIdNr_returnsReverseCharge() {
            Client client = new Client(user, "Apple Distribution International",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE9700053D");
            assertThat(service.determineVatTreatment(client)).isEqualTo(VatTreatment.REVERSE_CHARGE);
        }
    }

    @Nested
    class ZmReportable {

        @Test
        void euB2B_reverseCharge_isZmReportable() {
            Client client = new Client(user, "Austrian Corp", IncomeStream.FREIBERUF,
                    ClientType.B2B, "AT", "ATU12345678");
            assertThat(service.isZmReportable(client, VatTreatment.REVERSE_CHARGE)).isTrue();
        }

        @Test
        void deClient_regular_notZmReportable() {
            Client client = new Client(user, "DE GmbH", IncomeStream.FREIBERUF);
            assertThat(service.isZmReportable(client, VatTreatment.REGULAR)).isFalse();
        }

        @Test
        void nonEU_thirdCountry_notZmReportable() {
            Client client = new Client(user, "US Corp", IncomeStream.FREIBERUF,
                    ClientType.B2B, "US", null);
            assertThat(service.isZmReportable(client, VatTreatment.THIRD_COUNTRY)).isFalse();
        }

        @Test
        void appleDistribution_isZmReportable() {
            Client client = new Client(user, "Apple Distribution International",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE9700053D");
            assertThat(service.isZmReportable(client, VatTreatment.REVERSE_CHARGE)).isTrue();
        }

        @Test
        void googleIreland_isZmReportable() {
            Client client = new Client(user, "Google Ireland Ltd",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE6388047V");
            assertThat(service.isZmReportable(client, VatTreatment.REVERSE_CHARGE)).isTrue();
        }

        @Test
        void nullClient_notZmReportable() {
            assertThat(service.isZmReportable(null, VatTreatment.REGULAR)).isFalse();
        }
    }

    @Nested
    class PlatformPayoutDetection {

        @Test
        void appleInName_detected() {
            Client client = new Client(user, "Apple Distribution International",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE9700053D");
            assertThat(service.isKnownPlatformPayout(client)).isTrue();
        }

        @Test
        void googleInName_detected() {
            Client client = new Client(user, "Google Ireland Ltd",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE6388047V");
            assertThat(service.isKnownPlatformPayout(client)).isTrue();
        }

        @Test
        void regularCompany_notDetected() {
            Client client = new Client(user, "Acme Consulting GmbH", IncomeStream.FREIBERUF);
            assertThat(service.isKnownPlatformPayout(client)).isFalse();
        }

        @Test
        void caseInsensitive() {
            Client client = new Client(user, "APPLE INC",
                    IncomeStream.FREIBERUF, ClientType.B2B, "US", null);
            assertThat(service.isKnownPlatformPayout(client)).isTrue();
        }
    }

    @Nested
    class VatNotices {

        @Test
        void reverseCharge_returnsSteuerschuldnerschaft() {
            assertThat(service.getVatNotice(VatTreatment.REVERSE_CHARGE))
                    .contains("Steuerschuldnerschaft des Leistungsempfängers");
        }

        @Test
        void thirdCountry_returnsParagraph3a() {
            assertThat(service.getVatNotice(VatTreatment.THIRD_COUNTRY))
                    .contains("§3a UStG");
        }

        @Test
        void intraEU_returnsInnergemeinschaftlich() {
            assertThat(service.getVatNotice(VatTreatment.INTRA_EU))
                    .contains("innergemeinschaftliche");
        }

        @Test
        void regular_returnsNull() {
            assertThat(service.getVatNotice(VatTreatment.REGULAR)).isNull();
        }

        @Test
        void smallBusiness_returnsNull() {
            assertThat(service.getVatNotice(VatTreatment.SMALL_BUSINESS)).isNull();
        }
    }

    @Nested
    class EuCountries {

        @Test
        void allEuMembersPresent() {
            assertThat(ReverseChargeService.EU_COUNTRIES).containsExactlyInAnyOrder(
                    "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
                    "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL",
                    "PT", "RO", "SK", "SI", "ES", "SE");
        }

        @Test
        void germany_notInEuList() {
            assertThat(ReverseChargeService.EU_COUNTRIES).doesNotContain("DE");
        }

        @Test
        void uk_notInEuList() {
            assertThat(ReverseChargeService.EU_COUNTRIES).doesNotContain("GB");
        }

        @Test
        void switzerland_notInEuList() {
            assertThat(ReverseChargeService.EU_COUNTRIES).doesNotContain("CH");
        }
    }
}
