package de.dreistrom.invoicing.dto;

import de.dreistrom.invoicing.domain.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInvoiceStatusRequest(
        @NotNull InvoiceStatus status
) {}
