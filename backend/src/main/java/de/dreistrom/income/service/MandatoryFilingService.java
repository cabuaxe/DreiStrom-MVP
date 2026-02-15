package de.dreistrom.income.service;

import de.dreistrom.income.dto.MandatoryFilingResponse;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Checks mandatory tax filing status per §46 Abs. 2 Nr. 1 EStG.
 * Filing is required when Nebeneinkünfte (FREIBERUF + GEWERBE) exceed €410.
 */
@Service
@RequiredArgsConstructor
public class MandatoryFilingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THRESHOLD = new BigDecimal("410");

    private final IncomeEntryRepository incomeEntryRepository;

    @Transactional(readOnly = true)
    public MandatoryFilingResponse getStatus(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long selfEmployedCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);
        BigDecimal nebeneinkuenfte = selfEmployedCents != null
                ? new BigDecimal(selfEmployedCents).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MandatoryFilingResponse(
                year,
                nebeneinkuenfte,
                THRESHOLD,
                nebeneinkuenfte.compareTo(THRESHOLD) > 0
        );
    }
}
