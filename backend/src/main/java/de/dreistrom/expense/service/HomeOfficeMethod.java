package de.dreistrom.expense.service;

/**
 * Home office deduction methods per German tax law.
 */
public enum HomeOfficeMethod {
    /** Dedicated Arbeitszimmer: proportional rent and utilities by floor area. */
    ARBEITSZIMMER,
    /** Homeoffice-Pauschale: flat rate of 6 EUR/day, max 1,260 EUR/year. */
    PAUSCHALE
}
