-- ============================================================
-- DreiStrom MVP – Reverse Charge & B2B handling (V5)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- Add THIRD_COUNTRY to vat_treatment enum for non-EU clients (§3a UStG)
ALTER TABLE invoice
    MODIFY COLUMN vat_treatment
        ENUM('REGULAR','REVERSE_CHARGE','SMALL_BUSINESS','INTRA_EU','THIRD_COUNTRY')
        NOT NULL DEFAULT 'REGULAR'
        COMMENT 'Regelbesteuerung / Reverse-Charge / Kleinunternehmer / Innergemeinschaftlich / Drittland';

-- Flag for Zusammenfassende Meldung (ZM) reporting
-- Required for EU B2B reverse charge transactions (e.g. Apple/Google payouts)
ALTER TABLE invoice
    ADD COLUMN zm_reportable BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'Flag for ZM (Zusammenfassende Meldung) reporting'
        AFTER notes;

-- Reverse charge notice stored in notes field (auto-appended by InvoiceService)
-- No separate column needed – notes already exists as TEXT
