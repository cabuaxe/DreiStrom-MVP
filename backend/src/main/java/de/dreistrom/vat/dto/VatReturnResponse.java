package de.dreistrom.vat.dto;

import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record VatReturnResponse(
        Long id,
        short year,
        PeriodType periodType,
        short periodNumber,
        BigDecimal outputVat,
        BigDecimal inputVat,
        BigDecimal netPayable,
        VatReturnStatus status,
        LocalDate submissionDate,
        Instant createdAt,
        Instant updatedAt
) {}
