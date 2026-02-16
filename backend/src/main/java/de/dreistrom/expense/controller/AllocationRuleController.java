package de.dreistrom.expense.controller;

import de.dreistrom.common.Idempotent;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.dto.AllocationRuleResponse;
import de.dreistrom.expense.dto.CreateAllocationRuleRequest;
import de.dreistrom.expense.dto.UpdateAllocationRuleRequest;
import de.dreistrom.expense.mapper.AllocationRuleMapper;
import de.dreistrom.expense.service.AllocationRuleService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/allocation-rules")
@RequiredArgsConstructor
@Tag(name = "Allocation Rules", description = "Manage three-stream allocation rules for expenses")
public class AllocationRuleController {

    private final AllocationRuleService allocationRuleService;
    private final AllocationRuleMapper allocationRuleMapper;
    private final EntityManager entityManager;

    @PostMapping
    @Idempotent
    @Operation(operationId = "createAllocationRule", summary = "Create a new allocation rule",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Allocation rule created",
                            headers = @Header(name = "Location", description = "URI of the created rule")),
                    @ApiResponse(responseCode = "400", description = "Validation error (percentages must sum to 100)"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
            })
    public ResponseEntity<AllocationRuleResponse> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreateAllocationRuleRequest request) {

        validateAllocationSum(request.freiberufPct(), request.gewerbePct(), request.personalPct());

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());

        AllocationRule saved = allocationRuleService.create(
                user, request.name(),
                request.freiberufPct(), request.gewerbePct(), request.personalPct());

        AllocationRuleResponse response = allocationRuleMapper.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/allocation-rules/" + saved.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(operationId = "listAllocationRules", summary = "List all allocation rules",
            responses = @ApiResponse(responseCode = "200", description = "List of allocation rules"))
    public ResponseEntity<List<AllocationRuleResponse>> list(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        List<AllocationRule> rules = allocationRuleService.listAll(userDetails.getId());
        return ResponseEntity.ok(allocationRuleMapper.toResponseList(rules));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAllocationRule", summary = "Get an allocation rule by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Allocation rule found"),
                    @ApiResponse(responseCode = "404", description = "Allocation rule not found")
            })
    public ResponseEntity<AllocationRuleResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        AllocationRule rule = allocationRuleService.getById(id, userDetails.getId());
        return ResponseEntity.ok(allocationRuleMapper.toResponse(rule));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateAllocationRule", summary = "Update an allocation rule",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Allocation rule updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error (percentages must sum to 100)"),
                    @ApiResponse(responseCode = "404", description = "Allocation rule not found")
            })
    public ResponseEntity<AllocationRuleResponse> update(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAllocationRuleRequest request) {

        validateAllocationSum(request.freiberufPct(), request.gewerbePct(), request.personalPct());

        AllocationRule updated = allocationRuleService.update(
                id, userDetails.getId(), request.name(),
                request.freiberufPct(), request.gewerbePct(), request.personalPct());

        return ResponseEntity.ok(allocationRuleMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteAllocationRule", summary = "Delete an allocation rule",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Allocation rule deleted"),
                    @ApiResponse(responseCode = "404", description = "Allocation rule not found")
            })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        allocationRuleService.delete(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    private void validateAllocationSum(short freiberufPct, short gewerbePct, short personalPct) {
        if (freiberufPct + gewerbePct + personalPct != 100) {
            throw new IllegalArgumentException(
                    "Allocation percentages must sum to 100, got: " +
                    (freiberufPct + gewerbePct + personalPct));
        }
    }
}
