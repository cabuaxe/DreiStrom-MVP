-- Document vault schema for GoBD-compliant document storage
-- §147 AO retention: 10 years invoices/bookkeeping, 6 years correspondence

CREATE TABLE document (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    file_name       VARCHAR(500)    NOT NULL,
    content_type    VARCHAR(255)    NOT NULL,
    file_size       BIGINT          NOT NULL,
    s3_key          VARCHAR(1000)   NOT NULL,
    sha256_hash     CHAR(64)        NOT NULL,
    document_type   ENUM('INVOICE','RECEIPT','CONTRACT','CORRESPONDENCE',
                         'TAX_NOTICE','BANK_STATEMENT','PAYSLIP','OTHER')
                    NOT NULL,
    retention_years INT             NOT NULL DEFAULT 10,
    retention_until DATE            NOT NULL,
    deletion_locked BOOLEAN         NOT NULL DEFAULT TRUE,
    description     VARCHAR(1000),
    tags            JSON,
    uploaded_at     TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_document_user FOREIGN KEY (user_id)
        REFERENCES app_user(id) ON DELETE RESTRICT,

    INDEX idx_document_user_type (user_id, document_type),
    INDEX idx_document_retention (retention_until, deletion_locked),
    INDEX idx_document_hash (sha256_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
