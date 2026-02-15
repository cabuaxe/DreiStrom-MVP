package de.dreistrom.tax.service;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.expense.service.DepreciationService;
import de.dreistrom.expense.service.StreamDepreciationSummary;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.dto.EuerResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Generates Einnahmen-Ueberschuss-Rechnung (EÜR) per §4 Abs. 3 EStG.
 * Produces separate profit/loss statements for Freiberuf and Gewerbe streams.
 * Allocates shared expenses per documented ratios from the expense module.
 */
@Service
@RequiredArgsConstructor
public class EuerService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final DepreciationService depreciationService;

    /**
     * Generate EÜR for a specific stream and tax year.
     *
     * @param userId the user
     * @param stream FREIBERUF or GEWERBE
     * @param year   the tax year
     * @return EÜR with income, expenses, depreciation, and profit
     */
    @Transactional(readOnly = true)
    public EuerResult generate(Long userId, IncomeStream stream, int year) {
        if (stream == IncomeStream.EMPLOYMENT) {
            throw new IllegalArgumentException(
                    "EÜR is only applicable to Freiberuf and Gewerbe streams, not EMPLOYMENT");
        }

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // Income for this stream
        BigDecimal totalIncome = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, stream.name(), yearStart, yearEnd));

        // Allocated expenses for this stream
        BigDecimal allocatedExpenses = centsToEuros(
                getStreamExpenseCents(userId, stream, yearStart, yearEnd));

        // Depreciation (AfA) for this stream
        StreamDepreciationSummary depSummary =
                depreciationService.computeStreamTotalsForYear(userId, year);
        BigDecimal depreciation = stream == IncomeStream.FREIBERUF
                ? depSummary.freiberuf()
                : depSummary.gewerbe();

        BigDecimal totalExpenses = allocatedExpenses.add(depreciation);
        BigDecimal profit = totalIncome.subtract(totalExpenses);

        return new EuerResult(
                year, stream,
                totalIncome,
                allocatedExpenses,
                BigDecimal.ZERO, // shared expenses are already in allocatedExpenses via allocation rules
                depreciation,
                totalExpenses,
                profit
        );
    }

    /**
     * Generate dual-stream EÜR for both Freiberuf and Gewerbe.
     */
    @Transactional(readOnly = true)
    public DualStreamEuer generateDual(Long userId, int year) {
        EuerResult freiberuf = generate(userId, IncomeStream.FREIBERUF, year);
        EuerResult gewerbe = generate(userId, IncomeStream.GEWERBE, year);
        return new DualStreamEuer(freiberuf, gewerbe);
    }

    private Long getStreamExpenseCents(Long userId, IncomeStream stream,
                                        LocalDate from, LocalDate to) {
        return stream == IncomeStream.FREIBERUF
                ? expenseEntryRepository.sumCentsFreiberufByDateRange(userId, from, to)
                : expenseEntryRepository.sumCentsGewerbeByDateRange(userId, from, to);
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public record DualStreamEuer(EuerResult freiberuf, EuerResult gewerbe) {
        public BigDecimal combinedProfit() {
            return freiberuf.profit().add(gewerbe.profit());
        }
    }
}
