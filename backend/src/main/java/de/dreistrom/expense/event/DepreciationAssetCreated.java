package de.dreistrom.expense.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.expense.domain.DepreciationAsset;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DepreciationAssetCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final LocalDate acquisitionDate;
    private final BigDecimal netCost;
    private final int usefulLifeMonths;
    private final BigDecimal annualAfa;
    private final Long expenseEntryId;

    public DepreciationAssetCreated(DepreciationAsset asset) {
        super("DepreciationAsset", asset.getId(), "DEPRECIATION_ASSET_CREATED");
        this.name = asset.getName();
        this.acquisitionDate = asset.getAcquisitionDate();
        this.netCost = asset.getNetCost();
        this.usefulLifeMonths = asset.getUsefulLifeMonths();
        this.annualAfa = asset.getAnnualAfa();
        this.expenseEntryId = asset.getExpenseEntry() != null
                ? asset.getExpenseEntry().getId() : null;
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", name);
            node.put("acquisitionDate", acquisitionDate.toString());
            node.put("netCost", netCost.toPlainString());
            node.put("usefulLifeMonths", usefulLifeMonths);
            node.put("annualAfa", annualAfa.toPlainString());
            if (expenseEntryId != null) {
                node.put("expenseEntryId", expenseEntryId);
            }
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
