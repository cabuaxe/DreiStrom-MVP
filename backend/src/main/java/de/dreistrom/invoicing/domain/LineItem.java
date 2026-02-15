package de.dreistrom.invoicing.domain;

import java.math.BigDecimal;

/**
 * Single line item on an invoice, stored as JSON array element.
 */
public record LineItem(
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal vatRate
) {}
