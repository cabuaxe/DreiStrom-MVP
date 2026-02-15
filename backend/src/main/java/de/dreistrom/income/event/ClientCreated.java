package de.dreistrom.income.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.income.domain.Client;

public class ClientCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String streamType;
    private final String clientType;

    public ClientCreated(Client client) {
        super("Client", client.getId(), "CLIENT_CREATED");
        this.name = client.getName();
        this.streamType = client.getStreamType().name();
        this.clientType = client.getClientType().name();
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", name);
            node.put("streamType", streamType);
            node.put("clientType", clientType);
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
