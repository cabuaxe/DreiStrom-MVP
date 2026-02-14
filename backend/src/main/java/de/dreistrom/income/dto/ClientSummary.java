package de.dreistrom.income.dto;

import de.dreistrom.income.domain.ClientType;

public record ClientSummary(
        Long id,
        String name,
        ClientType clientType
) {}
