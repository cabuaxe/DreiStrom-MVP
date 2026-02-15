-- App Store payout import schema for Apple/Google marketplace revenue

CREATE TABLE appstore_payout (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    platform        ENUM('APPLE','GOOGLE') NOT NULL,
    report_date     DATE            NOT NULL,
    region          VARCHAR(10)     NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    gross_revenue_cents     BIGINT  NOT NULL DEFAULT 0,
    commission_cents        BIGINT  NOT NULL DEFAULT 0,
    net_revenue_cents       BIGINT  NOT NULL DEFAULT 0,
    vat_cents               BIGINT  NOT NULL DEFAULT 0,
    product_id      VARCHAR(500),
    product_name    VARCHAR(500),
    quantity        INT             NOT NULL DEFAULT 0,
    import_batch_id VARCHAR(100)    NOT NULL,
    raw_csv_line    TEXT,
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_appstore_payout_user FOREIGN KEY (user_id)
        REFERENCES app_user(id) ON DELETE RESTRICT,

    INDEX idx_payout_user_platform (user_id, platform, report_date),
    INDEX idx_payout_batch (import_batch_id),
    UNIQUE KEY uk_payout_line (user_id, platform, report_date, product_id, region, import_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
