package de.dreistrom.expense.service;

/**
 * Recommendation comparing both home office deduction methods.
 *
 * @param arbeitszimmer  result using the Arbeitszimmer method
 * @param pauschale      result using the Homeoffice-Pauschale method
 * @param recommended    the more advantageous method
 */
public record HomeOfficeRecommendation(
        HomeOfficeResult arbeitszimmer,
        HomeOfficeResult pauschale,
        HomeOfficeMethod recommended
) {}
