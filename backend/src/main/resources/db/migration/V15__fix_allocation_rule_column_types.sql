-- Fix allocation_rule percentage columns: TINYINT UNSIGNED → SMALLINT
-- Hibernate maps Java 'short' to SMALLINT; aligning DB to match entity
ALTER TABLE allocation_rule
    MODIFY COLUMN freiberuf_pct SMALLINT NOT NULL DEFAULT 0,
    MODIFY COLUMN gewerbe_pct   SMALLINT NOT NULL DEFAULT 0,
    MODIFY COLUMN personal_pct  SMALLINT NOT NULL DEFAULT 0;
