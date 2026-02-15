package de.dreistrom.income.dto;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.ClientType;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        IncomeStream streamType,
        ClientType clientType,
        String country,
        String ustIdNr,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
