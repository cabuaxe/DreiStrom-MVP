package de.dreistrom.income.service;

import de.dreistrom.income.dto.AbfaerbungStatusResponse;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal RATIO_THRESHOLD = new BigDecimal("0.03");
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("24500");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;

    public AbfaerbungStatusResponse getAbfaerbungStatus(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long gewerbeCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "GEWERBE", yearStart, yearEnd);
        Long selfEmployedCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);

        BigDecimal gewerbe = gewerbeCents != null
                ? new BigDecimal(gewerbeCents).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal selfEmployed = selfEmployedCents != null
                ? new BigDecimal(selfEmployedCents).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ratio = BigDecimal.ZERO;
        if (selfEmployed.compareTo(BigDecimal.ZERO) > 0) {
            ratio = gewerbe.divide(selfEmployed, 4, RoundingMode.HALF_UP);
        }

        boolean thresholdExceeded = ratio.compareTo(RATIO_THRESHOLD) > 0
                && gewerbe.compareTo(AMOUNT_THRESHOLD) > 0;

        return new AbfaerbungStatusResponse(ratio, gewerbe, selfEmployed, thresholdExceeded, year);
    }
}
