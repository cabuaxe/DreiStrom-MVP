package de.dreistrom.income.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;

public class ClientModified extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String beforeName;
    private final String afterName;
    private final String beforeClientType;
    private final String afterClientType;
    private final String beforeCountry;
    private final String afterCountry;
    private final String beforeUstIdNr;
    private final String afterUstIdNr;
    private final Boolean beforeActive;
    private final Boolean afterActive;

    public ClientModified(Long clientId,
                          String beforeName, String afterName,
                          String beforeClientType, String afterClientType,
                          String beforeCountry, String afterCountry,
                          String beforeUstIdNr, String afterUstIdNr,
                          Boolean beforeActive, Boolean afterActive) {
        super("Client", clientId, "CLIENT_MODIFIED");
        this.beforeName = beforeName;
        this.afterName = afterName;
        this.beforeClientType = beforeClientType;
        this.afterClientType = afterClientType;
        this.beforeCountry = beforeCountry;
        this.afterCountry = afterCountry;
        this.beforeUstIdNr = beforeUstIdNr;
        this.afterUstIdNr = afterUstIdNr;
        this.beforeActive = beforeActive;
        this.afterActive = afterActive;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();

            ObjectNode before = node.putObject("before");
            before.put("name", beforeName);
            before.put("clientType", beforeClientType);
            before.put("country", beforeCountry);
            if (beforeUstIdNr != null) before.put("ustIdNr", beforeUstIdNr);
            before.put("active", beforeActive);

            ObjectNode after = node.putObject("after");
            after.put("name", afterName);
            after.put("clientType", afterClientType);
            after.put("country", afterCountry);
            if (afterUstIdNr != null) after.put("ustIdNr", afterUstIdNr);
            after.put("active", afterActive);

            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
