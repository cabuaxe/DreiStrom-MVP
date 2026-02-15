-- ============================================================
-- DreiStrom MVP – Invoice Schema (V4)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- invoice_sequence – auto-incrementing invoice numbers
-- per stream and fiscal year (composite PK)
-- -----------------------------------------------------------
CREATE TABLE invoice_sequence (
    stream_type     ENUM('FREIBERUF','GEWERBE')     NOT NULL,
    fiscal_year     SMALLINT UNSIGNED                NOT NULL,
    next_value      INT UNSIGNED                     NOT NULL DEFAULT 1,

    PRIMARY KEY (stream_type, fiscal_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- invoice – issued invoices for Freiberuf/Gewerbe streams
-- EMPLOYMENT excluded: employment income is not invoiced.
-- line_items stored as JSON array for flexible item structure.
-- -----------------------------------------------------------
CREATE TABLE invoice (
    id              BIGINT                          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT                          NOT NULL,
    stream_type     ENUM('FREIBERUF','GEWERBE')     NOT NULL,
    number          VARCHAR(50)                     NOT NULL COMMENT 'Unique invoice number, e.g. FB-2026-001',
    client_id       BIGINT                          NOT NULL,
    invoice_date    DATE                            NOT NULL,
    due_date        DATE                            NULL,
    line_items      JSON                            NOT NULL COMMENT 'Array of {description, quantity, unitPrice, vatRate}',
    net_total_cents BIGINT                          NOT NULL COMMENT 'Net total in cents (euro)',
    vat_cents       BIGINT                          NOT NULL DEFAULT 0 COMMENT 'VAT amount in cents',
    gross_total_cents BIGINT                        NOT NULL COMMENT 'Gross total in cents (net + VAT)',
    currency        CHAR(3)                         NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217',
    vat_treatment   ENUM('REGULAR','REVERSE_CHARGE','SMALL_BUSINESS','INTRA_EU')
                                                    NOT NULL DEFAULT 'REGULAR'
                                                    COMMENT 'Regelbesteuerung / Reverse-Charge / Kleinunternehmer / Innergemeinschaftlich',
    status          ENUM('DRAFT','SENT','PAID','OVERDUE','CANCELLED')
                                                    NOT NULL DEFAULT 'DRAFT',
    notes           TEXT                            NULL COMMENT 'Free-text notes on the invoice',
    created_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)                    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)   REFERENCES app_user(id),
    FOREIGN KEY (client_id) REFERENCES client(id),
    UNIQUE KEY uk_invoice_number (number),
    INDEX idx_invoice_user_stream (user_id, stream_type),
    INDEX idx_invoice_date (invoice_date),
    INDEX idx_invoice_client (client_id),
    INDEX idx_invoice_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
