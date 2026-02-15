package de.dreistrom.tax.service;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.expense.service.DepreciationService;
import de.dreistrom.expense.service.StreamDepreciationSummary;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import de.dreistrom.socialinsurance.repository.SocialInsuranceEntryRepository;
import de.dreistrom.tax.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Assembles the full annual tax return package (Einkommensteuererklaerung)
 * by orchestrating all tax calculators and aggregating data across modules.
 * <p>
 * Produces: Anlage N (§19), Anlage S (§18), Anlage G (§15),
 * Anlage EÜR (dual), Anlage Vorsorgeaufwand, plus tax totals.
 */
@Service
@RequiredArgsConstructor
public class AnnualTaxPackageService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // Standard German social insurance rates (employee share, 2024/2025)
    private static final BigDecimal KV_RATE = new BigDecimal("0.073");   // 14.6% / 2
    private static final BigDecimal PV_RATE = new BigDecimal("0.017");   // ~3.4% / 2
    private static final BigDecimal RV_RATE = new BigDecimal("0.093");   // 18.6% / 2
    private static final BigDecimal AV_RATE = new BigDecimal("0.013");   // 2.6% / 2

    private final TaxAssessmentService taxAssessmentService;
    private final GewerbesteuerCalculator gewerbesteuerCalculator;
    private final EuerService euerService;
    private final DepreciationService depreciationService;
    private final IncomeEntryRepository incomeEntryRepository;
    private final SocialInsuranceEntryRepository socialInsuranceEntryRepository;

    /**
     * Assemble the complete annual tax return package for a user and year.
     *
     * @param userId the user
     * @param year   the tax year
     * @return full AnnualTaxPackage with all Anlagen
     */
    @Transactional(readOnly = true)
    public AnnualTaxPackage assemble(Long userId, int year) {
        // ── Tax calculation (income tax + Soli) ──────────────────────────
        TaxCalculationResult taxCalc = taxAssessmentService.assess(userId, year);

        // ── Gewerbesteuer ────────────────────────────────────────────────
        GewerbesteuerResult gewSt = gewerbesteuerCalculator.calculate(
                userId, year, taxCalc.incomeTax());

        // ── EÜR for both streams ─────────────────────────────────────────
        EuerResult euerFreiberuf = euerService.generate(userId, IncomeStream.FREIBERUF, year);
        EuerResult euerGewerbe = euerService.generate(userId, IncomeStream.GEWERBE, year);

        // ── Depreciation by stream ───────────────────────────────────────
        StreamDepreciationSummary depSummary =
                depreciationService.computeStreamTotalsForYear(userId, year);

        // ── Anlage N (Employment §19 EStG) ───────────────────────────────
        AnnualTaxPackage.AnlageN anlageN = buildAnlageN(taxCalc);

        // ── Anlage S (Freiberuf §18 EStG) ────────────────────────────────
        AnnualTaxPackage.AnlageS anlageS = new AnnualTaxPackage.AnlageS(
                euerFreiberuf.totalIncome(),
                euerFreiberuf.directExpenses(),
                euerFreiberuf.profit(),
                depSummary.freiberuf()
        );

        // ── Anlage G (Gewerbe §15 EStG) ─────────────────────────────────
        AnnualTaxPackage.AnlageG anlageG = new AnnualTaxPackage.AnlageG(
                euerGewerbe.totalIncome(),
                euerGewerbe.directExpenses(),
                euerGewerbe.profit(),
                depSummary.gewerbe(),
                gewSt.gewerbesteuer(),
                gewSt.paragraph35Credit()
        );

        // ── Anlage Vorsorgeaufwand ───────────────────────────────────────
        AnnualTaxPackage.AnlageVorsorgeaufwand vorsorge =
                buildVorsorgeaufwand(userId, year, taxCalc.employmentIncome());

        return new AnnualTaxPackage(
                year,
                anlageN,
                anlageS,
                anlageG,
                euerFreiberuf,
                euerGewerbe,
                vorsorge,
                taxCalc,
                gewSt
        );
    }

    /**
     * Build Anlage N from employment income data.
     * Lohnsteuer is approximated as the proportional share of income tax
     * attributable to employment income.
     */
    private AnnualTaxPackage.AnlageN buildAnlageN(TaxCalculationResult taxCalc) {
        BigDecimal brutto = taxCalc.employmentIncome();

        // Approximate Lohnsteuer as proportional share of ESt
        BigDecimal lohnsteuer = BigDecimal.ZERO;
        BigDecimal soli = BigDecimal.ZERO;
        if (taxCalc.totalGrossIncome().signum() > 0 && brutto.signum() > 0) {
            BigDecimal ratio = brutto.divide(taxCalc.totalGrossIncome(), 6, RoundingMode.HALF_UP);
            lohnsteuer = taxCalc.incomeTax().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
            soli = taxCalc.solidaritaetszuschlag().multiply(ratio)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal werbungskosten = taxCalc.deductions().werbungskostenpauschale();

        return new AnnualTaxPackage.AnlageN(
                brutto,
                lohnsteuer,
                soli,
                BigDecimal.ZERO, // Kirchensteuer — not modeled in MVP
                werbungskosten,
                BigDecimal.ZERO  // Fahrtkosten — not modeled in MVP
        );
    }

    /**
     * Build Anlage Vorsorgeaufwand from social insurance entries.
     * Calculates contributions using standard German employee rates
     * applied to employment income.
     */
    private AnnualTaxPackage.AnlageVorsorgeaufwand buildVorsorgeaufwand(
            Long userId, int year, BigDecimal employmentIncome) {

        // Sum employment income from social insurance entries if available,
        // otherwise fall back to income entry totals
        List<SocialInsuranceEntry> entries =
                socialInsuranceEntryRepository.findByUserIdAndYearOrderByMonthAsc(
                        userId, (short) year);

        BigDecimal basisIncome;
        if (!entries.isEmpty()) {
            basisIncome = entries.stream()
                    .map(SocialInsuranceEntry::getEmploymentIncome)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            basisIncome = employmentIncome;
        }

        BigDecimal kv = basisIncome.multiply(KV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pv = basisIncome.multiply(PV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rv = basisIncome.multiply(RV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal av = basisIncome.multiply(AV_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = kv.add(pv).add(rv).add(av);

        return new AnnualTaxPackage.AnlageVorsorgeaufwand(kv, pv, rv, av, total);
    }
}
