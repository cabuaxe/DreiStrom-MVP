-- ============================================================
-- DreiStrom MVP – Tax Period & VAT Return Schema (V6)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- tax_period – tax calculation periods for EÜR and income tax
-- Supports monthly (Voranmeldung), quarterly, and annual.
-- Separate per-stream tracking via FK to income_entry for
-- Freiberuf/Gewerbe EÜR reference entries.
-- -----------------------------------------------------------
CREATE TABLE tax_period (
    id              BIGINT                          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT                          NOT NULL,
    year            SMALLINT UNSIGNED               NOT NULL COMMENT 'Fiscal year, e.g. 2026',
    period_type     ENUM('MONTHLY','QUARTERLY','ANNUAL')
                                                    NOT NULL,
    period_number   TINYINT UNSIGNED                NOT NULL COMMENT '1-12 for MONTHLY, 1-4 for QUARTERLY, 1 for ANNUAL',
    freiberuf_revenue_cents  BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Freiberuf gross revenue in cents',
    gewerbe_revenue_cents    BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Gewerbe gross revenue in cents',
    estimated_tax_cents      BIGINT                 NOT NULL DEFAULT 0 COMMENT 'Estimated income tax in cents',
    actual_tax_cents         BIGINT                 NULL     COMMENT 'Actual tax after filing (null until filed)',
    status          ENUM('OPEN','FILED','ASSESSED')
                                                    NOT NULL DEFAULT 'OPEN'
                                                    COMMENT 'OPEN = in progress, FILED = submitted, ASSESSED = Bescheid received',
    created_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    UNIQUE KEY uk_tax_period (user_id, year, period_type, period_number),
    INDEX idx_tax_period_year (user_id, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- vat_return – Umsatzsteuer-Voranmeldung (USt-VA) data
-- Tracks output VAT, input VAT, and net payable per period.
-- Linked to tax_period for consolidated reporting.
-- -----------------------------------------------------------
CREATE TABLE vat_return (
    id              BIGINT                          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT                          NOT NULL,
    tax_period_id   BIGINT                          NULL COMMENT 'Optional link to tax_period',
    year            SMALLINT UNSIGNED               NOT NULL COMMENT 'Fiscal year',
    period_type     ENUM('MONTHLY','QUARTERLY','ANNUAL')
                                                    NOT NULL,
    period_number   TINYINT UNSIGNED                NOT NULL COMMENT '1-12 for MONTHLY, 1-4 for QUARTERLY, 1 for ANNUAL',
    output_vat_cents BIGINT                         NOT NULL DEFAULT 0 COMMENT 'Collected VAT (Umsatzsteuer) in cents',
    input_vat_cents  BIGINT                         NOT NULL DEFAULT 0 COMMENT 'Deductible VAT (Vorsteuer) in cents',
    net_payable_cents BIGINT                        NOT NULL DEFAULT 0 COMMENT 'Net VAT payable (output - input) in cents, negative = refund',
    status          ENUM('DRAFT','SUBMITTED','ACCEPTED','CORRECTED')
                                                    NOT NULL DEFAULT 'DRAFT'
                                                    COMMENT 'DRAFT = preparing, SUBMITTED = sent to Finanzamt, ACCEPTED = confirmed, CORRECTED = amended',
    submission_date DATE                            NULL COMMENT 'Date submitted to Finanzamt',
    notes           TEXT                            NULL,
    created_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)       REFERENCES app_user(id),
    FOREIGN KEY (tax_period_id) REFERENCES tax_period(id),
    UNIQUE KEY uk_vat_return (user_id, year, period_type, period_number),
    INDEX idx_vat_return_year (user_id, year),
    INDEX idx_vat_return_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
