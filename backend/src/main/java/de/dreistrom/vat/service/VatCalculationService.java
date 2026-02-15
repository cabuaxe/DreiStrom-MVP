package de.dreistrom.vat.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.vat.dto.VatSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Computes output VAT (Umsatzsteuer), input VAT (Vorsteuer), and net payable
 * for a given user and period. Supports standard (19%), reduced (7%), and
 * exempt (0%) rates. Kleinunternehmer mode (§19 UStG) zeroes all VAT.
 */
@Service
@RequiredArgsConstructor
public class VatCalculationService {

    public static final BigDecimal STANDARD_RATE = new BigDecimal("19");
    public static final BigDecimal REDUCED_RATE = new BigDecimal("7");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final InvoiceRepository invoiceRepository;
    private final ExpenseEntryRepository expenseEntryRepository;

    /**
     * Calculate VAT summary for a user and period.
     *
     * @param userId          the user
     * @param from            period start (inclusive)
     * @param to              period end (inclusive)
     * @param kleinunternehmer if true, all VAT is zeroed (§19 UStG)
     * @return VAT summary with output, input, and net payable
     */
    @Transactional(readOnly = true)
    public VatSummary calculate(Long userId, LocalDate from, LocalDate to,
                                boolean kleinunternehmer) {
        if (kleinunternehmer) {
            return VatSummary.zero();
        }

        // Output VAT from invoices (already stored per invoice)
        BigDecimal freiberufOutputVat = centsToEuros(
                invoiceRepository.sumVatCentsByStreamAndDateRange(
                        userId, "FREIBERUF", from, to));
        BigDecimal gewerbeOutputVat = centsToEuros(
                invoiceRepository.sumVatCentsByStreamAndDateRange(
                        userId, "GEWERBE", from, to));
        BigDecimal outputVat = freiberufOutputVat.add(gewerbeOutputVat);

        // Input VAT (Vorsteuer) from allocated business expenses at standard rate.
        // Personal-allocated expenses are not deductible.
        BigDecimal freiberufExpenses = centsToEuros(
                expenseEntryRepository.sumCentsFreiberufByDateRange(userId, from, to));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, from, to));

        BigDecimal freiberufInputVat = extractVat(freiberufExpenses, STANDARD_RATE);
        BigDecimal gewerbeInputVat = extractVat(gewerbeExpenses, STANDARD_RATE);
        BigDecimal inputVat = freiberufInputVat.add(gewerbeInputVat);

        BigDecimal netPayable = outputVat.subtract(inputVat);

        return new VatSummary(
                outputVat, freiberufOutputVat, gewerbeOutputVat,
                inputVat, freiberufInputVat, gewerbeInputVat,
                netPayable, false);
    }

    /**
     * Extract VAT from a gross amount at the given rate.
     * Formula: VAT = gross × rate / (100 + rate)
     */
    public BigDecimal extractVat(BigDecimal grossAmount, BigDecimal rate) {
        if (grossAmount == null || grossAmount.signum() == 0 || rate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(rate)
                .divide(HUNDRED.add(rate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate net amount from gross.
     * Formula: net = gross × 100 / (100 + rate)
     */
    public BigDecimal netFromGross(BigDecimal grossAmount, BigDecimal rate) {
        if (grossAmount == null || grossAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(HUNDRED)
                .divide(HUNDRED.add(rate), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate gross amount from net.
     * Formula: gross = net × (100 + rate) / 100
     */
    public BigDecimal grossFromNet(BigDecimal netAmount, BigDecimal rate) {
        if (netAmount == null || netAmount.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return netAmount.multiply(HUNDRED.add(rate))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
