-- ============================================================
-- DreiStrom MVP – Vorauszahlung (advance tax payments) Schema (V7)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- vorauszahlung – quarterly advance tax payment schedule
-- Due dates: 10 Mar, 10 Jun, 10 Sep, 10 Dec per year.
-- Tracks Finanzamt assessment basis, due amounts, and payments.
-- -----------------------------------------------------------
CREATE TABLE vorauszahlung (
    id                     BIGINT            AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT            NOT NULL,
    `year`                 SMALLINT UNSIGNED NOT NULL COMMENT 'Tax year',
    quarter                TINYINT UNSIGNED  NOT NULL COMMENT '1-4 for Q1-Q4',
    due_date               DATE             NOT NULL COMMENT 'Payment due date (10 Mar/Jun/Sep/Dec)',
    assessment_basis_cents BIGINT           NOT NULL DEFAULT 0 COMMENT 'Finanzamt assessment basis in cents',
    amount_cents           BIGINT           NOT NULL DEFAULT 0 COMMENT 'Amount due in cents',
    paid_cents             BIGINT           NOT NULL DEFAULT 0 COMMENT 'Amount actually paid in cents',
    status                 ENUM('PENDING','PAID','OVERDUE')
                                            NOT NULL DEFAULT 'PENDING',
    paid_date              DATE             NULL COMMENT 'Date payment was made',
    notes                  TEXT             NULL,
    created_at             TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    UNIQUE KEY uk_vorauszahlung (user_id, `year`, quarter),
    INDEX idx_vorauszahlung_year (user_id, `year`),
    INDEX idx_vorauszahlung_due (due_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
