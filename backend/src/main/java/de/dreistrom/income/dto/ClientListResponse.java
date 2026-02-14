package de.dreistrom.income.dto;

import java.util.List;

public record ClientListResponse(
        List<ClientResponse> clients,
        boolean scheinselbstaendigkeitWarning
) {}
