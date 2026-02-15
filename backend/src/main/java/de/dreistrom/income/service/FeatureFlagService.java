package de.dreistrom.income.service;

import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.dto.UserFeatureFlags;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import de.dreistrom.socialinsurance.repository.SocialInsuranceEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Computes progressive disclosure feature flags from the user's actual business volume.
 * <p>
 * Architecture doc §4.2.4: "The UI adapts to the user's actual complexity.
 * A user earning €5,000 in side income sees a simplified view.
 * OSS rules, ZM, and Bilanzierung thresholds surface only when relevant."
 * <p>
 * Flags are computed dynamically from income, expense, client, and social insurance data.
 * Complexity levels: 1 = micro (< €5k), 2 = small (< €100k), 3 = medium (≥ €100k).
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final long MICRO_THRESHOLD_CENTS = 500_000L;    // €5,000
    private static final long SMALL_THRESHOLD_CENTS = 10_000_000L; // €100,000
    private static final long BILANZIERUNG_APPROACH_REVENUE_CENTS = 20_000_000L; // €200,000
    private static final long BILANZIERUNG_APPROACH_PROFIT_CENTS = 2_000_000L;   // €20,000
    private static final long FILING_THRESHOLD_CENTS = 41_000L;    // €410
    private static final long OSS_THRESHOLD_CENTS = 1_000_000L;    // €10,000

    private final IncomeEntryRepository incomeEntryRepository;
    private final ClientRepository clientRepository;
    private final SocialInsuranceEntryRepository socialInsuranceEntryRepository;

    @Transactional(readOnly = true)
    public UserFeatureFlags getFlags(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // ── Income stream activity ─────────────────────────────────
        Long employmentCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "EMPLOYMENT", yearStart, yearEnd);
        Long freiberufCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "FREIBERUF", yearStart, yearEnd);
        Long gewerbeCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "GEWERBE", yearStart, yearEnd);

        boolean hasEmployment = employmentCents != null && employmentCents > 0;
        boolean hasFreiberuf = freiberufCents != null && freiberufCents > 0;
        boolean hasGewerbe = gewerbeCents != null && gewerbeCents > 0;

        int activeStreams = (hasEmployment ? 1 : 0) + (hasFreiberuf ? 1 : 0) + (hasGewerbe ? 1 : 0);
        boolean hasMultipleStreams = activeStreams >= 2;

        long selfEmployedCents = (freiberufCents != null ? freiberufCents : 0L)
                + (gewerbeCents != null ? gewerbeCents : 0L);
        long totalCents = selfEmployedCents + (employmentCents != null ? employmentCents : 0L);

        // ── Client analysis ────────────────────────────────────────
        List<Client> activeClients = clientRepository.findByUserIdAndActiveTrue(userId);
        boolean hasClients = !activeClients.isEmpty();
        boolean hasEuB2cClients = activeClients.stream()
                .anyMatch(c -> c.getClientType() == ClientType.B2C
                        && !"DE".equals(c.getCountry())
                        && isEuCountry(c.getCountry()));
        boolean hasEuB2bClients = activeClients.stream()
                .anyMatch(c -> c.getClientType() == ClientType.B2B
                        && !"DE".equals(c.getCountry())
                        && isEuCountry(c.getCountry()));

        // ── Social insurance data ──────────────────────────────────
        List<SocialInsuranceEntry> siEntries = socialInsuranceEntryRepository
                .findByUserIdAndYearOrderByMonthAsc(userId, (short) year);
        boolean hasSocialInsuranceData = !siEntries.isEmpty();

        // ── Complexity level ───────────────────────────────────────
        int complexityLevel;
        if (selfEmployedCents < MICRO_THRESHOLD_CENTS) {
            complexityLevel = 1; // micro
        } else if (selfEmployedCents < SMALL_THRESHOLD_CENTS) {
            complexityLevel = 2; // small
        } else {
            complexityLevel = 3; // medium
        }

        // ── Feature flag computation ───────────────────────────────
        boolean anySelfEmployed = hasFreiberuf || hasGewerbe;

        // Kleinunternehmer: show when any self-employed income exists
        boolean showKleinunternehmer = anySelfEmployed;

        // Abfärbung: show when both Freiberuf AND Gewerbe exist (mixed income risk)
        boolean showAbfaerbung = hasFreiberuf && hasGewerbe;

        // Gewerbesteuer: show when Gewerbe income exists
        boolean showGewerbesteuer = hasGewerbe;

        // Bilanzierung: show when approaching §141 AO thresholds
        boolean showBilanzierung = hasGewerbe
                && (gewerbeCents > BILANZIERUNG_APPROACH_REVENUE_CENTS
                || computeGewerbeProfit(userId, yearStart, yearEnd, gewerbeCents)
                > BILANZIERUNG_APPROACH_PROFIT_CENTS);

        // Social insurance: show when user has both employment and self-employment
        boolean showSocialInsurance = hasEmployment && anySelfEmployed;

        // Mandatory filing: show when self-employed income > €410
        boolean showMandatoryFiling = selfEmployedCents > FILING_THRESHOLD_CENTS;

        // ArbZG: show when user tracks working hours
        boolean showArbZG = hasSocialInsuranceData && hasEmployment && anySelfEmployed;

        // Invoicing: show when user has clients
        boolean showInvoicing = hasClients || anySelfEmployed;

        // Expense allocation: show when multiple streams active
        boolean showExpenseAllocation = hasMultipleStreams;

        // Vorauszahlungen: show when filing obligation exists
        boolean showVorauszahlungen = selfEmployedCents > FILING_THRESHOLD_CENTS;

        // Tax estimation: show when any meaningful income (above micro threshold)
        boolean showTaxEstimation = anySelfEmployed && selfEmployedCents >= MICRO_THRESHOLD_CENTS;

        // OSS: show when EU B2C clients and self-employed revenue > €10,000
        boolean showOss = hasEuB2cClients && selfEmployedCents > OSS_THRESHOLD_CENTS;

        // Zusammenfassende Meldung: show when EU B2B clients (reverse charge)
        boolean showZm = hasEuB2bClients;

        return new UserFeatureFlags(
                year,
                hasEmployment, hasFreiberuf, hasGewerbe, hasMultipleStreams,
                showKleinunternehmer, showAbfaerbung, showGewerbesteuer,
                showBilanzierung, showSocialInsurance, showMandatoryFiling,
                showArbZG,
                showInvoicing, showExpenseAllocation, showVorauszahlungen,
                showTaxEstimation,
                showOss, showZm,
                complexityLevel
        );
    }

    private long computeGewerbeProfit(Long userId, LocalDate yearStart, LocalDate yearEnd,
                                      long gewerbeRevenueCents) {
        // Simplified profit estimation (revenue only, expense lookup optional)
        // Full profit calculation is in GewerbesteuerThresholdService
        return gewerbeRevenueCents;
    }

    private static boolean isEuCountry(String countryCode) {
        return EU_COUNTRIES.contains(countryCode);
    }

    private static final java.util.Set<String> EU_COUNTRIES = java.util.Set.of(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
            "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
            "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    );
}
