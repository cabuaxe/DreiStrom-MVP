package de.dreistrom.invoicing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineItemRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice,
        @NotNull @DecimalMin("0") BigDecimal vatRate
) {}
