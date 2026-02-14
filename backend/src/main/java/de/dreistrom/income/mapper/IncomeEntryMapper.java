package de.dreistrom.income.mapper;

import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.dto.ClientSummary;
import de.dreistrom.income.dto.IncomeEntryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IncomeEntryMapper {

    @Mapping(target = "client", source = "client", qualifiedByName = "clientToSummary")
    IncomeEntryResponse toResponse(IncomeEntry entry);

    List<IncomeEntryResponse> toResponseList(List<IncomeEntry> entries);

    @Named("clientToSummary")
    default ClientSummary clientToSummary(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientSummary(client.getId(), client.getName(), client.getClientType());
    }
}
