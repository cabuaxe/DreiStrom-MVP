package de.dreistrom.invoicing.dto;

import de.dreistrom.income.dto.ClientSummary;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceResponse(
        Long id,
        InvoiceStream streamType,
        String number,
        ClientSummary client,
        LocalDate invoiceDate,
        LocalDate dueDate,
        List<LineItem> lineItems,
        BigDecimal netTotal,
        BigDecimal vat,
        BigDecimal grossTotal,
        String currency,
        VatTreatment vatTreatment,
        InvoiceStatus status,
        String notes,
        boolean zmReportable,
        Instant createdAt,
        Instant updatedAt
) {}
