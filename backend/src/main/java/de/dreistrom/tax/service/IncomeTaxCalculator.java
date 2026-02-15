package de.dreistrom.tax.service;

import de.dreistrom.tax.domain.TaxYearParams;
import de.dreistrom.tax.dto.DeductionBreakdown;
import de.dreistrom.tax.dto.TaxCalculationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implements the §32a EStG progressive income tax schedule (14-45%)
 * and Solidaritaetszuschlag (§5 SolZG).
 * <p>
 * All computations use BigDecimal with RoundingMode.HALF_UP.
 * The tax result is truncated to full euros (floor) per §32a Abs. 1 S. 6 EStG.
 */
@Service
public class IncomeTaxCalculator {

    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /**
     * Compute the full income tax assessment for a tax year.
     *
     * @param params              year-specific bracket parameters
     * @param employmentIncome    gross employment income (Einkuenfte §19 EStG)
     * @param freiberufIncome     gross Freiberuf income (Einkuenfte §18 EStG)
     * @param gewerbeIncome       gross Gewerbe income (Einkuenfte §15 EStG)
     * @param freiberufExpenses   deductible Freiberuf business expenses
     * @param gewerbeExpenses     deductible Gewerbe business expenses
     * @return complete tax calculation result
     */
    public TaxCalculationResult calculate(TaxYearParams params,
                                          BigDecimal employmentIncome,
                                          BigDecimal freiberufIncome,
                                          BigDecimal gewerbeIncome,
                                          BigDecimal freiberufExpenses,
                                          BigDecimal gewerbeExpenses) {

        BigDecimal totalGross = employmentIncome.add(freiberufIncome).add(gewerbeIncome);

        // Apply deductions
        BigDecimal werbungskosten = hasEmploymentIncome(employmentIncome)
                ? params.werbungskostenpauschale()
                : BigDecimal.ZERO;
        BigDecimal sonderausgaben = params.sonderausgabenpauschale();
        BigDecimal totalDeductions = freiberufExpenses
                .add(gewerbeExpenses)
                .add(werbungskosten)
                .add(sonderausgaben);

        DeductionBreakdown deductions = new DeductionBreakdown(
                freiberufExpenses, gewerbeExpenses,
                werbungskosten, sonderausgaben, totalDeductions);

        // Taxable income (zvE) — cannot be negative
        BigDecimal zvE = totalGross.subtract(totalDeductions)
                .max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.DOWN);

        // Progressive tax per §32a EStG
        BigDecimal est = computeProgressiveTax(params, zvE);

        // Solidaritaetszuschlag per §5 SolZG
        BigDecimal soli = computeSoli(params, est);

        BigDecimal totalTax = est.add(soli);

        // Marginal rate (rate at the top euro of zvE)
        BigDecimal marginalRate = computeMarginalRate(params, zvE);

        // Effective rate = totalTax / totalGross (or zvE if preferred)
        BigDecimal effectiveRate = totalGross.signum() > 0
                ? totalTax.multiply(HUNDRED).divide(totalGross, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TaxCalculationResult(
                params.year(),
                employmentIncome, freiberufIncome, gewerbeIncome, totalGross,
                deductions, zvE,
                est, soli, totalTax,
                marginalRate, effectiveRate
        );
    }

    /**
     * Compute progressive income tax per §32a Abs. 1 EStG.
     * The result is truncated to full euros (floor).
     *
     * @param params year-specific parameters
     * @param zvE    zu versteuerndes Einkommen (taxable income), rounded to full euros
     * @return income tax amount in EUR (truncated to full euros)
     */
    public BigDecimal computeProgressiveTax(TaxYearParams params, BigDecimal zvE) {
        if (zvE.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tax;

        if (zvE.compareTo(params.grundfreibetrag()) <= 0) {
            // Zone 1: Grundfreibetrag — no tax
            tax = BigDecimal.ZERO;

        } else if (zvE.compareTo(params.zone2Upper()) <= 0) {
            // Zone 2: (a·y + b)·y  where y = (zvE - grundfreibetrag) / 10_000
            BigDecimal y = zvE.subtract(params.grundfreibetrag())
                    .divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP);
            tax = params.zone2A().multiply(y)
                    .add(params.zone2B())
                    .multiply(y)
                    .setScale(0, RoundingMode.DOWN);

        } else if (zvE.compareTo(params.zone3Upper()) <= 0) {
            // Zone 3: (a·z + b)·z + c  where z = (zvE - zone2Upper) / 10_000
            BigDecimal z = zvE.subtract(params.zone2Upper())
                    .divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP);
            tax = params.zone3A().multiply(z)
                    .add(params.zone3B())
                    .multiply(z)
                    .add(params.zone3C())
                    .setScale(0, RoundingMode.DOWN);

        } else if (zvE.compareTo(params.zone4Upper()) <= 0) {
            // Zone 4: rate·zvE - subtraction
            tax = params.zone4Rate().multiply(zvE)
                    .subtract(params.zone4Sub())
                    .setScale(0, RoundingMode.DOWN);

        } else {
            // Zone 5 (Reichensteuer): rate·zvE - subtraction
            tax = params.zone5Rate().multiply(zvE)
                    .subtract(params.zone5Sub())
                    .setScale(0, RoundingMode.DOWN);
        }

        return tax.setScale(2);
    }

    /**
     * Compute Solidaritaetszuschlag per §5 SolZG.
     * <ul>
     *   <li>ESt ≤ exemption → Soli = 0</li>
     *   <li>Milderungszone: Soli = min(5.5% × ESt, 11.9% × (ESt - exemption))</li>
     *   <li>Full: Soli = 5.5% × ESt</li>
     * </ul>
     *
     * @param params year-specific parameters
     * @param est    computed income tax
     * @return Solidaritaetszuschlag in EUR (rounded to 2 decimals)
     */
    public BigDecimal computeSoli(TaxYearParams params, BigDecimal est) {
        if (est.compareTo(params.soliExemption()) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        BigDecimal fullSoli = est.multiply(params.soliRate())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal milderung = est.subtract(params.soliExemption())
                .multiply(params.soliMilderungsRate())
                .setScale(2, RoundingMode.HALF_UP);

        return fullSoli.min(milderung);
    }

    /**
     * Determine the marginal tax rate for a given taxable income.
     * Returns the percentage rate applied to the top euro.
     */
    public BigDecimal computeMarginalRate(TaxYearParams params, BigDecimal zvE) {
        if (zvE.compareTo(params.grundfreibetrag()) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        } else if (zvE.compareTo(params.zone2Upper()) <= 0) {
            // Zone 2 marginal: d/dzvE of (a·y + b)·y = (2a·y + b) / 10_000
            BigDecimal y = zvE.subtract(params.grundfreibetrag())
                    .divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP);
            BigDecimal marginal = params.zone2A().multiply(new BigDecimal("2")).multiply(y)
                    .add(params.zone2B())
                    .divide(TEN_THOUSAND, 6, RoundingMode.HALF_UP);
            return marginal.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        } else if (zvE.compareTo(params.zone3Upper()) <= 0) {
            // Zone 3 marginal: d/dzvE of (a·z + b)·z + c = (2a·z + b) / 10_000
            BigDecimal z = zvE.subtract(params.zone2Upper())
                    .divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP);
            BigDecimal marginal = params.zone3A().multiply(new BigDecimal("2")).multiply(z)
                    .add(params.zone3B())
                    .divide(TEN_THOUSAND, 6, RoundingMode.HALF_UP);
            return marginal.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        } else if (zvE.compareTo(params.zone4Upper()) <= 0) {
            return new BigDecimal("42.00");
        } else {
            return new BigDecimal("45.00");
        }
    }

    private boolean hasEmploymentIncome(BigDecimal employment) {
        return employment != null && employment.signum() > 0;
    }
}
