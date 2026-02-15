package de.dreistrom.invoicing.dto;

import de.dreistrom.invoicing.domain.VatTreatment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateInvoiceRequest(
        @NotNull Long clientId,
        @NotNull LocalDate invoiceDate,
        LocalDate dueDate,
        @NotEmpty @Valid List<LineItemRequest> lineItems,
        @NotNull @DecimalMin("0") BigDecimal netTotal,
        @NotNull @DecimalMin("0") BigDecimal vat,
        @NotNull @DecimalMin("0") BigDecimal grossTotal,
        VatTreatment vatTreatment,
        @Size(max = 2000) String notes
) {}
