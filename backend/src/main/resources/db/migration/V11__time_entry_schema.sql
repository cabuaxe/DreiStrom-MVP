-- ============================================================
-- DreiStrom MVP – Time Entry Schema (V11)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- time_entry – daily working time records per activity type.
-- Used for social insurance monitor (weekly aggregation),
-- ArbZG compliance tracking, and project-based time reports.
-- -----------------------------------------------------------
CREATE TABLE time_entry (
    id              BIGINT              AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT              NOT NULL,
    entry_date      DATE                NOT NULL,
    hours           DECIMAL(4,2)        NOT NULL COMMENT 'Hours worked (e.g. 8.50)',
    activity_type   ENUM('EMPLOYMENT','FREIBERUF','GEWERBE')
                                        NOT NULL,
    description     VARCHAR(500)        NULL,
    created_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)        NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_time_entry_date   (user_id, entry_date),
    INDEX idx_time_entry_type   (user_id, activity_type, entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
