package de.dreistrom.invoicing.service;

import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.invoicing.domain.VatTreatment;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Determines VAT treatment and ZM reporting obligations based on
 * client properties (country, type, USt-IdNr).
 *
 * Rules:
 * - DE clients → REGULAR (standard German VAT)
 * - EU B2B with USt-IdNr → REVERSE_CHARGE (§13b UStG)
 * - EU B2C → REGULAR (VAT charged at German rate)
 * - Non-EU (Drittland) → THIRD_COUNTRY (§3a UStG, no VAT)
 */
@Service
public class ReverseChargeService {

    /** EU-27 member state ISO 3166-1 alpha-2 codes (excluding DE). */
    static final Set<String> EU_COUNTRIES = Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL",
            "PT", "RO", "SK", "SI", "ES", "SE"
    );

    /** Known platform payout providers that require ZM reporting. */
    private static final Set<String> ZM_PAYOUT_KEYWORDS = Set.of(
            "apple", "google"
    );

    /**
     * Auto-determine the appropriate VatTreatment based on client properties.
     *
     * @param client the invoice recipient
     * @return the recommended VatTreatment
     */
    public VatTreatment determineVatTreatment(Client client) {
        if (client == null) {
            return VatTreatment.REGULAR;
        }

        String country = client.getCountry();

        // Domestic (DE) → standard VAT
        if ("DE".equals(country)) {
            return VatTreatment.REGULAR;
        }

        // EU member state
        if (EU_COUNTRIES.contains(country)) {
            // B2B with valid USt-IdNr → reverse charge (§13b UStG)
            if (client.getClientType() == ClientType.B2B
                    && client.getUstIdNr() != null
                    && !client.getUstIdNr().isBlank()) {
                return VatTreatment.REVERSE_CHARGE;
            }
            // EU B2C or B2B without USt-IdNr → regular VAT
            return VatTreatment.REGULAR;
        }

        // Non-EU (Drittland) → no VAT per §3a UStG
        return VatTreatment.THIRD_COUNTRY;
    }

    /**
     * Determine whether an invoice should be flagged for ZM
     * (Zusammenfassende Meldung) reporting.
     *
     * ZM is required for:
     * - EU B2B reverse charge transactions
     * - Known platform payout providers (Apple, Google)
     */
    public boolean isZmReportable(Client client, VatTreatment vatTreatment) {
        if (client == null) {
            return false;
        }

        // All EU B2B reverse charge invoices are ZM-reportable
        if (vatTreatment == VatTreatment.REVERSE_CHARGE
                && EU_COUNTRIES.contains(client.getCountry())) {
            return true;
        }

        // Apple/Google payout providers
        return isKnownPlatformPayout(client);
    }

    /**
     * Get the appropriate VAT notice text for the given treatment.
     *
     * @param vatTreatment the VAT treatment
     * @return notice text, or null if no special notice is required
     */
    public String getVatNotice(VatTreatment vatTreatment) {
        return switch (vatTreatment) {
            case REVERSE_CHARGE ->
                    "Steuerschuldnerschaft des Leistungsempfängers (Reverse Charge, §13b UStG).";
            case THIRD_COUNTRY ->
                    "Leistung nicht steuerbar per §3a UStG (Leistungsort im Drittland).";
            case INTRA_EU ->
                    "Steuerfreie innergemeinschaftliche Lieferung/Leistung.";
            case SMALL_BUSINESS, REGULAR -> null;
        };
    }

    /**
     * Detect known platform payout providers (Apple, Google) by client name.
     */
    boolean isKnownPlatformPayout(Client client) {
        if (client.getName() == null) {
            return false;
        }
        String nameLower = client.getName().toLowerCase();
        return ZM_PAYOUT_KEYWORDS.stream().anyMatch(nameLower::contains);
    }
}
