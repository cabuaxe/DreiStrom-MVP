-- ============================================================
-- V16 – Fix ALL Hibernate schema validation mismatches
-- Aligns MySQL column types with JPA entity mappings so that
-- spring.jpa.hibernate.ddl-auto=validate passes cleanly.
-- ============================================================

-- -----------------------------------------------------------
-- 1. document table: transform V1 schema to match V12 entity
--    V12 migration was recorded as success but the actual table
--    still has the V1 structure (column renames + additions).
-- -----------------------------------------------------------

-- Rename and retype existing columns
ALTER TABLE document
    CHANGE COLUMN file_name    file_name    VARCHAR(500)  NOT NULL,
    CHANGE COLUMN file_hash    sha256_hash  VARCHAR(64)   NOT NULL COMMENT 'SHA-256 hex digest',
    CHANGE COLUMN mime_type    content_type VARCHAR(255)  NOT NULL,
    CHANGE COLUMN storage_key  s3_key       VARCHAR(1000) NOT NULL,
    CHANGE COLUMN category     document_type ENUM('INVOICE','RECEIPT','CONTRACT','CORRESPONDENCE',
                                                  'TAX_NOTICE','BANK_STATEMENT','PAYSLIP','OTHER')
                               NOT NULL,
    CHANGE COLUMN retention_expiry retention_until DATE NOT NULL,
    CHANGE COLUMN upload_ts    uploaded_at  TIMESTAMP(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- Add new columns
ALTER TABLE document
    ADD COLUMN retention_years INT          NOT NULL DEFAULT 10 AFTER sha256_hash,
    ADD COLUMN deletion_locked BOOLEAN      NOT NULL DEFAULT TRUE AFTER retention_until,
    ADD COLUMN description     VARCHAR(1000) NULL AFTER deletion_locked,
    ADD COLUMN tags            JSON          NULL AFTER description,
    ADD COLUMN created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER uploaded_at,
    ADD COLUMN updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at;

-- Update indexes to match V12 expected indexes
ALTER TABLE document
    DROP INDEX idx_document_category,
    DROP INDEX idx_document_retention,
    ADD INDEX idx_document_user_type (user_id, document_type),
    ADD INDEX idx_document_retention (retention_until, deletion_locked),
    ADD INDEX idx_document_hash (sha256_hash);

-- -----------------------------------------------------------
-- 2. CHAR → VARCHAR fixes (Hibernate maps String to VARCHAR)
-- -----------------------------------------------------------
ALTER TABLE client
    MODIFY COLUMN country VARCHAR(2) NOT NULL DEFAULT 'DE' COMMENT 'ISO 3166-1 alpha-2';

ALTER TABLE income_entry
    MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217';

ALTER TABLE expense_entry
    MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217';

ALTER TABLE invoice
    MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'EUR' COMMENT 'ISO 4217';

-- -----------------------------------------------------------
-- 3. TINYINT UNSIGNED / SMALLINT UNSIGNED → SMALLINT or INT
--    (Java short → SMALLINT, Java int → INT)
-- -----------------------------------------------------------

-- vorauszahlung
ALTER TABLE vorauszahlung
    MODIFY COLUMN `year`   SMALLINT NOT NULL COMMENT 'Tax year',
    MODIFY COLUMN quarter  SMALLINT NOT NULL COMMENT '1-4 for Q1-Q4';

-- vat_return
ALTER TABLE vat_return
    MODIFY COLUMN `year`        SMALLINT NOT NULL COMMENT 'Fiscal year',
    MODIFY COLUMN period_number SMALLINT NOT NULL COMMENT '1-12 monthly, 1-4 quarterly, 1 annual';

-- social_insurance_entry
ALTER TABLE social_insurance_entry
    MODIFY COLUMN `year`  SMALLINT NOT NULL,
    MODIFY COLUMN `month` SMALLINT NOT NULL COMMENT '1-12';

-- invoice_sequence
ALTER TABLE invoice_sequence
    MODIFY COLUMN fiscal_year INT NOT NULL,
    MODIFY COLUMN next_value  INT NOT NULL DEFAULT 1;
