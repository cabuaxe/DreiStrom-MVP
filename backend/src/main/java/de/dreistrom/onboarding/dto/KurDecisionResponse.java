package de.dreistrom.onboarding.dto;

import de.dreistrom.onboarding.domain.DecisionChoice;

import java.math.BigDecimal;
import java.util.List;

public record KurDecisionResponse(
        DecisionChoice recommendation,
        String summary,
        BigDecimal projectedTotalRevenue,
        boolean belowCurrentYearLimit,
        boolean belowProjectedYearLimit,
        BigDecimal estimatedVorsteuerSavings,
        BigDecimal b2bRatio,
        List<String> prosKleinunternehmer,
        List<String> consKleinunternehmer,
        List<String> prosRegelbesteuerung,
        List<String> consRegelbesteuerung
) {}
