-- ============================================================
-- DreiStrom MVP – Income Entry & Client Schema (V2)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- client – customer / client master data
-- -----------------------------------------------------------
CREATE TABLE client (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    ust_id_nr       VARCHAR(20)     NULL     COMMENT 'EU VAT identification number',
    country         CHAR(2)         NOT NULL DEFAULT 'DE' COMMENT 'ISO 3166-1 alpha-2',
    client_type     ENUM('B2B','B2C') NOT NULL DEFAULT 'B2B',
    stream_type     ENUM('EMPLOYMENT','FREIBERUF','GEWERBE') NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_client_user (user_id),
    INDEX idx_client_stream (stream_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- income_entry – individual income records per stream
-- -----------------------------------------------------------
CREATE TABLE income_entry (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    stream_type     ENUM('EMPLOYMENT','FREIBERUF','GEWERBE') NOT NULL,
    amount_cents    BIGINT          NOT NULL COMMENT 'Amount in cents (euro)',
    currency        CHAR(3)         NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217',
    entry_date      DATE            NOT NULL,
    source          VARCHAR(255)    NULL     COMMENT 'Payment source description',
    client_id       BIGINT          NULL,
    invoice_id      BIGINT          NULL     COMMENT 'FK to invoice table (future)',
    description     VARCHAR(500)    NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)   REFERENCES app_user(id),
    FOREIGN KEY (client_id) REFERENCES client(id),
    INDEX idx_income_user_stream_date (user_id, stream_type, entry_date),
    INDEX idx_income_client (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
