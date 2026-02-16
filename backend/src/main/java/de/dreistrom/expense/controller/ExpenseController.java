package de.dreistrom.expense.controller;

import de.dreistrom.common.Idempotent;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.dto.CreateExpenseEntryRequest;
import de.dreistrom.expense.dto.ExpenseEntryResponse;
import de.dreistrom.expense.dto.UpdateExpenseEntryRequest;
import de.dreistrom.expense.mapper.ExpenseEntryMapper;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.service.DepreciationService;
import de.dreistrom.expense.service.DepreciationYearEntry;
import de.dreistrom.expense.service.ExpenseService;
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
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage expense entries across all three streams")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final DepreciationService depreciationService;
    private final DepreciationAssetRepository depreciationAssetRepository;
    private final ExpenseEntryMapper expenseEntryMapper;
    private final EntityManager entityManager;

    @PostMapping
    @Idempotent
    @Operation(operationId = "createExpense", summary = "Create a new expense entry",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Expense entry created",
                            headers = @Header(name = "Location", description = "URI of the created entry")),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Allocation rule not found"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
            })
    public ResponseEntity<ExpenseEntryResponse> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreateExpenseEntryRequest request) {

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());

        ExpenseEntry saved = expenseService.create(
                user, request.amount(), request.category(),
                request.entryDate(), request.allocationRuleId(),
                request.receiptDocId(), request.description());

        ExpenseEntryResponse response = expenseEntryMapper.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/expenses/" + saved.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(operationId = "listExpenses", summary = "List expense entries with optional filters",
            responses = @ApiResponse(responseCode = "200", description = "List of expense entries"))
    public ResponseEntity<List<ExpenseEntryResponse>> list(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        Long userId = userDetails.getId();
        List<ExpenseEntry> entries;

        if (category != null && fromDate != null && toDate != null) {
            entries = expenseService.listByCategoryAndDateRange(userId, category, fromDate, toDate);
        } else if (category != null) {
            entries = expenseService.listByCategory(userId, category);
        } else if (fromDate != null && toDate != null) {
            entries = expenseService.listByDateRange(userId, fromDate, toDate);
        } else {
            entries = expenseService.listAll(userId);
        }

        return ResponseEntity.ok(expenseEntryMapper.toResponseList(entries));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getExpense", summary = "Get an expense entry by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Expense entry found"),
                    @ApiResponse(responseCode = "404", description = "Expense entry not found")
            })
    public ResponseEntity<ExpenseEntryResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        ExpenseEntry entry = expenseService.getById(id, userDetails.getId());
        return ResponseEntity.ok(expenseEntryMapper.toResponse(entry));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateExpense", summary = "Update an expense entry",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Expense entry updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error"),
                    @ApiResponse(responseCode = "404", description = "Expense entry not found")
            })
    public ResponseEntity<ExpenseEntryResponse> update(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateExpenseEntryRequest request) {

        ExpenseEntry updated = expenseService.update(
                id, userDetails.getId(), request.amount(),
                request.category(), request.entryDate(),
                request.allocationRuleId(), request.receiptDocId(),
                request.description());

        return ResponseEntity.ok(expenseEntryMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteExpense", summary = "Delete an expense entry",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Expense entry deleted"),
                    @ApiResponse(responseCode = "404", description = "Expense entry not found")
            })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        expenseService.delete(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/depreciation")
    @Operation(operationId = "getExpenseDepreciationSchedule", summary = "Get depreciation schedule for an expense entry's linked asset",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Depreciation schedule"),
                    @ApiResponse(responseCode = "404", description = "Expense entry or linked asset not found")
            })
    public ResponseEntity<List<DepreciationYearEntry>> getDepreciationSchedule(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        // Verify ownership
        expenseService.getById(id, userDetails.getId());

        DepreciationAsset asset = depreciationAssetRepository.findByExpenseEntryId(id)
                .stream().findFirst()
                .orElseThrow(() -> new de.dreistrom.common.controller.EntityNotFoundException(
                        "DepreciationAsset for ExpenseEntry", id));

        List<DepreciationYearEntry> schedule = depreciationService.computeSchedule(asset);
        return ResponseEntity.ok(schedule);
    }
}
