-- ============================================================
-- DreiStrom MVP – MySQL User & Privilege Setup
-- ============================================================

-- Create application user (limited privileges – GoBD compliant)
CREATE USER IF NOT EXISTS 'dreistrom_app'@'%' IDENTIFIED BY 'dreistrom_dev';

-- Create migration user (full privileges for Flyway DDL)
CREATE USER IF NOT EXISTS 'dreistrom_migration'@'%' IDENTIFIED BY 'dreistrom_dev';

-- -----------------------------------------------------------
-- Migration user: ALL PRIVILEGES (Flyway needs DDL + DML)
-- -----------------------------------------------------------
GRANT ALL PRIVILEGES ON dreistrom.* TO 'dreistrom_migration'@'%' WITH GRANT OPTION;

-- -----------------------------------------------------------
-- App user: SELECT + INSERT on all tables (database-level).
-- UPDATE and DELETE are granted per-table AFTER Flyway creates
-- them (see afterMigrate.sql callback). event_log is excluded
-- from UPDATE/DELETE to enforce GoBD append-only audit trail.
-- -----------------------------------------------------------
GRANT SELECT, INSERT ON dreistrom.* TO 'dreistrom_app'@'%';

FLUSH PRIVILEGES;
