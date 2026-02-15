package de.dreistrom.income.controller;

import de.dreistrom.common.Idempotent;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.dto.ClientListResponse;
import de.dreistrom.income.dto.ClientResponse;
import de.dreistrom.income.dto.CreateClientRequest;
import de.dreistrom.income.dto.UpdateClientRequest;
import de.dreistrom.income.mapper.ClientMapper;
import de.dreistrom.income.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Manage client/customer master data")
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;
    private final EntityManager entityManager;

    @PostMapping
    @Idempotent
    @Operation(summary = "Create a new client",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Client created",
                            headers = @Header(name = "Location", description = "URI of the created client")),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
            })
    public ResponseEntity<ClientResponse> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreateClientRequest request) {

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());

        Client saved = clientService.create(
                user, request.name(), request.streamType(),
                request.clientType(), request.country(), request.ustIdNr());

        ClientResponse response = clientMapper.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/clients/" + saved.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List clients with optional stream type filter",
            responses = @ApiResponse(responseCode = "200", description = "List of clients with Scheinselbständigkeit warning"))
    public ResponseEntity<ClientListResponse> list(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) IncomeStream streamType) {

        Long userId = userDetails.getId();
        List<Client> clients;

        if (streamType != null) {
            clients = clientService.listByStreamType(userId, streamType);
        } else {
            clients = clientService.listAll(userId);
        }

        boolean warning = clientService.checkScheinselbstaendigkeitRisk(userId);
        List<ClientResponse> responses = clientMapper.toResponseList(clients);

        return ResponseEntity.ok(new ClientListResponse(responses, warning));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a client by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Client found"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            })
    public ResponseEntity<ClientResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        Client client = clientService.getById(id, userDetails.getId());
        return ResponseEntity.ok(clientMapper.toResponse(client));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a client",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Client updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            })
    public ResponseEntity<ClientResponse> update(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateClientRequest request) {

        Client updated = clientService.update(
                id, userDetails.getId(), request.name(),
                request.clientType(), request.country(),
                request.ustIdNr(), request.active());

        return ResponseEntity.ok(clientMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a client (set active=false)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Client deactivated"),
                    @ApiResponse(responseCode = "404", description = "Client not found")
            })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        clientService.delete(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
