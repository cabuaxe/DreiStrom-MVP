package de.dreistrom.document.domain;

/**
 * Document classification for retention rules per §147 AO.
 */
public enum DocumentType {
    INVOICE,          // 10 years
    RECEIPT,          // 10 years
    CONTRACT,         // 6 years
    CORRESPONDENCE,   // 6 years
    TAX_NOTICE,       // 10 years
    BANK_STATEMENT,   // 10 years
    PAYSLIP,          // 6 years
    OTHER;            // 10 years (conservative default)

    public int retentionYears() {
        return switch (this) {
            case CONTRACT, CORRESPONDENCE, PAYSLIP -> 6;
            default -> 10;
        };
    }
}
