-- ============================================================
-- DreiStrom MVP – Social Insurance Monitor Schema (V10)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- social_insurance_entry – monthly tracking of working hours
-- and income for primary/secondary classification.
-- §5 Abs. 5 SGB V: self-employed is secondary if
-- employment is the main activity (>20h/week and higher income).
-- -----------------------------------------------------------
CREATE TABLE social_insurance_entry (
    id                          BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id                     BIGINT              NOT NULL,
    `year`                      SMALLINT UNSIGNED   NOT NULL,
    `month`                     TINYINT UNSIGNED    NOT NULL COMMENT '1-12',
    employment_hours_weekly     DECIMAL(5,1)        NOT NULL DEFAULT 0 COMMENT 'Avg weekly hours in employment',
    self_employed_hours_weekly  DECIMAL(5,1)        NOT NULL DEFAULT 0 COMMENT 'Avg weekly hours self-employed',
    employment_income_cents     BIGINT              NOT NULL DEFAULT 0 COMMENT 'Monthly employment income in cents',
    self_employed_income_cents  BIGINT              NOT NULL DEFAULT 0 COMMENT 'Monthly self-employed income in cents',
    created_at                  TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    UNIQUE KEY uk_si_entry (user_id, `year`, `month`),
    INDEX idx_si_entry_year (user_id, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
