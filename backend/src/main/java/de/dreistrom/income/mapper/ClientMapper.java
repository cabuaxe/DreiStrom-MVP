package de.dreistrom.income.mapper;

import de.dreistrom.income.domain.Client;
import de.dreistrom.income.dto.ClientResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientResponse toResponse(Client client);

    List<ClientResponse> toResponseList(List<Client> clients);
}
