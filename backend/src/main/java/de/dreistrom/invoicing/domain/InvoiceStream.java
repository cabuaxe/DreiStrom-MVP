package de.dreistrom.invoicing.domain;

/**
 * Invoice-eligible income streams.
 * EMPLOYMENT is excluded: employment income is not invoiced.
 */
public enum InvoiceStream {
    FREIBERUF,
    GEWERBE
}
