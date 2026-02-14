-- ============================================================
-- DreiStrom MVP – Baseline Schema (V1)
-- MySQL 8.4 LTS | InnoDB | utf8mb4_unicode_ci
-- ============================================================

-- -----------------------------------------------------------
-- app_user – application users (single-owner MVP)
-- -----------------------------------------------------------
CREATE TABLE app_user (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    display_name    VARCHAR(255)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- event_log – GoBD-compliant append-only audit trail
-- No UPDATE or DELETE permitted for dreistrom_app user.
-- -----------------------------------------------------------
CREATE TABLE event_log (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    BIGINT          NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    payload         JSON            NOT NULL,
    actor           VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    INDEX idx_event_aggregate (aggregate_type, aggregate_id),
    INDEX idx_event_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- document – GoBD document vault metadata
-- Deletion blocked until retention_expiry.
-- -----------------------------------------------------------
CREATE TABLE document (
    id                  BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    file_name           VARCHAR(255)    NOT NULL,
    file_hash           CHAR(64)        NOT NULL COMMENT 'SHA-256 hex digest',
    file_size           BIGINT          NOT NULL,
    mime_type           VARCHAR(100)    NOT NULL,
    category            VARCHAR(100)    NOT NULL,
    storage_key         VARCHAR(500)    NOT NULL,
    retention_expiry    DATE            NOT NULL,
    upload_ts           TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    FOREIGN KEY (user_id) REFERENCES app_user(id),
    INDEX idx_document_user (user_id),
    INDEX idx_document_category (category),
    INDEX idx_document_retention (retention_expiry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
