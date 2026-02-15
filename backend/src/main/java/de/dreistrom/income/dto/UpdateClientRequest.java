package de.dreistrom.income.dto;

import de.dreistrom.income.domain.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(
        @NotBlank String name,
        ClientType clientType,
        @Size(max = 2) String country,
        @Size(max = 20) String ustIdNr,
        Boolean active
) {}
