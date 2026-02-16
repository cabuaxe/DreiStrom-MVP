package de.dreistrom.income.controller;

import de.dreistrom.common.Idempotent;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.dto.CreateIncomeEntryRequest;
import de.dreistrom.income.dto.IncomeEntryResponse;
import de.dreistrom.income.dto.UpdateIncomeEntryRequest;
import de.dreistrom.income.mapper.IncomeEntryMapper;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.service.IncomeService;
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
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/income-entries")
@RequiredArgsConstructor
@Tag(name = "Income Entries", description = "Manage income entries across all three streams")
public class IncomeController {

    private final IncomeService incomeService;
    private final IncomeEntryMapper incomeEntryMapper;
    private final ClientRepository clientRepository;
    private final EntityManager entityManager;

    @PostMapping
    @Idempotent
    @Operation(operationId = "createIncomeEntry", summary = "Create a new income entry",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Income entry created",
                            headers = @Header(name = "Location", description = "URI of the created entry")),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Client not found"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
            })
    public ResponseEntity<IncomeEntryResponse> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreateIncomeEntryRequest request) {

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());
        Client client = resolveClient(request.clientId(), userDetails.getId(), request.streamType());

        IncomeEntry saved = incomeService.create(
                user, request.streamType(), request.amount(),
                request.entryDate(), request.source(), client, request.description());

        IncomeEntryResponse response = incomeEntryMapper.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/income-entries/" + saved.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(operationId = "listIncomeEntries", summary = "List income entries with optional filters",
            responses = @ApiResponse(responseCode = "200", description = "List of income entries"))
    public ResponseEntity<List<IncomeEntryResponse>> list(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) IncomeStream streamType,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        Long userId = userDetails.getId();
        List<IncomeEntry> entries;

        if (streamType != null && fromDate != null && toDate != null) {
            entries = incomeService.listByStreamAndDateRange(userId, streamType, fromDate, toDate);
        } else if (streamType != null) {
            entries = incomeService.listByStream(userId, streamType);
        } else if (fromDate != null && toDate != null) {
            entries = incomeService.listByDateRange(userId, fromDate, toDate);
        } else {
            entries = incomeService.listAll(userId);
        }

        return ResponseEntity.ok(incomeEntryMapper.toResponseList(entries));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getIncomeEntry", summary = "Get an income entry by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Income entry found"),
                    @ApiResponse(responseCode = "404", description = "Income entry not found")
            })
    public ResponseEntity<IncomeEntryResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        IncomeEntry entry = incomeService.getById(id, userDetails.getId());
        return ResponseEntity.ok(incomeEntryMapper.toResponse(entry));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateIncomeEntry", summary = "Update an income entry",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Income entry updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Income entry not found")
            })
    public ResponseEntity<IncomeEntryResponse> update(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncomeEntryRequest request) {

        IncomeEntry existing = incomeService.getById(id, userDetails.getId());
        Client client = resolveClient(request.clientId(), userDetails.getId(), existing.getStreamType());

        IncomeEntry updated = incomeService.update(
                id, existing.getStreamType(), request.amount(),
                request.entryDate(), request.source(), client, request.description());

        return ResponseEntity.ok(incomeEntryMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteIncomeEntry", summary = "Delete an income entry",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Income entry deleted"),
                    @ApiResponse(responseCode = "404", description = "Income entry not found")
            })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        incomeService.delete(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    private Client resolveClient(Long clientId, Long userId, IncomeStream streamType) {
        if (clientId == null) {
            return null;
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Client", clientId);
        }
        if (client.getStreamType() != streamType) {
            throw new IllegalArgumentException(
                    "Client stream type " + client.getStreamType()
                    + " does not match income stream type " + streamType);
        }
        return client;
    }
}
