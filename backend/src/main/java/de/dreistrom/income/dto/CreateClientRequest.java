package de.dreistrom.income.dto;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank String name,
        @NotNull IncomeStream streamType,
        ClientType clientType,
        @Size(max = 2) String country,
        @Size(max = 20) String ustIdNr
) {}
