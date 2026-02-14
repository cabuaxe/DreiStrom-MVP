-- ============================================================
-- DreiStrom MVP – Expense Schema (V3)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- allocation_rule – stream-split percentages for expenses
-- CHECK constraint enforces freiberuf + gewerbe + personal = 100
-- -----------------------------------------------------------
CREATE TABLE allocation_rule (
    id              BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT              NOT NULL,
    name            VARCHAR(255)        NOT NULL COMMENT 'Rule name, e.g. Home Office Split',
    freiberuf_pct   TINYINT UNSIGNED    NOT NULL DEFAULT 0,
    gewerbe_pct     TINYINT UNSIGNED    NOT NULL DEFAULT 0,
    personal_pct    TINYINT UNSIGNED    NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_allocation_user (user_id),
    CONSTRAINT chk_allocation_sum CHECK (freiberuf_pct + gewerbe_pct + personal_pct = 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- expense_entry – individual expense records
-- -----------------------------------------------------------
CREATE TABLE expense_entry (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    amount_cents        BIGINT          NOT NULL COMMENT 'Amount in cents (euro)',
    currency            CHAR(3)         NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217',
    category            VARCHAR(100)    NOT NULL COMMENT 'Expense category',
    entry_date          DATE            NOT NULL,
    receipt_doc_id      BIGINT          NULL     COMMENT 'FK to document table for receipt scan',
    allocation_rule_id  BIGINT          NULL     COMMENT 'FK to allocation_rule for stream split',
    description         VARCHAR(500)    NULL,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)             REFERENCES app_user(id),
    FOREIGN KEY (receipt_doc_id)      REFERENCES document(id),
    FOREIGN KEY (allocation_rule_id)  REFERENCES allocation_rule(id),
    INDEX idx_expense_user_date (user_id, entry_date),
    INDEX idx_expense_category (category),
    INDEX idx_expense_receipt (receipt_doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- depreciation_asset – AfA (Absetzung für Abnutzung) assets
-- -----------------------------------------------------------
CREATE TABLE depreciation_asset (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    name                VARCHAR(255)    NOT NULL COMMENT 'Asset description',
    acquisition_date    DATE            NOT NULL,
    net_cost_cents      BIGINT          NOT NULL COMMENT 'Net acquisition cost in cents (euro)',
    useful_life_months  INT             NOT NULL COMMENT 'Useful life in months per AfA table',
    annual_afa_cents    BIGINT          NOT NULL COMMENT 'Annual depreciation amount in cents',
    expense_entry_id    BIGINT          NULL     COMMENT 'FK to originating expense entry',
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id)          REFERENCES app_user(id),
    FOREIGN KEY (expense_entry_id) REFERENCES expense_entry(id),
    INDEX idx_depreciation_user (user_id),
    INDEX idx_depreciation_date (acquisition_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
